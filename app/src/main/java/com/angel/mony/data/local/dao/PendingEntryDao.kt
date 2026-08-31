package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.angel.mony.data.local.entity.PendingEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEntryDao {
    @Query("SELECT * FROM pending_entries ORDER BY isDone ASC, dateEpochDay ASC, description COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PendingEntryEntity>>

    @Query("SELECT * FROM pending_entries WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): PendingEntryEntity?

    @Query("SELECT * FROM pending_entries WHERE transactionId = :transactionId LIMIT 1")
    suspend fun findByTransactionId(transactionId: Long): PendingEntryEntity?

    @Query("SELECT * FROM pending_entries WHERE sourceShoppingListId = :listId LIMIT 1")
    suspend fun findByShoppingListId(listId: Long): PendingEntryEntity?

    @Upsert
    suspend fun upsert(entry: PendingEntryEntity): Long

    @Query("DELETE FROM pending_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM pending_entries")
    suspend fun getAll(): List<PendingEntryEntity>
}
