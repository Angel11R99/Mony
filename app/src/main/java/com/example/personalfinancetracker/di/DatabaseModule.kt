package com.example.personalfinancetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.database.FinanceDatabase
import com.example.personalfinancetracker.data.repository.RoomCategoryRepository
import com.example.personalfinancetracker.data.repository.RoomBudgetRepository
import com.example.personalfinancetracker.data.repository.RoomTransactionRepository
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
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

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, "personal_finance.db")
            .addMigrations(migration1To2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    initialCategories.forEach { (name, type, icon) ->
                        db.execSQL(
                            "INSERT INTO categories (name, type, icon, isActive, createdAtEpochMillis) VALUES (?, ?, ?, 1, ?)",
                            arrayOf(name, type, icon, now),
                        )
                    }
                }
            })
            .build()

    @Provides fun categoryDao(db: FinanceDatabase): CategoryDao = db.categoryDao()
    @Provides fun transactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao()
    @Provides fun budgetConfigDao(db: FinanceDatabase): BudgetConfigDao = db.budgetConfigDao()

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
}
