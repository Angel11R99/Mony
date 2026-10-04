package com.angel.mony.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.angel.mony.data.local.dao.BudgetConfigDao
import com.angel.mony.data.local.dao.BudgetCycleDao
import com.angel.mony.data.local.dao.CategoryDao
import com.angel.mony.data.local.dao.ExpenseFundingDao
import com.angel.mony.data.local.dao.FixedEntryDao
import com.angel.mony.data.local.dao.FortnightPaymentDao
import com.angel.mony.data.local.dao.FortnightPlanDao
import com.angel.mony.data.local.dao.FortnightTemplateDao
import com.angel.mony.data.local.dao.PendingEntryDao
import com.angel.mony.data.local.dao.SavingsGoalDao
import com.angel.mony.data.local.dao.ShoppingListDao
import com.angel.mony.data.local.dao.TransactionDao
import com.angel.mony.data.local.database.FinanceDatabase
import com.angel.mony.data.local.database.FinanceMigrationStep
import com.angel.mony.data.local.database.FinanceMigrationSteps
import com.angel.mony.data.local.database.createFinanceDatabase
import com.angel.mony.data.repository.OpenFoodFactsProductCatalogRepository
import com.angel.mony.data.repository.RoomBackupRepository
import com.angel.mony.data.repository.RoomBudgetRepository
import com.angel.mony.data.repository.RoomCategoryRepository
import com.angel.mony.data.repository.RoomExpenseFundingRepository
import com.angel.mony.data.repository.RoomFixedEntryRepository
import com.angel.mony.data.repository.RoomFortnightRepository
import com.angel.mony.data.repository.RoomPendingEntryRepository
import com.angel.mony.data.repository.RoomSavingsRepository
import com.angel.mony.data.repository.RoomShoppingListRepository
import com.angel.mony.data.repository.RoomTransactionRepository
import com.angel.mony.domain.repository.BackupRepository
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.ExpenseFundingRepository
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.domain.repository.FortnightRepository
import com.angel.mony.domain.repository.PendingEntryRepository
import com.angel.mony.domain.repository.ProductCatalogRepository
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.repository.ShoppingListRepository
import com.angel.mony.domain.repository.TransactionRepository
import com.angel.mony.domain.usecase.EvaluateExpenseFunding
import com.angel.mony.domain.usecase.SaveBudget
import com.angel.mony.domain.usecase.SaveExpenseWithFunding
import com.angel.mony.domain.usecase.SaveTransaction
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private fun FinanceMigrationStep.toAndroidMigration() = object : Migration(startVersion, endVersion) {
    override fun migrate(db: SupportSQLiteDatabase) {
        statements.forEach(db::execSQL)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val androidTestMigrations = FinanceMigrationSteps.all.map(FinanceMigrationStep::toAndroidMigration)

    internal val migration13To14 = androidTestMigrations.first { it.startVersion == 13 }
    internal val migration14To15 = androidTestMigrations.first { it.startVersion == 14 }
    internal val migration15To16 = androidTestMigrations.first { it.startVersion == 15 }
    internal val migration16To17 = androidTestMigrations.first { it.startVersion == 16 }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FinanceDatabase =
        createFinanceDatabase(context)

    @Provides fun categoryDao(db: FinanceDatabase): CategoryDao = db.categoryDao()
    @Provides fun transactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao()
    @Provides fun budgetConfigDao(db: FinanceDatabase): BudgetConfigDao = db.budgetConfigDao()
    @Provides fun budgetCycleDao(db: FinanceDatabase): BudgetCycleDao = db.budgetCycleDao()
    @Provides fun fixedEntryDao(db: FinanceDatabase): FixedEntryDao = db.fixedEntryDao()
    @Provides fun pendingEntryDao(db: FinanceDatabase): PendingEntryDao = db.pendingEntryDao()
    @Provides fun savingsGoalDao(db: FinanceDatabase): SavingsGoalDao = db.savingsGoalDao()
    @Provides fun shoppingListDao(db: FinanceDatabase): ShoppingListDao = db.shoppingListDao()
    @Provides fun fortnightTemplateDao(db: FinanceDatabase): FortnightTemplateDao = db.fortnightTemplateDao()
    @Provides fun fortnightPlanDao(db: FinanceDatabase): FortnightPlanDao = db.fortnightPlanDao()
    @Provides fun fortnightPaymentDao(db: FinanceDatabase): FortnightPaymentDao = db.fortnightPaymentDao()
    @Provides fun expenseFundingDao(db: FinanceDatabase): ExpenseFundingDao = db.expenseFundingDao()

    @Provides
    fun evaluateExpenseFunding(
        transactions: TransactionRepository,
        expenseFunding: ExpenseFundingRepository,
    ): EvaluateExpenseFunding = EvaluateExpenseFunding(transactions, expenseFunding)

    @Provides
    fun saveBudget(
        budgets: BudgetRepository,
        categories: CategoryRepository,
        transactions: TransactionRepository,
    ): SaveBudget = SaveBudget(budgets, categories, transactions)

    @Provides
    fun saveExpenseWithFunding(
        transactions: TransactionRepository,
    ): SaveExpenseWithFunding = SaveExpenseWithFunding(transactions)

    @Provides
    fun saveTransaction(transactions: TransactionRepository): SaveTransaction = SaveTransaction(transactions)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun transactions(implementation: RoomTransactionRepository): TransactionRepository
    @Binds abstract fun categories(implementation: RoomCategoryRepository): CategoryRepository
    @Binds abstract fun budget(implementation: RoomBudgetRepository): BudgetRepository
    @Binds abstract fun fixedEntries(implementation: RoomFixedEntryRepository): FixedEntryRepository
    @Binds abstract fun pendingEntries(implementation: RoomPendingEntryRepository): PendingEntryRepository
    @Binds abstract fun savings(implementation: RoomSavingsRepository): SavingsRepository
    @Binds abstract fun shoppingLists(implementation: RoomShoppingListRepository): ShoppingListRepository
    @Binds abstract fun fortnights(implementation: RoomFortnightRepository): FortnightRepository
    @Binds abstract fun backup(implementation: RoomBackupRepository): BackupRepository
    @Binds abstract fun expenseFunding(implementation: RoomExpenseFundingRepository): ExpenseFundingRepository
    @Binds abstract fun productCatalog(implementation: OpenFoodFactsProductCatalogRepository): ProductCatalogRepository
}
