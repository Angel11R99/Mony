package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.BudgetConfig
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observe(): Flow<BudgetConfig?>
    suspend fun save(config: BudgetConfig)
}
