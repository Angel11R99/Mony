package com.angel.mony.presentation.transactions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.TransactionRepository
import com.angel.mony.domain.usecase.SaveTransaction
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    categories: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val saveTransaction: SaveTransaction,
    private val transactionRepository: TransactionRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val type = TransactionType.valueOf(savedStateHandle.get<String>("type") ?: TransactionType.EXPENSE.name)
    val categories: StateFlow<List<Category>> = categories.observeActive(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val editingTransaction = MutableStateFlow<FinanceTransaction?>(null)
    private val transactionId = savedStateHandle.get<Long>("transactionId") ?: 0L
    val isEditing: Boolean = transactionId != 0L
    val suggestedCategoryId: StateFlow<Long?> = transactionRepository.observeAll()
        .map { transactions -> lastCategoryForType(transactions, type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val suggestedDate: StateFlow<LocalDate?> = transactionRepository.observeAll()
        .map { transactions -> lastDateForType(transactions, type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val activePeriod: StateFlow<DateRange> = budgetRepository.observe()
        .map { budget -> activeBudgetPeriod(budget) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DateRange.currentFortnight())

    init {
        if (transactionId != 0L) viewModelScope.launch {
            editingTransaction.value = transactionRepository.get(transactionId)
        }
    }

    fun consumeError() {
        error.value = null
    }

    fun save(amount: String, categoryId: Long?, note: String, date: String, onSaved: () -> Unit) {
        if (saving.value) return
        val cents = MoneyFormatter.parseToCents(amount)
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
        when {
            cents == null || cents <= 0 -> error.value = "Introduce un monto válido"
            categoryId == null -> error.value = "Selecciona una categoría"
            parsedDate == null -> error.value = "Usa una fecha válida (AAAA-MM-DD)"
            else -> viewModelScope.launch {
                saving.value = true
                val result = runCatching {
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
                        fixedEntryId = existing?.fixedEntryId,
                        savingsGoalId = existing?.savingsGoalId,
                    ))
                }
                saving.value = false
                result.onSuccess {
                    context.showToast(if (isEditing) {
                        "Movimiento actualizado correctamente"
                    } else {
                        if (type == TransactionType.EXPENSE) "Gasto guardado correctamente" else "Ingreso guardado correctamente"
                    })
                    onSaved()
                    withContext(Dispatchers.IO + NonCancellable) {
                        runCatching { updateAllFinanceWidgets(context) }
                    }
                }.onFailure { error.value = it.message ?: "No se pudo guardar" }
            }
        }
    }
}

internal fun lastCategoryForType(
    transactions: List<FinanceTransaction>,
    type: TransactionType,
): Long? = transactions.firstOrNull { it.type == type }?.categoryId

internal fun lastDateForType(
    transactions: List<FinanceTransaction>,
    type: TransactionType,
): LocalDate? = transactions.firstOrNull { it.type == type }?.date
