package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type DESC, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY name")
    fun observeActive(type: String): Flow<List<CategoryEntity>>

    @Query(
        "SELECT categoryId FROM transactions " +
            "UNION SELECT categoryId FROM fixed_entries " +
            "UNION SELECT categoryId FROM pending_entries"
    )
    fun observeUsedCategoryIds(): Flow<List<Long>>

    @Insert
    suspend fun insert(entity: CategoryEntity)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE categories SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
