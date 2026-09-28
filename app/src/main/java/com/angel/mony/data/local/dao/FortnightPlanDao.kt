package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.angel.mony.data.local.entity.FortnightPlanEntity
import com.angel.mony.data.local.entity.FortnightPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FortnightPlanDao {
    @Query("SELECT * FROM fortnight_plans ORDER BY startDateEpochDay DESC")
    fun observePlans(): Flow<List<FortnightPlanEntity>>

    @Query("SELECT * FROM fortnight_plans WHERE id = :id LIMIT 1")
    fun observePlan(id: Long): Flow<FortnightPlanEntity?>

    @Query("SELECT * FROM fortnight_items ORDER BY planId ASC, position ASC, createdAtEpochMillis ASC")
    fun observeAllItems(): Flow<List<FortnightPlanItemEntity>>

    @Query("SELECT * FROM fortnight_plans WHERE id = :id LIMIT 1")
    suspend fun getPlan(id: Long): FortnightPlanEntity?

    @Query("SELECT * FROM fortnight_plans WHERE startDateEpochDay = :startEpochDay AND endDateEpochDay = :endEpochDay LIMIT 1")
    suspend fun getPlanForPeriod(startEpochDay: Long, endEpochDay: Long): FortnightPlanEntity?

    @Query("SELECT * FROM fortnight_items WHERE planId = :planId ORDER BY position ASC, createdAtEpochMillis ASC")
    fun observeItems(planId: Long): Flow<List<FortnightPlanItemEntity>>

    @Query("SELECT * FROM fortnight_items WHERE planId = :planId ORDER BY position ASC, createdAtEpochMillis ASC")
    suspend fun getItems(planId: Long): List<FortnightPlanItemEntity>

    @Query("SELECT * FROM fortnight_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: Long): FortnightPlanItemEntity?

    @Query("SELECT COALESCE(SUM(plannedAmountInCents), 0) FROM fortnight_items WHERE planId = :planId")
    suspend fun getPlannedTotal(planId: Long): Long

    @Insert
    suspend fun insertPlan(plan: FortnightPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: FortnightPlanEntity): Int

    @Insert
    suspend fun insertItem(item: FortnightPlanItemEntity): Long

    @Update
    suspend fun updateItem(item: FortnightPlanItemEntity): Int

    @Query("DELETE FROM fortnight_items WHERE id = :id")
    suspend fun deleteItem(id: Long): Int

    @Query("DELETE FROM fortnight_plans WHERE id = :id")
    suspend fun deletePlan(id: Long): Int

    @Query("UPDATE fortnight_plans SET status = 'CLOSED', closedAtEpochMillis = :closedAt WHERE id = :id AND status != 'CLOSED'")
    suspend fun closePlan(id: Long, closedAt: Long): Int

    @Query("UPDATE fortnight_plans SET status = 'OPEN', closedAtEpochMillis = NULL WHERE id = :id AND status = 'CLOSED'")
    suspend fun reopenPlan(id: Long): Int

    @Query("SELECT * FROM fortnight_plans")
    suspend fun getAllPlans(): List<FortnightPlanEntity>

    @Query("SELECT * FROM fortnight_items")
    suspend fun getAllItems(): List<FortnightPlanItemEntity>
}
