package com.angel.mony.domain.repository

import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycle
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observe(): Flow<BudgetConfig?>
    fun observeHistory(): Flow<List<BudgetCycle>>
    suspend fun save(config: BudgetConfig)
    suspend fun closeCycle(cycle: BudgetCycle, nextConfig: BudgetConfig)
}
