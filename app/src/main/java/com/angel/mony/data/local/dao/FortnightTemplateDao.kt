package com.angel.mony.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.angel.mony.data.local.entity.FortnightTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FortnightTemplateDao {
    @Query("SELECT * FROM fortnight_templates ORDER BY isActive DESC, description ASC")
    fun observeTemplates(): Flow<List<FortnightTemplateEntity>>

    @Query("SELECT * FROM fortnight_templates WHERE isActive = 1 ORDER BY description ASC")
    fun observeActiveTemplates(): Flow<List<FortnightTemplateEntity>>

    @Query("SELECT * FROM fortnight_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplate(id: Long): FortnightTemplateEntity?

    @Query("SELECT * FROM fortnight_templates WHERE id = :id LIMIT 1")
    fun observeTemplate(id: Long): Flow<FortnightTemplateEntity?>

    @Insert
    suspend fun insertTemplate(template: FortnightTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: FortnightTemplateEntity): Int

    @Query("UPDATE fortnight_templates SET isActive = :isActive, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun setTemplateActive(id: Long, isActive: Boolean, updatedAt: Long): Int

    @Query("DELETE FROM fortnight_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long): Int

    @Query("SELECT COUNT(*) FROM fortnight_items WHERE templateId = :id")
    suspend fun countItemsUsingTemplate(id: Long): Int

    @Query("SELECT * FROM fortnight_templates")
    suspend fun getAllTemplates(): List<FortnightTemplateEntity>
}
