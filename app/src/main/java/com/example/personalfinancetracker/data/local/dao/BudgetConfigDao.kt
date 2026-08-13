package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetConfigDao {
    @Query("SELECT * FROM budget_config WHERE id = 1")
    fun observe(): Flow<BudgetConfigEntity?>

    @Upsert
    suspend fun upsert(config: BudgetConfigEntity)
}
