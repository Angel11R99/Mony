package com.example.personalfinancetracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetConfigEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetConfigDao(): BudgetConfigDao
}
