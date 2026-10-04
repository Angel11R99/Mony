package com.angel.mony.data.local.database

import androidx.room.Database
import androidx.room.ConstructedBy
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.angel.mony.data.local.dao.CategoryDao
import com.angel.mony.data.local.dao.BudgetConfigDao
import com.angel.mony.data.local.dao.BudgetCycleDao
import com.angel.mony.data.local.dao.ExpenseFundingDao
import com.angel.mony.data.local.dao.FortnightPaymentDao
import com.angel.mony.data.local.dao.FortnightPlanDao
import com.angel.mony.data.local.dao.FortnightTemplateDao
import com.angel.mony.data.local.dao.TransactionDao
import com.angel.mony.data.local.dao.FixedEntryDao
import com.angel.mony.data.local.dao.PendingEntryDao
import com.angel.mony.data.local.dao.SavingsGoalDao
import com.angel.mony.data.local.dao.ShoppingListDao
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.ExpenseFundingEntity
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import com.angel.mony.data.local.entity.FortnightPlanEntity
import com.angel.mony.data.local.entity.FortnightPlanItemEntity
import com.angel.mony.data.local.entity.FortnightTemplateEntity
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
        FortnightTemplateEntity::class,
        FortnightPlanEntity::class,
        FortnightPlanItemEntity::class,
        FortnightPaymentEntity::class,
        ExpenseFundingEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
@ConstructedBy(FinanceDatabaseConstructor::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetConfigDao(): BudgetConfigDao
    abstract fun budgetCycleDao(): BudgetCycleDao
    abstract fun fixedEntryDao(): FixedEntryDao
    abstract fun pendingEntryDao(): PendingEntryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun fortnightTemplateDao(): FortnightTemplateDao
    abstract fun fortnightPlanDao(): FortnightPlanDao
    abstract fun fortnightPaymentDao(): FortnightPaymentDao
    abstract fun expenseFundingDao(): ExpenseFundingDao
}

@Suppress("KotlinNoActualForExpect")
expect object FinanceDatabaseConstructor : RoomDatabaseConstructor<FinanceDatabase> {
    override fun initialize(): FinanceDatabase
}
