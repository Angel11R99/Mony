package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.personalfinancetracker.data.local.entity.PendingEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEntryDao {
    @Query("SELECT * FROM pending_entries ORDER BY isDone ASC, dateEpochDay ASC, description COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PendingEntryEntity>>

    @Query("SELECT * FROM pending_entries WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): PendingEntryEntity?

    @Upsert
    suspend fun upsert(entry: PendingEntryEntity): Long

    @Query("DELETE FROM pending_entries WHERE id = :id")
    suspend fun delete(id: Long)
}