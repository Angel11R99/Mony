package com.example.personalfinancetracker.presentation.savings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.SavingsRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
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
) {
    val activeGoals: List<SavingsGoalProgress> get() = goals.filter { it.isActive }
    val completedGoals: List<SavingsGoalProgress> get() = goals.filter { !it.isActive }
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SavingsViewModel @Inject constructor(
    private val savings: SavingsRepository,
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(
        savings.observeGoals(),
        categories.observeActive(TransactionType.EXPENSE),
    ) { goals, expenseCategories ->
        val ahorro = expenseCategories.firstOrNull { it.name.equals(SAVINGS_CATEGORY, ignoreCase = true) }
        SavingsUiState(goals, ahorro?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    private     val pendingDeleteGoal = MutableStateFlow<SavingsGoalProgress?>(null)
    val pendingDelete: StateFlow<SavingsGoalProgress?> = pendingDeleteGoal

    val pendingCompleteGoal = MutableStateFlow<SavingsGoalProgress?>(null)
    val pendingComplete: StateFlow<SavingsGoalProgress?> = pendingCompleteGoal

    val selectedGoal = MutableStateFlow<SavingsGoalProgress?>(null)

    val contributions: StateFlow<List<FinanceTransaction>> = selectedGoal
        .flatMapLatest { goal ->
            if (goal == null) flowOf(emptyList())
            else transactions.observeBySavingsGoal(goal.goal.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            else -> viewModelScope.launch {
                isSaving.value = true
                val categoryId = state.value.savingsCategoryId
                runCatching {
                    val now = Instant.now()
                    val note = rawDescription.trim()
                    transactions.create(
                        FinanceTransaction(
                            amountInCents = cents,
                            type = TransactionType.EXPENSE,
                            categoryId = categoryId!!,
                            description = if (note.isEmpty()) "$CONTRIBUTION_PREFIX${goal.goal.name}"
                            else "$CONTRIBUTION_PREFIX${goal.goal.name} · $note",
                            date = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                            createdAt = now,
                            updatedAt = now,
                            savingsGoalId = goal.goal.id,
                        )
                    )
                    updateAllFinanceWidgets(context)
                }.onSuccess {
                    message.value = "Aporte registrado."
                    onDone()
                }.onFailure { message.value = "No se pudo registrar el aporte" }
                isSaving.value = false
            }
        }
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
