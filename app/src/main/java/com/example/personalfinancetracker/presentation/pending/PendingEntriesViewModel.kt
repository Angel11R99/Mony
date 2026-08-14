package com.example.personalfinancetracker.presentation.pending

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.label
import com.example.personalfinancetracker.domain.model.toTransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
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

data class PendingEntriesUiState(
    val entries: List<PendingEntry> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
)

@HiltViewModel
class PendingEntriesViewModel @Inject constructor(
    private val pendingEntries: PendingEntryRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(pendingEntries.observeAll(), categories.observeAll()) { entries, categoryList ->
        PendingEntriesUiState(entries, categoryList.associateBy(Category::id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingEntriesUiState())

    val message = MutableStateFlow<String?>(null)

    fun consumeMessage() { message.value = null }

    fun save(
        existing: PendingEntry?,
        type: PendingType,
        description: String,
        amount: String,
        categoryId: Long?,
        comment: String,
        date: LocalDate,
        onSaved: () -> Unit,
    ) {
        val cents = MoneyFormatter.parseToCents(amount)
        when {
            description.isBlank() -> message.value = "Escribe una descripción"
            cents == null || cents <= 0 -> message.value = "Introduce un monto válido"
            categoryId == null -> message.value = "Selecciona una categoría"
            else -> viewModelScope.launch {
                val now = Instant.now()
                runCatching {
                    val base = existing ?: PendingEntry(
                        type = type,
                        description = description.trim(),
                        amountInCents = cents,
                        categoryId = categoryId,
                        date = date,
                        createdAt = now,
                        updatedAt = now,
                    )
                    pendingEntries.save(
                        base.copy(
                            id = existing?.id ?: 0,
                            type = type,
                            description = description.trim(),
                            amountInCents = cents,
                            categoryId = categoryId,
                            date = date,
                            comment = comment.trim().ifBlank { null },
                            updatedAt = now,
                        )
                    )
                }.onSuccess {
                    message.value = "${type.label()} pendiente guardado"
                    onSaved()
                }.onFailure { message.value = "No se pudo guardar" }
            }
        }
    }

    fun toggleDone(entry: PendingEntry) {
        viewModelScope.launch {
            runCatching {
                if (entry.isDone) {
                    pendingEntries.reopen(entry)
                } else {
                    val now = Instant.now()
                    pendingEntries.complete(
                        entry = entry,
                        transaction = entry.toTransaction(now),
                    )
                }
                updateAllFinanceWidgets(context)
            }.onSuccess {
                message.value = if (entry.isDone) {
                    "Pendiente restaurado"
                } else {
                    "${entry.type.label()} registrado en el historial"
                }
            }.onFailure { message.value = "No se pudo actualizar el pendiente" }
        }
    }

    fun delete(entry: PendingEntry) = viewModelScope.launch {
        pendingEntries.delete(entry.id)
        message.value = "${entry.type.label()} eliminado"
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