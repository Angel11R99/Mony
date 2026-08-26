package com.example.personalfinancetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.BudgetCycleDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.dao.FixedEntryDao
import com.example.personalfinancetracker.data.local.dao.PendingEntryDao
import com.example.personalfinancetracker.data.local.dao.SavingsGoalDao
import com.example.personalfinancetracker.data.local.database.FinanceDatabase
import com.example.personalfinancetracker.data.repository.RoomCategoryRepository
import com.example.personalfinancetracker.data.repository.RoomBudgetRepository
import com.example.personalfinancetracker.data.repository.RoomTransactionRepository
import com.example.personalfinancetracker.data.repository.RoomFixedEntryRepository
import com.example.personalfinancetracker.data.repository.RoomPendingEntryRepository
import com.example.personalfinancetracker.data.repository.RoomSavingsRepository
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.domain.repository.FixedEntryRepository
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
import com.example.personalfinancetracker.domain.repository.SavingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS budget_config (id INTEGER NOT NULL, amountInCents INTEGER NOT NULL, period TEXT NOT NULL, PRIMARY KEY(id))"
            )
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budget_config ADD COLUMN cycleStartEpochDay INTEGER")
            db.execSQL("ALTER TABLE budget_config ADD COLUMN cycleStartedAtEpochMillis INTEGER")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS budget_cycle_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    period TEXT NOT NULL,
                    budgetAmountInCents INTEGER NOT NULL,
                    incomeInCents INTEGER NOT NULL,
                    expenseInCents INTEGER NOT NULL,
                    startDateEpochDay INTEGER NOT NULL,
                    endDateEpochDay INTEGER NOT NULL,
                    closedAtEpochMillis INTEGER NOT NULL
                )""".trimIndent()
            )
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budget_config ADD COLUMN incomeTransactionId INTEGER")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS fixed_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amountInCents INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    comment TEXT,
                    isActive INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fixed_entries_categoryId ON fixed_entries(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fixed_entries_type ON fixed_entries(type)")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN manualDateMode TEXT NOT NULL DEFAULT 'TODAY'")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN manualSpecificDateEpochDay INTEGER")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN scheduleMode TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN scheduleHour INTEGER NOT NULL DEFAULT 9")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN scheduleSpecificDateEpochDay INTEGER")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN nextRunAtEpochMillis INTEGER")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN lastAddedAtEpochMillis INTEGER")
            db.execSQL("ALTER TABLE fixed_entries ADD COLUMN lastAddedDateEpochDay INTEGER")
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN fixedEntryId INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_fixedEntryId ON transactions(fixedEntryId)")
        }
    }

    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budget_config ADD COLUMN closingDays TEXT NOT NULL DEFAULT '15'")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS pending_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amountInCents INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    comment TEXT,
                    isDone INTEGER NOT NULL,
                    doneAtEpochMillis INTEGER,
                    transactionId INTEGER,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_entries_categoryId ON pending_entries(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_entries_type ON pending_entries(type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_entries_dateEpochDay ON pending_entries(dateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_entries_isDone ON pending_entries(isDone)")
        }
    }

    private val migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE pending_entries ADD COLUMN reminderMinutesOfDay INTEGER")
        }
    }

    private val migration10To11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN budgetLimitInCents INTEGER DEFAULT NULL")
        }
    }

    private val migration11To12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS savings_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    targetAmountInCents INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL("ALTER TABLE transactions ADD COLUMN savingsGoalId INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_savingsGoalId ON transactions(savingsGoalId)")
        }
    }

    private val migration12To13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN completedAtEpochMillis INTEGER DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, "personal_finance.db")
            .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6, migration6To7, migration7To8, migration8To9, migration9To10, migration10To11, migration11To12, migration12To13)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    initialCategories.forEach { (name, type, icon) ->
                        db.execSQL(
                            "INSERT INTO categories (name, type, icon, isActive, createdAtEpochMillis) VALUES (?, ?, ?, 1, ?)",
                            arrayOf<Any>(name, type, icon, now),
                        )
                    }
                }
            })
            .build()

    @Provides fun categoryDao(db: FinanceDatabase): CategoryDao = db.categoryDao()
    @Provides fun transactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao()
    @Provides fun budgetConfigDao(db: FinanceDatabase): BudgetConfigDao = db.budgetConfigDao()
    @Provides fun budgetCycleDao(db: FinanceDatabase): BudgetCycleDao = db.budgetCycleDao()
    @Provides fun fixedEntryDao(db: FinanceDatabase): FixedEntryDao = db.fixedEntryDao()
    @Provides fun pendingEntryDao(db: FinanceDatabase): PendingEntryDao = db.pendingEntryDao()
    @Provides fun savingsGoalDao(db: FinanceDatabase): SavingsGoalDao = db.savingsGoalDao()

    private val initialCategories = listOf(
        Triple("Salario", "INCOME", "payments"),
        Triple("Trabajo extra", "INCOME", "work"),
        Triple("Freelance", "INCOME", "laptop"),
        Triple("Venta", "INCOME", "sell"),
        Triple("Otros ingresos", "INCOME", "add_circle"),
        Triple("Alimentación", "EXPENSE", "restaurant"),
        Triple("Transporte", "EXPENSE", "directions_car"),
        Triple("Vivienda", "EXPENSE", "home"),
        Triple("Servicios", "EXPENSE", "receipt_long"),
        Triple("Internet", "EXPENSE", "wifi"),
        Triple("Teléfono", "EXPENSE", "phone_android"),
        Triple("Salud", "EXPENSE", "medical_services"),
        Triple("Educación", "EXPENSE", "school"),
        Triple("Entretenimiento", "EXPENSE", "movie"),
        Triple("Compras", "EXPENSE", "shopping_cart"),
        Triple("Deudas", "EXPENSE", "credit_card"),
        Triple("Suscripciones", "EXPENSE", "subscriptions"),
        Triple("Familia", "EXPENSE", "family_restroom"),
        Triple("Ahorro", "EXPENSE", "savings"),
        Triple("Emergencias", "EXPENSE", "emergency"),
        Triple("Otros", "EXPENSE", "more_horiz"),
    )
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
}
