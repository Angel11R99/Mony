package com.angel.mony.domain.repository

import com.angel.mony.domain.model.SavingsGoalProgress
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    fun observeGoals(): Flow<List<SavingsGoalProgress>>
    suspend fun create(name: String, targetAmountInCents: Long): Long
    suspend fun update(id: Long, name: String, targetAmountInCents: Long)
    suspend fun complete(id: Long)
    suspend fun reopen(id: Long)
    suspend fun delete(id: Long)
}
