package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.angel.mony.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

data class SavingsGoalWithSaved(
    val id: Long,
    val name: String,
    val targetAmountInCents: Long,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val savedInCents: Long,
)

@Dao
interface SavingsGoalDao {
    @Query(
        """SELECT g.id AS id, g.name AS name, g.targetAmountInCents AS targetAmountInCents,
            g.createdAtEpochMillis AS createdAtEpochMillis,
            g.completedAtEpochMillis AS completedAtEpochMillis,
            COALESCE(SUM(t.amountInCents), 0) AS savedInCents
            FROM savings_goals g
            LEFT JOIN transactions t ON t.savingsGoalId = g.id
            GROUP BY g.id
            ORDER BY g.completedAtEpochMillis IS NULL DESC, g.createdAtEpochMillis DESC"""
    )
    fun observeAllWithSaved(): Flow<List<SavingsGoalWithSaved>>

    @Insert
    suspend fun insert(entity: SavingsGoalEntity): Long

    @Query("UPDATE savings_goals SET name = :name, targetAmountInCents = :targetAmountInCents WHERE id = :id")
    suspend fun update(id: Long, name: String, targetAmountInCents: Long)

    @Query("UPDATE savings_goals SET completedAtEpochMillis = :completedAtEpochMillis WHERE id = :id")
    suspend fun complete(id: Long, completedAtEpochMillis: Long)

    @Query("UPDATE savings_goals SET completedAtEpochMillis = NULL WHERE id = :id")
    suspend fun reopen(id: Long)

    @Query("UPDATE transactions SET savingsGoalId = NULL WHERE savingsGoalId = :goalId")
    suspend fun unlinkTransactions(goalId: Long)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM savings_goals")
    suspend fun getAll(): List<SavingsGoalEntity>
}
