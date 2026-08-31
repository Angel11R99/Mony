package com.angel.mony.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.angel.mony.data.local.dao.CategoryDao
import com.angel.mony.data.local.dao.BudgetConfigDao
import com.angel.mony.data.local.dao.BudgetCycleDao
import com.angel.mony.data.local.dao.TransactionDao
import com.angel.mony.data.local.dao.FixedEntryDao
import com.angel.mony.data.local.dao.PendingEntryDao
import com.angel.mony.data.local.dao.SavingsGoalDao
import com.angel.mony.data.local.dao.ShoppingListDao
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.data.local.entity.FixedEntryEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.entity.ProductRecognitionAliasEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetConfigEntity::class,
        BudgetCycleEntity::class,
        FixedEntryEntity::class,
        PendingEntryEntity::class,
        SavingsGoalEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        ShoppingAdjustmentEntity::class,
        KnownProductEntity::class,
        ProductRecognitionAliasEntity::class,
    ],
    version = 15,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetConfigDao(): BudgetConfigDao
    abstract fun budgetCycleDao(): BudgetCycleDao
    abstract fun fixedEntryDao(): FixedEntryDao
    abstract fun pendingEntryDao(): PendingEntryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun shoppingListDao(): ShoppingListDao
}
