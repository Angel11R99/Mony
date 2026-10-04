package com.angel.mony.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.datetime.Clock

const val FINANCE_DATABASE_NAME = "personal_finance.db"

internal fun configureFinanceDatabase(
    builder: RoomDatabase.Builder<FinanceDatabase>,
): RoomDatabase.Builder<FinanceDatabase> = builder
    .setDriver(BundledSQLiteDriver())
    .addMigrations(*FinanceMigrationSteps.roomMigrations.toTypedArray())
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(connection: SQLiteConnection) {
            seedInitialCategories(connection, Clock.System.now().toEpochMilliseconds())
        }
    })

internal fun seedInitialCategories(connection: SQLiteConnection, createdAtEpochMillis: Long) {
    connection.prepare(
        "INSERT INTO categories (name, type, icon, isActive, createdAtEpochMillis) " +
            "VALUES (?, ?, ?, 1, ?)"
    ).use { statement ->
        initialCategories.forEach { (name, type, icon) ->
            statement.bindText(1, name)
            statement.bindText(2, type)
            statement.bindText(3, icon)
            statement.bindLong(4, createdAtEpochMillis)
            statement.step()
            statement.reset()
            statement.clearBindings()
        }
    }
}

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
