package com.example.personalfinancetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    val budget = budgetRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val message = MutableStateFlow<String?>(null)

    fun consumeMessage() {
        message.value = null
    }

    fun updateClosingDays(days: List<Int>) {
        val sanitized = days.filter { it in 1..31 }.distinct().sorted()
        if (sanitized.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val current = budgetRepository.observe().first()
                    ?: error("Primero configura un presupuesto")
                budgetRepository.save(current.copy(closingDays = sanitized))
            }.onSuccess {
                message.value = "Días de cierre guardados correctamente"
            }.onFailure {
                message.value = it.message ?: "No se pudieron guardar los días de cierre"
            }
        }
    }
}
