package com.angel.mony.presentation.transactions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.TransactionRepository
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

enum class TransactionField { AMOUNT, CATEGORY, DATE }

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    categories: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val type = TransactionType.valueOf(savedStateHandle.get<String>("type") ?: TransactionType.EXPENSE.name)
    val categories: StateFlow<List<Category>> = categories.observeActive(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val error = MutableStateFlow<String?>(null)
    val fieldErrors = MutableStateFlow<Map<TransactionField, String>>(emptyMap())
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

    // Funding dialog state
    private val _showFundingDialog = MutableStateFlow<ExpenseCreationResult.RequiresFundingSource?>(null)
    val showFundingDialog: StateFlow<ExpenseCreationResult.RequiresFundingSource?> = _showFundingDialog
    private var pendingTransaction: FinanceTransaction? = null
    private var pendingOnSaved: (() -> Unit)? = null

    init {
        if (transactionId != 0L) viewModelScope.launch {
            editingTransaction.value = transactionRepository.get(transactionId)
        }
    }

    fun consumeError() {
        error.value = null
    }

    fun clearFieldError(field: TransactionField) {
        val current = fieldErrors.value
        if (field in current) {
            fieldErrors.value = current - field
        }
    }

    fun save(amount: String, categoryId: Long?, note: String, date: String, onSaved: () -> Unit) {
        if (saving.value) return
        val cents = MoneyFormatter.parseToCents(amount)
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()

        val errors = mutableMapOf<TransactionField, String>()
        if (cents == null || cents <= 0) errors[TransactionField.AMOUNT] = "Ingresa un monto válido"
        if (categoryId == null) errors[TransactionField.CATEGORY] = "Selecciona una categoría"
        if (parsedDate == null) errors[TransactionField.DATE] = "Selecciona una fecha válida"

        if (errors.isNotEmpty()) {
            fieldErrors.value = errors
            return
        }

        val transaction = FinanceTransaction(
            id = if (isEditing) transactionId else 0,
            amountInCents = cents!!,
            type = type,
            categoryId = categoryId!!,
            description = note,
            date = parsedDate!!,
            createdAt = editingTransaction.value?.createdAt ?: Instant.now(),
            updatedAt = Instant.now(),
            fixedEntryId = editingTransaction.value?.fixedEntryId,
            savingsGoalId = editingTransaction.value?.savingsGoalId,
        )

        if (isEditing) {
            saveExistingTransaction(transaction, onSaved)
        } else {
            saveNewTransaction(transaction, onSaved)
        }
    }

    private fun saveNewTransaction(transaction: FinanceTransaction, onSaved: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            val result = transactionRepository.createWithFunding(transaction, null)
            saving.value = false
            handleResult(result, transaction, onSaved)
        }
    }

    private fun saveExistingTransaction(transaction: FinanceTransaction, onSaved: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            val result = transactionRepository.updateWithFunding(transaction, transactionId, null)
            saving.value = false
            handleResult(result, transaction, onSaved)
        }
    }

    private suspend fun handleResult(
        result: ExpenseCreationResult,
        transaction: FinanceTransaction,
        onSaved: () -> Unit,
    ) {
        when (result) {
            is ExpenseCreationResult.Saved -> {
                context.showToast(if (isEditing) {
                    "Movimiento actualizado correctamente"
                } else {
                    if (type == TransactionType.EXPENSE) "Gasto guardado correctamente" else "Ingreso guardado correctamente"
                })
                fieldErrors.value = emptyMap()
                onSaved()
                runCatching { updateAllFinanceWidgets(context) }
            }
            is ExpenseCreationResult.RequiresFundingSource -> {
                pendingTransaction = transaction
                pendingOnSaved = onSaved
                _showFundingDialog.value = result
            }
            is ExpenseCreationResult.Error -> {
                error.value = result.message
            }
        }
    }

    fun confirmFundingSource(sourceDescription: String) {
        val result = _showFundingDialog.value
            ?: return
        val transaction = pendingTransaction
            ?: return
        val onSaved = pendingOnSaved
            ?: return

        _showFundingDialog.value = null
        pendingTransaction = null
        pendingOnSaved = null

        viewModelScope.launch {
            saving.value = true
            val finalResult = if (isEditing) {
                transactionRepository.updateWithFunding(transaction, transactionId, sourceDescription)
            } else {
                transactionRepository.createWithFunding(transaction, sourceDescription)
            }
            saving.value = false
            when (finalResult) {
                is ExpenseCreationResult.Saved -> {
                    context.showToast(if (isEditing) {
                        "Movimiento actualizado correctamente"
                    } else {
                        if (type == TransactionType.EXPENSE) "Gasto guardado correctamente" else "Ingreso guardado correctamente"
                    })
                    fieldErrors.value = emptyMap()
                    onSaved()
                    runCatching { updateAllFinanceWidgets(context) }
                }
                is ExpenseCreationResult.Error -> {
                    error.value = finalResult.message
                    // Re-show dialog on error
                    _showFundingDialog.value = result
                    pendingTransaction = transaction
                    pendingOnSaved = onSaved
                }
                is ExpenseCreationResult.RequiresFundingSource -> {
                    // Should not happen since we provided a source
                    _showFundingDialog.value = finalResult
                    pendingTransaction = transaction
                    pendingOnSaved = onSaved
                }
            }
        }
    }

    fun cancelFundingDialog() {
        _showFundingDialog.value = null
        pendingTransaction = null
        pendingOnSaved = null
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
