package com.example.personalfinancetracker.presentation.transactions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.domain.usecase.SaveTransaction
import com.example.personalfinancetracker.widget.FinanceWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    categories: CategoryRepository,
    private val saveTransaction: SaveTransaction,
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val type = TransactionType.valueOf(savedStateHandle.get<String>("type") ?: TransactionType.EXPENSE.name)
    val categories: StateFlow<List<Category>> = categories.observeActive(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val editingTransaction = MutableStateFlow<FinanceTransaction?>(null)
    private val transactionId = savedStateHandle.get<Long>("transactionId") ?: 0L

    init {
        if (transactionId != 0L) viewModelScope.launch {
            editingTransaction.value = transactionRepository.get(transactionId)
        }
    }

    fun save(amount: String, categoryId: Long?, note: String, date: String, onSaved: () -> Unit) {
        val cents = MoneyFormatter.parseToCents(amount)
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
        when {
            cents == null || cents <= 0 -> error.value = "Introduce un monto válido"
            categoryId == null -> error.value = "Selecciona una categoría"
            parsedDate == null -> error.value = "Usa una fecha válida (AAAA-MM-DD)"
            else -> viewModelScope.launch {
                saving.value = true
                runCatching {
                    val existing = editingTransaction.value
                    saveTransaction(FinanceTransaction(
                        id = existing?.id ?: 0,
                        amountInCents = cents,
                        type = type,
                        categoryId = categoryId,
                        description = note,
                        date = parsedDate,
                        createdAt = existing?.createdAt ?: Instant.now(),
                        updatedAt = Instant.now(),
                    ))
                    FinanceWidget().updateAll(context)
                }.onSuccess { onSaved() }
                    .onFailure { error.value = it.message ?: "No se pudo guardar" }
                saving.value = false
            }
        }
    }
}
