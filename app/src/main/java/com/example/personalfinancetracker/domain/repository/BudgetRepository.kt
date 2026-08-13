package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycle
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observe(): Flow<BudgetConfig?>
    fun observeHistory(): Flow<List<BudgetCycle>>
    suspend fun save(config: BudgetConfig)
    suspend fun closeCycle(cycle: BudgetCycle, nextConfig: BudgetConfig)
}
