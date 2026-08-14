package com.example.personalfinancetracker.presentation.fixed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.FixedScheduleMode
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.calculateNextRun
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.FixedEntryRepository
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class FixedEntriesUiState(
    val entries: List<FixedEntry> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
)

@HiltViewModel
class FixedEntriesViewModel @Inject constructor(
    private val fixedEntries: FixedEntryRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(fixedEntries.observeAll(), categories.observeAll()) { entries, categoryList ->
        FixedEntriesUiState(entries, categoryList.associateBy(Category::id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixedEntriesUiState())

    val message = MutableStateFlow<String?>(null)

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
                        scheduleSpecificDate = scheduleSpecificDate,
                        nextRunAt = nextRun,
                    )
                )
                message.value = "Configuración guardada"
                onSaved()
            }
        }
    }

    fun addNow(entry: FixedEntry, postingDate: LocalDate) {
        if (!entry.isActive) {
            message.value = "Activa la plantilla antes de agregarla"
            return
        }
        viewModelScope.launch {
            val now = Instant.now()
            runCatching {
                fixedEntries.post(
                    entry = entry.copy(
                        lastAddedAt = now,
                        lastAddedDate = postingDate,
                    ),
                    transaction = entry.toTransaction(date = postingDate, now = now),
                )
                updateAllFinanceWidgets(context)
            }.onSuccess {
                val kind = if (entry.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"
                message.value = "$kind agregado correctamente"
            }.onFailure { message.value = "No se pudo agregar el movimiento" }
        }
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
    date = date,
    createdAt = now,
    updatedAt = now,
    fixedEntryId = id,
)
