package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.angel.mony.data.local.entity.BudgetCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCycleDao {
    @Query("SELECT * FROM budget_cycle_history ORDER BY closedAtEpochMillis DESC")
    fun observeAll(): Flow<List<BudgetCycleEntity>>

    @Insert
    suspend fun insert(cycle: BudgetCycleEntity)
}
