package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<FinanceTransaction>>
    fun observeByPeriod(period: DateRange): Flow<List<FinanceTransaction>>
    suspend fun get(id: Long): FinanceTransaction?
    suspend fun create(transaction: FinanceTransaction): Long
    suspend fun update(transaction: FinanceTransaction)
    suspend fun delete(id: Long)
}
