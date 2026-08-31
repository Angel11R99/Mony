package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.angel.mony.data.local.entity.BudgetConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetConfigDao {
    @Query("SELECT * FROM budget_config WHERE id = 1")
    fun observe(): Flow<BudgetConfigEntity?>

    @Upsert
    suspend fun upsert(config: BudgetConfigEntity)
}
