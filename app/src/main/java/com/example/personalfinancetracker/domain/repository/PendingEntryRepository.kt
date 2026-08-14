package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.PendingEntry
import kotlinx.coroutines.flow.Flow

interface PendingEntryRepository {
    fun observeAll(): Flow<List<PendingEntry>>
    suspend fun save(entry: PendingEntry): Long
    suspend fun complete(entry: PendingEntry, transaction: FinanceTransaction)
    suspend fun reopen(entry: PendingEntry)
    suspend fun delete(id: Long)
}