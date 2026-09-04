package com.angel.mony.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.BudgetAlertPreferences
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.usecase.SaveBudget
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val saveBudgetUseCase: SaveBudget,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val alertPreferences = BudgetAlertPreferences(context)

    val budget = budgetRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val message = MutableStateFlow<String?>(null)
    val isSavingCycles = MutableStateFlow(false)
    val isSavingBudget = MutableStateFlow(false)
    val alertsEnabled: StateFlow<Boolean> = alertPreferences.alertsEnabled

    fun consumeMessage() {
        message.value = null
    }

    fun setAlertsEnabled(enabled: Boolean) {
        alertPreferences.setAlertsEnabled(enabled)
        message.value = if (enabled) "Alertas de presupuesto activadas"
        else "Alertas de presupuesto desactivadas"
    }

    fun updateCycleSchedules(schedules: List<BudgetCycleSchedule>) {
        if (schedules.isEmpty() || isSavingCycles.value) return
        viewModelScope.launch {
            isSavingCycles.value = true
            runCatching {
                val current = budgetRepository.observe().first()
                    ?: error("Primero configura un presupuesto")
                budgetRepository.save(current.copy(cycleSchedules = schedules.distinct()))
                updateAllFinanceWidgets(context)
            }.onSuccess {
                message.value = "Ciclos guardados correctamente"
            }.onFailure {
                message.value = it.message ?: "No se pudieron guardar los ciclos"
            }
            isSavingCycles.value = false
        }
    }

    fun saveBudget(amount: String, period: BudgetPeriod, onSaved: () -> Unit) {
        val amountInCents = MoneyFormatter.parseToCents(amount)
        if (amountInCents == null || amountInCents <= 0 || isSavingBudget.value) return
        persistBudget(
            amountInCents = amountInCents,
            period = period,
            successMessage = "Presupuesto guardado correctamente",
            onSaved = onSaved,
        )
    }

    fun updateBudgetPeriod(period: BudgetPeriod) {
        val current = budget.value ?: return
        if (current.period == period || isSavingBudget.value) return
        persistBudget(
            amountInCents = current.amountInCents,
            period = period,
            successMessage = "Tipo de ciclo actualizado correctamente",
        )
    }

    private fun persistBudget(
        amountInCents: Long,
        period: BudgetPeriod,
        successMessage: String,
        onSaved: () -> Unit = {},
    ) {
        isSavingBudget.value = true
        viewModelScope.launch {
            runCatching {
                saveBudgetUseCase(amountInCents, period)
                updateAllFinanceWidgets(context)
            }.onSuccess {
                message.value = successMessage
                onSaved()
            }.onFailure {
                message.value = it.message ?: "No se pudo guardar el presupuesto"
            }
            isSavingBudget.value = false
        }
    }
}
