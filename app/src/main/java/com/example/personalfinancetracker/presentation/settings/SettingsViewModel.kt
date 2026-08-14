package com.example.personalfinancetracker.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val budget = budgetRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val message = MutableStateFlow<String?>(null)
    val isSavingCycles = MutableStateFlow(false)

    fun consumeMessage() {
        message.value = null
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
}
