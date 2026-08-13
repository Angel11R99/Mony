package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.personalfinancetracker.data.local.entity.FixedEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedEntryDao {
    @Query("SELECT * FROM fixed_entries ORDER BY isActive DESC, description COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FixedEntryEntity>>

    @Upsert
    suspend fun upsert(entry: FixedEntryEntity): Long

    @Query("DELETE FROM fixed_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
