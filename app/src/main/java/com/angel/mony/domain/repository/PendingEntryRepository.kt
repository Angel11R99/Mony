package com.angel.mony.domain.repository

import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.PendingEntry
import kotlinx.coroutines.flow.Flow

interface PendingEntryRepository {
    fun observeAll(): Flow<List<PendingEntry>>
    suspend fun get(id: Long): PendingEntry?
    suspend fun save(entry: PendingEntry): Long
    suspend fun complete(entry: PendingEntry, transaction: FinanceTransaction)
    suspend fun reopen(entry: PendingEntry)
    suspend fun delete(id: Long)
}
