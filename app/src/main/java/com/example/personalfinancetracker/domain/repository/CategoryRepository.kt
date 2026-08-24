package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActive(type: TransactionType): Flow<List<Category>>
    fun observeAll(): Flow<List<Category>>
    fun observeUsedCategoryIds(): Flow<Set<Long>>
    suspend fun create(name: String, type: TransactionType, budgetLimitInCents: Long?)
    suspend fun update(id: Long, name: String, budgetLimitInCents: Long?)
    suspend fun setActive(id: Long, isActive: Boolean)
    suspend fun deleteIfUnused(id: Long): Boolean
}
