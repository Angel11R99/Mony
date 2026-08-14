package com.example.personalfinancetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun updateClosingDays(days: List<Int>) {
        val sanitized = days.filter { it in 1..31 }.distinct().sorted()
        if (sanitized.isEmpty()) return
        viewModelScope.launch {
            val current = budgetRepository.observe().first() ?: return@launch
            budgetRepository.save(current.copy(closingDays = sanitized))
        }
    }
}
