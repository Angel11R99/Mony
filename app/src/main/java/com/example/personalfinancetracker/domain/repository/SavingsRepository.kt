package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    fun observeGoals(): Flow<List<SavingsGoalProgress>>
    suspend fun create(name: String, targetAmountInCents: Long): Long
    suspend fun update(id: Long, name: String, targetAmountInCents: Long)
    suspend fun delete(id: Long)
}
