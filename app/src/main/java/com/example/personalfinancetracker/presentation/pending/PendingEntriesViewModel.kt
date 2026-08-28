package com.example.personalfinancetracker.presentation.pending

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.EntryDisplayPreferences
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.EntryCardSize
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.label
import com.example.personalfinancetracker.domain.model.isPendingReminderInFuture
import com.example.personalfinancetracker.domain.model.isPendingDateValid
import com.example.personalfinancetracker.domain.model.toTransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
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
import java.time.LocalTime
import javax.inject.Inject

data class PendingEntriesUiState(
    val entries: List<PendingEntry> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val isReady: Boolean = false,
)

data class PendingDraftState(
    val type: PendingType,
    val categoryId: Long?,
    val date: LocalDate,
    val reminderTime: LocalTime?,
)

@HiltViewModel
class PendingEntriesViewModel @Inject constructor(
    private val pendingEntries: PendingEntryRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(pendingEntries.observeAll(), categories.observeAll()) { entries, categoryList ->
        PendingEntriesUiState(
            entries = entries,
            categories = categoryList.associateBy(Category::id),
            isReady = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingEntriesUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    private val processingEntryIds = mutableSetOf<Long>()
    private val displayPreferences = EntryDisplayPreferences(context)

    private val _lastDraft = MutableStateFlow(
        PendingDraftState(PendingType.PAYMENT, null, LocalDate.now(), null),
    )
    val lastDraft: StateFlow<PendingDraftState> = _lastDraft

    val cardSize: StateFlow<EntryCardSize> = displayPreferences.pendingCardSize

    fun setCardSize(size: EntryCardSize) = displayPreferences.setPendingCardSize(size)

    fun consumeMessage() { message.value = null }

    fun save(
        existing: PendingEntry?,
        type: PendingType,
        description: String,
        amount: String,
        categoryId: Long?,
        comment: String,
        date: LocalDate,
        reminderTime: LocalTime?,
        onSaved: () -> Unit,
    ) {
        val cents = MoneyFormatter.parseToCents(amount)
        val now = Instant.now()
        when {
            isSaving.value -> Unit
            description.isBlank() -> message.value = "Escribe una descripción"
            cents == null || cents <= 0 -> message.value = "Introduce un monto válido"
            categoryId == null -> message.value = "Selecciona una categoría"
            !isPendingDateValid(date) -> message.value = "La fecha del recordatorio no puede haber pasado"
            reminderTime != null && !isPendingReminderInFuture(date, reminderTime, now) ->
                message.value = "La fecha y hora de la alerta deben estar en el futuro"
            else -> viewModelScope.launch {
                isSaving.value = true
                runCatching {
                    val base = existing ?: PendingEntry(
                        type = type,
                        description = description.trim(),
                        amountInCents = cents,
                        categoryId = categoryId,
                        date = date,
                        reminderTime = reminderTime,
                        createdAt = now,
                        updatedAt = now,
                    )
                    val savedEntry = base.copy(
                        id = existing?.id ?: 0,
                        type = type,
                        description = description.trim(),
                        amountInCents = cents,
                        categoryId = categoryId,
                        date = date,
                        reminderTime = reminderTime,
                        comment = comment.trim().ifBlank { null },
                        updatedAt = now,
                    )
                    val savedId = pendingEntries.save(savedEntry)
                    val persistedEntry = savedEntry.copy(id = existing?.id ?: savedId)
                    PendingReminderScheduler.schedule(context, persistedEntry)
                }.onSuccess {
                    message.value = "Recordatorio de ${type.label()} guardado"
                    if (existing == null) {
                        _lastDraft.value = PendingDraftState(type, categoryId, date, reminderTime)
                    }
                    onSaved()
                }.onFailure { error -> message.value = error.message ?: "No se pudo guardar" }
                isSaving.value = false
            }
        }
    }

    fun toggleDone(entry: PendingEntry) {
        if (!processingEntryIds.add(entry.id)) return
        viewModelScope.launch {
            runCatching {
                if (entry.isDone) {
                    pendingEntries.reopen(entry)
                    PendingReminderScheduler.schedule(
                        context,
                        entry.copy(isDone = false, doneAt = null, transactionId = null),
                    )
                } else {
                    val now = Instant.now()
                    pendingEntries.complete(
                        entry = entry,
                        transaction = entry.toTransaction(now),
                    )
                    PendingReminderScheduler.cancel(context, entry.id)
                }
                updateAllFinanceWidgets(context)
            }.onSuccess {
                message.value = if (entry.isDone) {
                    "Recordatorio restaurado"
                } else {
                    "${entry.type.label()} registrado en el historial"
                }
            }.onFailure { message.value = "No se pudo actualizar el recordatorio" }
            processingEntryIds.remove(entry.id)
        }
    }

    fun delete(entry: PendingEntry) = viewModelScope.launch {
        if (!processingEntryIds.add(entry.id)) return@launch
        runCatching {
            pendingEntries.delete(entry.id)
            PendingReminderScheduler.cancel(context, entry.id)
        }.onSuccess {
            message.value = "${entry.type.label()} eliminado"
        }.onFailure { error ->
            message.value = error.message ?: "No se pudo eliminar el recordatorio"
        }
        processingEntryIds.remove(entry.id)
    }
}

internal fun PendingEntry.toTransaction(now: Instant): FinanceTransaction = FinanceTransaction(
    amountInCents = amountInCents,
    type = type.toTransactionType(),
    categoryId = categoryId,
    description = listOfNotNull(description, comment?.takeIf { it.isNotBlank() })
        .joinToString(" · "),
    date = date,
    createdAt = now,
    updatedAt = now,
)
