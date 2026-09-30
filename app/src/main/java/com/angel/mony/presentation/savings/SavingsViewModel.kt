package com.angel.mony.presentation.savings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.EntryDisplayPreferences
import com.angel.mony.core.FinanceDataCache
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.EntryCardSize
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.repository.TransactionRepository
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SavingsUiState(
    val goals: List<SavingsGoalProgress> = emptyList(),
    val savingsCategoryId: Long? = null,
    val isReady: Boolean = false,
) {
    val activeGoals: List<SavingsGoalProgress> get() = goals.filter { it.isActive }
    val completedGoals: List<SavingsGoalProgress> get() = goals.filter { !it.isActive }
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SavingsViewModel @Inject constructor(
    private val savings: SavingsRepository,
    private val transactions: TransactionRepository,
    dataCache: FinanceDataCache,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val displayPreferences = EntryDisplayPreferences(context)

    val state = combine(
        dataCache.savingsGoals,
        dataCache.categories,
    ) { goals, expenseCategories ->
        val ahorro = expenseCategories.firstOrNull {
            it.type == TransactionType.EXPENSE && it.isActive &&
                it.name.equals(SAVINGS_CATEGORY, ignoreCase = true)
        }
        SavingsUiState(
            goals = goals,
            savingsCategoryId = ahorro?.id,
            isReady = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SavingsUiState(
            goals = dataCache.savingsGoals.value,
            savingsCategoryId = dataCache.categories.value.firstOrNull {
                it.type == TransactionType.EXPENSE && it.isActive &&
                    it.name.equals(SAVINGS_CATEGORY, ignoreCase = true)
            }?.id,
            isReady = true,
        ),
    )

    val cardSize: StateFlow<EntryCardSize> = displayPreferences.savingsCardSize
    fun setCardSize(size: EntryCardSize) = displayPreferences.setSavingsCardSize(size)

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    private     val pendingDeleteGoal = MutableStateFlow<SavingsGoalProgress?>(null)
    val pendingDelete: StateFlow<SavingsGoalProgress?> = pendingDeleteGoal

    val pendingCompleteGoal = MutableStateFlow<SavingsGoalProgress?>(null)
    val pendingComplete: StateFlow<SavingsGoalProgress?> = pendingCompleteGoal

    private val pendingReopenGoal = MutableStateFlow<SavingsGoalProgress?>(null)
    val pendingReopen: StateFlow<SavingsGoalProgress?> = pendingReopenGoal

    val selectedGoal = MutableStateFlow<SavingsGoalProgress?>(null)

    val contributions: StateFlow<List<FinanceTransaction>> = selectedGoal
        .flatMapLatest { goal ->
            if (goal == null) flowOf(emptyList())
            else transactions.observeBySavingsGoal(goal.goal.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Funding dialog state
    private val _showFundingDialog = MutableStateFlow<ExpenseCreationResult.RequiresFundingSource?>(null as ExpenseCreationResult.RequiresFundingSource?)
    val showFundingDialog: StateFlow<ExpenseCreationResult.RequiresFundingSource?> = _showFundingDialog
    private var pendingContribution: Pair<SavingsGoalProgress, FinanceTransaction>? = null

    fun openContributions(goal: SavingsGoalProgress) {
        selectedGoal.value = goal
    }

    fun closeContributions() {
        selectedGoal.value = null
    }

    fun consumeMessage() {
        message.value = null
    }

    fun create(rawName: String, rawTarget: String, onSaved: () -> Unit) =
        persist(rawName, rawTarget, "Meta creada correctamente.", onSaved) { name, target ->
            savings.create(name, target)
        }

    fun update(goalId: Long, rawName: String, rawTarget: String, onSaved: () -> Unit) =
        persist(rawName, rawTarget, "Meta actualizada.", onSaved) { name, target ->
            savings.update(goalId, name, target)
        }

    fun requestDelete(goal: SavingsGoalProgress) {
        pendingDeleteGoal.value = goal
    }

    fun cancelDelete() {
        pendingDeleteGoal.value = null
    }

    fun requestComplete(goal: SavingsGoalProgress) {
        pendingCompleteGoal.value = goal
    }

    fun cancelComplete() {
        pendingCompleteGoal.value = null
    }

    fun confirmComplete() {
        val goal = pendingCompleteGoal.value ?: return
        viewModelScope.launch {
            runCatching { savings.complete(goal.goal.id) }
                .onSuccess {
                    message.value = "Meta completada."
                    if (selectedGoal.value?.goal?.id == goal.goal.id) closeContributions()
                }
                .onFailure { message.value = "No se pudo completar la meta" }
            pendingCompleteGoal.value = null
        }
    }

    fun requestReopen(goal: SavingsGoalProgress) {
        pendingReopenGoal.value = goal
    }

    fun cancelReopen() {
        pendingReopenGoal.value = null
    }

    fun confirmReopen() {
        val goal = pendingReopenGoal.value ?: return
        viewModelScope.launch {
            runCatching { savings.reopen(goal.goal.id) }
                .onSuccess { message.value = "Meta reabierta." }
                .onFailure { message.value = "No se pudo reabrir la meta" }
            pendingReopenGoal.value = null
        }
    }

    fun confirmDelete() {
        val goal = pendingDeleteGoal.value ?: return
        viewModelScope.launch {
            runCatching { savings.delete(goal.goal.id) }
                .onSuccess {
                    message.value = "Meta eliminada. Sus aportes quedan en el historial."
                    if (selectedGoal.value?.goal?.id == goal.goal.id) closeContributions()
                }
                .onFailure { message.value = "No se pudo eliminar la meta" }
            pendingDeleteGoal.value = null
        }
    }

    fun contribute(
        goal: SavingsGoalProgress,
        rawAmount: String,
        rawDescription: String,
        onDone: () -> Unit,
    ) {
        if (isSaving.value) return
        val cents = MoneyFormatter.parseToCents(rawAmount)
        when {
            cents == null || cents <= 0 -> {
                message.value = "Introduce un monto válido"
                return
            }
            state.value.savingsCategoryId == null -> {
                message.value = "Necesitas la categoría \"$SAVINGS_CATEGORY\" activa para aportar"
                return
            }
            else -> {
                val now = Instant.now()
                val note = rawDescription.trim()
                val transaction = FinanceTransaction(
                    amountInCents = cents,
                    type = TransactionType.EXPENSE,
                    categoryId = state.value.savingsCategoryId!!,
                    description = if (note.isEmpty()) "$CONTRIBUTION_PREFIX${goal.goal.name}"
                    else "$CONTRIBUTION_PREFIX${goal.goal.name} · $note",
                    date = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    createdAt = now,
                    updatedAt = now,
                    savingsGoalId = goal.goal.id,
                )
                viewModelScope.launch {
                    isSaving.value = true
                    val result = transactions.createWithFunding(transaction, null)
                    isSaving.value = false
                    handleContributionResult(result, transaction, goal, onDone)
                }
            }
        }
    }

    private fun handleContributionResult(
        result: ExpenseCreationResult,
        transaction: FinanceTransaction,
        goal: SavingsGoalProgress,
        onDone: () -> Unit,
    ) {
        when (result) {
            is ExpenseCreationResult.Saved -> {
                message.value = "Aporte registrado."
                viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
                onDone()
            }
            is ExpenseCreationResult.RequiresFundingSource -> {
                pendingContribution = Pair(goal, transaction)
                _showFundingDialog.value = result
            }
            is ExpenseCreationResult.Error -> {
                message.value = result.message
            }
        }
    }

    fun confirmFundingSource(sourceDescription: String) {
        val result = _showFundingDialog.value ?: return
        val contribution = pendingContribution ?: return

        _showFundingDialog.value = null
        pendingContribution = null

        viewModelScope.launch {
            isSaving.value = true
            val finalResult = transactions.createWithFunding(contribution.second, sourceDescription)
            isSaving.value = false
            when (finalResult) {
                is ExpenseCreationResult.Saved -> {
                    message.value = "Aporte registrado."
                    viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
                    // Note: onDone is not accessible here, the UI should handle navigation
                }
                is ExpenseCreationResult.Error -> {
                    message.value = finalResult.message
                    _showFundingDialog.value = result
                    pendingContribution = contribution
                }
                is ExpenseCreationResult.RequiresFundingSource -> {
                    _showFundingDialog.value = finalResult
                    pendingContribution = contribution
                }
            }
        }
    }

    fun cancelFundingDialog() {
        _showFundingDialog.value = null
        pendingContribution = null
    }

    private fun persist(
        rawName: String,
        rawTarget: String,
        successMessage: String,
        onSaved: () -> Unit,
        action: suspend (String, Long) -> Unit,
    ) {
        if (isSaving.value) return
        val name = rawName.trim()
        val target = MoneyFormatter.parseToCents(rawTarget)
        when {
            name.isEmpty() -> {
                message.value = "Escribe el nombre de la meta"
                return
            }
            target == null || target <= 0 -> {
                message.value = "El objetivo debe ser mayor que cero"
                return
            }
        }
        viewModelScope.launch {
            isSaving.value = true
            runCatching { action(name, target) }
                .onSuccess {
                    message.value = successMessage
                    onSaved()
                }
                .onFailure { message.value = "No se pudo guardar la meta" }
            isSaving.value = false
        }
    }

    companion object {
        const val SAVINGS_CATEGORY = "Ahorro"
        const val CONTRIBUTION_PREFIX = "Aporte · "
    }
}
