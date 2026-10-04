package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.angel.mony.data.local.entity.ExpenseFundingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseFundingDao {
    @Query("SELECT * FROM expense_funding WHERE transactionId = :transactionId LIMIT 1")
    fun observeByTransaction(transactionId: Long): Flow<ExpenseFundingEntity?>

    @Query("SELECT * FROM expense_funding WHERE dateEpochDay BETWEEN :start AND :end ORDER BY dateEpochDay DESC")
    suspend fun getByPeriod(start: Long, end: Long): List<ExpenseFundingEntity>

    @Query("SELECT * FROM expense_funding WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransaction(transactionId: Long): ExpenseFundingEntity?

    @Query("SELECT * FROM expense_funding WHERE dateEpochDay BETWEEN :start AND :end ORDER BY dateEpochDay DESC")
    fun observeByPeriod(start: Long, end: Long): Flow<List<ExpenseFundingEntity>>

    @Query("SELECT * FROM expense_funding ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<ExpenseFundingEntity>>

    @Insert suspend fun insert(funding: ExpenseFundingEntity): Long
    @Update suspend fun update(funding: ExpenseFundingEntity)
    @Query("DELETE FROM expense_funding WHERE transactionId = :transactionId") suspend fun deleteByTransaction(transactionId: Long)
    @Query("SELECT SUM(amountInCents) FROM expense_funding WHERE dateEpochDay BETWEEN :start AND :end") suspend fun sumByPeriod(start: Long, end: Long): Long?
    @Query("SELECT * FROM expense_funding") suspend fun getAll(): List<ExpenseFundingEntity>
}
