package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochDay BETWEEN :start AND :end ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeByPeriod(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): TransactionEntity?

    @Insert suspend fun insert(transaction: TransactionEntity): Long
    @Update suspend fun update(transaction: TransactionEntity)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun delete(id: Long)
}
