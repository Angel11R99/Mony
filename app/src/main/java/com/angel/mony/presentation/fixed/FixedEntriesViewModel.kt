package com.angel.mony.presentation.fixed

import com.angel.mony.core.time.toKotlinInstant
import com.angel.mony.core.time.toKotlinLocalDate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.EntryDisplayPreferences
import com.angel.mony.core.FinanceDataCache
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.EntryCardSize
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.FixedScheduleMode
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.calculateNextRun
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class FixedEntriesUiState(
    val entries: List<FixedEntry> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val isReady: Boolean = false,
)

@HiltViewModel
class FixedEntriesViewModel @Inject constructor(
    private val fixedEntries: FixedEntryRepository,
    dataCache: FinanceDataCache,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(dataCache.fixedEntries, dataCache.categories) { entries, categoryList ->
        FixedEntriesUiState(
            entries = entries,
            categories = categoryList.associateBy(Category::id),
            isReady = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FixedEntriesUiState(
            entries = dataCache.fixedEntries.value,
            categories = dataCache.categories.value.associateBy(Category::id),
            isReady = true,
        ),
    )

    val message = MutableStateFlow<String?>(null)
    private val displayPreferences = EntryDisplayPreferences(context)

    val cardSize: StateFlow<EntryCardSize> = displayPreferences.fixedCardSize

    fun setCardSize(size: EntryCardSize) = displayPreferences.setFixedCardSize(size)

    fun consumeMessage() { message.value = null }

    fun save(
        existing: FixedEntry?,
        type: TransactionType,
        description: String,
        amount: String,
        categoryId: Long?,
        comment: String,
        isActive: Boolean,
        onSaved: () -> Unit,
    ) {
        val cents = MoneyFormatter.parseToCents(amount)
        when {
            description.isBlank() -> message.value = "Escribe una descripción"
            cents == null || cents <= 0 -> message.value = "Introduce un monto válido"
            categoryId == null -> message.value = "Selecciona una categoría"
            else -> viewModelScope.launch {
                runCatching {
                    val base = existing ?: FixedEntry(
                        type = type,
                        description = description.trim(),
                        amountInCents = cents,
                        categoryId = categoryId,
                        comment = null,
                    )
                    fixedEntries.save(
                        base.copy(
                            id = existing?.id ?: 0,
                            type = type,
                            description = description.trim(),
                            amountInCents = cents,
                            categoryId = categoryId,
                            comment = comment.trim().ifBlank { null },
                            isActive = isActive,
                        )
                    )
                }.onSuccess {
                    message.value = "Plantilla guardada"
                    onSaved()
                }.onFailure { message.value = "No se pudo guardar" }
            }
        }
    }

    fun toggle(entry: FixedEntry) = viewModelScope.launch {
        fixedEntries.save(entry.copy(isActive = !entry.isActive))
    }

    fun delete(entry: FixedEntry) = viewModelScope.launch {
        fixedEntries.delete(entry.id)
        message.value = "Plantilla eliminada"
    }

    fun configure(
        entry: FixedEntry,
        scheduleMode: FixedScheduleMode,
        scheduleHour: Int,
        scheduleSpecificDate: LocalDate?,
        onSaved: () -> Unit,
    ) {
        val now = Instant.now()
        val nextRun = calculateNextRun(
            mode = scheduleMode,
            hour = scheduleHour,
            specificDate = scheduleSpecificDate,
            after = now,
        )
        when {
            scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME && nextRun == null ->
                message.value = "Selecciona una fecha futura para programar"
            else -> viewModelScope.launch {
                fixedEntries.save(
                    entry.copy(
                        scheduleMode = scheduleMode,
                        scheduleHour = scheduleHour.coerceIn(0, 23),
                        scheduleSpecificDate = scheduleSpecificDate?.toKotlinLocalDate(),
                        nextRunAt = nextRun?.toKotlinInstant(),
                    )
                )
                message.value = "Configuración guardada"
                onSaved()
            }
        }
    }

    // Funding dialog state
    private val _showFundingDialog = MutableStateFlow<ExpenseCreationResult.RequiresFundingSource?>(null)
    val showFundingDialog: StateFlow<ExpenseCreationResult.RequiresFundingSource?> = _showFundingDialog
    private var pendingFixedEntry: FixedEntry? = null
    private var pendingPostingDate: LocalDate? = null

    fun addNow(entry: FixedEntry, postingDate: LocalDate) {
        if (!entry.isActive) {
            message.value = "Activa la plantilla antes de agregarla"
            return
        }
        val now = Instant.now()
        val transaction = entry.toTransaction(date = postingDate, now = now)
        viewModelScope.launch {
            val result = fixedEntries.post(
                entry = entry.copy(
                    lastAddedAt = now.toKotlinInstant(),
                    lastAddedDate = postingDate.toKotlinLocalDate(),
                ),
                transaction = transaction,
            )
            handlePostResult(result, entry, postingDate, transaction)
        }
    }

    private fun handlePostResult(
        result: ExpenseCreationResult,
        entry: FixedEntry,
        postingDate: LocalDate,
        transaction: FinanceTransaction,
    ) {
        when (result) {
            is ExpenseCreationResult.Saved -> {
                val kind = if (entry.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"
                message.value = "$kind agregado correctamente"
                viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
            }
            is ExpenseCreationResult.RequiresFundingSource -> {
                pendingFixedEntry = entry
                pendingPostingDate = postingDate
                _showFundingDialog.value = result
            }
            is ExpenseCreationResult.Error -> {
                message.value = result.message
            }
        }
    }

    fun confirmFundingSource(sourceDescription: String) {
        val result = _showFundingDialog.value ?: return
        val entry = pendingFixedEntry ?: return
        val postingDate = pendingPostingDate ?: return
        val now = Instant.now()
        val transaction = entry.toTransaction(date = postingDate, now = now)

        _showFundingDialog.value = null
        pendingFixedEntry = null
        pendingPostingDate = null

        viewModelScope.launch {
            val finalResult = fixedEntries.post(
                entry = entry.copy(
                    lastAddedAt = now.toKotlinInstant(),
                    lastAddedDate = postingDate.toKotlinLocalDate(),
                ),
                transaction = transaction.copy(
                    createdAt = now.toKotlinInstant(),
                    updatedAt = now.toKotlinInstant(),
                ),
                fundingSourceDescription = sourceDescription,
            )
            when (finalResult) {
                is ExpenseCreationResult.Saved -> {
                    val kind = if (entry.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"
                    message.value = "$kind agregado correctamente"
                    viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
                }
                is ExpenseCreationResult.Error -> {
                    message.value = finalResult.message
                    _showFundingDialog.value = result
                    pendingFixedEntry = entry
                    pendingPostingDate = postingDate
                }
                is ExpenseCreationResult.RequiresFundingSource -> {
                    _showFundingDialog.value = finalResult
                    pendingFixedEntry = entry
                    pendingPostingDate = postingDate
                }
            }
        }
    }

    fun cancelFundingDialog() {
        _showFundingDialog.value = null
        pendingFixedEntry = null
        pendingPostingDate = null
    }
}

internal fun FixedEntry.toTransaction(
    date: LocalDate,
    now: Instant,
): FinanceTransaction = FinanceTransaction(
    amountInCents = amountInCents,
    type = type,
    categoryId = categoryId,
    description = listOfNotNull(description, comment?.takeIf { it.isNotBlank() })
        .joinToString(" · "),
    date = date.toKotlinLocalDate(),
    createdAt = now.toKotlinInstant(),
    updatedAt = now.toKotlinInstant(),
    fixedEntryId = id,
)
