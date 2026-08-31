package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.angel.mony.data.local.entity.FixedEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedEntryDao {
    @Query("SELECT * FROM fixed_entries ORDER BY isActive DESC, description COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FixedEntryEntity>>

    @Upsert
    suspend fun upsert(entry: FixedEntryEntity): Long

    @Query("DELETE FROM fixed_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE fixed_entries SET lastAddedAtEpochMillis = :addedAt, lastAddedDateEpochDay = :date WHERE id = :id")
    suspend fun updateLastAdded(id: Long, addedAt: Long?, date: Long?)

    @Query("SELECT id FROM fixed_entries WHERE lastAddedAtEpochMillis = :addedAt LIMIT 1")
    suspend fun findIdByLastAddedAt(addedAt: Long): Long?
}
