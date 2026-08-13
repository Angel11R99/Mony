package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type DESC, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY name")
    fun observeActive(type: String): Flow<List<CategoryEntity>>
}
