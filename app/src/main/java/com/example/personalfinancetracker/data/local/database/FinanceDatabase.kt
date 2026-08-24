package com.example.personalfinancetracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.BudgetCycleDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.dao.FixedEntryDao
import com.example.personalfinancetracker.data.local.dao.PendingEntryDao
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import com.example.personalfinancetracker.data.local.entity.BudgetCycleEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.data.local.entity.FixedEntryEntity
import com.example.personalfinancetracker.data.local.entity.PendingEntryEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetConfigEntity::class, BudgetCycleEntity::class, FixedEntryEntity::class, PendingEntryEntity::class],
    version = 11,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetConfigDao(): BudgetConfigDao
    abstract fun budgetCycleDao(): BudgetCycleDao
    abstract fun fixedEntryDao(): FixedEntryDao
    abstract fun pendingEntryDao(): PendingEntryDao
}
