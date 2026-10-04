package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FortnightPaymentDao {
    @Query("SELECT * FROM fortnight_payments ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAllPayments(): Flow<List<FortnightPaymentEntity>>

    @Query(
        """SELECT fortnight_payments.* FROM fortnight_payments
            INNER JOIN fortnight_items ON fortnight_items.id = fortnight_payments.itemId
            WHERE fortnight_items.planId = :planId
            ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC"""
    )
    fun observePayments(planId: Long): Flow<List<FortnightPaymentEntity>>

    @Query(
        """SELECT fortnight_payments.* FROM fortnight_payments
            INNER JOIN fortnight_items ON fortnight_items.id = fortnight_payments.itemId
            WHERE fortnight_items.planId = :planId
            ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC"""
    )
    suspend fun getPayments(planId: Long): List<FortnightPaymentEntity>

    @Query("SELECT * FROM fortnight_payments WHERE id = :id LIMIT 1")
    suspend fun getPayment(id: Long): FortnightPaymentEntity?

    @Query("SELECT * FROM fortnight_payments WHERE itemId = :itemId ORDER BY createdAtEpochMillis ASC")
    suspend fun getPaymentsForItem(itemId: Long): List<FortnightPaymentEntity>

    @Query("SELECT COUNT(*) FROM fortnight_payments WHERE itemId = :itemId")
    suspend fun countPaymentsForItem(itemId: Long): Int

    @Query("SELECT COUNT(*) FROM fortnight_payments WHERE itemId IN (SELECT id FROM fortnight_items WHERE planId = :planId)")
    suspend fun countPaymentsForPlan(planId: Long): Int

    @Query("SELECT itemId FROM fortnight_payments WHERE transactionId = :transactionId LIMIT 1")
    suspend fun findItemIdByTransaction(transactionId: Long): Long?

    @Insert
    suspend fun insertPayment(payment: FortnightPaymentEntity): Long

    @Query("DELETE FROM fortnight_payments WHERE id = :id")
    suspend fun deletePayment(id: Long): Int

    @Query("SELECT * FROM fortnight_payments")
    suspend fun getAllPayments(): List<FortnightPaymentEntity>
}
