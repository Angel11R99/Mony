package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personalfinancetracker.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

data class SavingsGoalWithSaved(
    val id: Long,
    val name: String,
    val targetAmountInCents: Long,
    val createdAtEpochMillis: Long,
    val savedInCents: Long,
)

@Dao
interface SavingsGoalDao {
    @Query(
        """SELECT g.id AS id, g.name AS name, g.targetAmountInCents AS targetAmountInCents,
            g.createdAtEpochMillis AS createdAtEpochMillis,
            COALESCE(SUM(t.amountInCents), 0) AS savedInCents
            FROM savings_goals g
            LEFT JOIN transactions t ON t.savingsGoalId = g.id
            GROUP BY g.id
            ORDER BY g.createdAtEpochMillis DESC"""
    )
    fun observeAllWithSaved(): Flow<List<SavingsGoalWithSaved>>

    @Insert
    suspend fun insert(entity: SavingsGoalEntity): Long

    @Query("UPDATE savings_goals SET name = :name, targetAmountInCents = :targetAmountInCents WHERE id = :id")
    suspend fun update(id: Long, name: String, targetAmountInCents: Long)

    @Query("UPDATE transactions SET savingsGoalId = NULL WHERE savingsGoalId = :goalId")
    suspend fun unlinkTransactions(goalId: Long)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
