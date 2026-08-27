package com.example.personalfinancetracker.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinancetracker.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListMigrationTest {
    private val databaseName = "shopping-list-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinanceDatabase::class.java,
    )

    @Test
    fun migration13To14PreservesFinanceDataAndCreatesShoppingTables() {
        helper.createDatabase(databaseName, 13).apply {
            execSQL(
                """INSERT INTO categories
                    (id, name, type, icon, isActive, createdAtEpochMillis, budgetLimitInCents)
                    VALUES (42, 'Compras', 'EXPENSE', 'shopping_cart', 1, 1000, NULL)""",
            )
            execSQL(
                """INSERT INTO transactions
                    (id, amountInCents, type, categoryId, description, dateEpochDay,
                     createdAtEpochMillis, updatedAtEpochMillis, fixedEntryId, savingsGoalId)
                    VALUES (7, 309140, 'EXPENSE', 42, 'Compra anterior', 20000,
                            1000, 1000, NULL, NULL)""",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            14,
            true,
            DatabaseModule.migration13To14,
        ).use { db ->
            db.query("SELECT name FROM categories WHERE id = 42").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Compras", cursor.getString(0))
            }
            db.query("SELECT amountInCents FROM transactions WHERE id = 7").use { cursor ->
                cursor.moveToFirst()
                assertEquals(309140L, cursor.getLong(0))
            }

            db.execSQL(
                """INSERT INTO shopping_lists
                    (id, name, status, budgetInCents, expenseTransactionId,
                     createdAtEpochMillis, updatedAtEpochMillis, completedAtEpochMillis)
                    VALUES (1, 'Supermercado', 'COMPLETED', 500000, 7, 1000, 2000, 2000)""",
            )
            db.execSQL(
                """INSERT INTO shopping_list_items
                    (id, shoppingListId, name, quantity, estimatedUnitPriceInCents,
                     actualUnitPriceInCents, barcode, isPurchased, isIdentified, notes,
                     createdAtEpochMillis, updatedAtEpochMillis)
                    VALUES (1, 1, 'Leche', 2, NULL, 8500, '7460000000000', 1, 1,
                            NULL, 1000, 2000)""",
            )
            db.query("SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.execSQL("DELETE FROM transactions WHERE id = 7")
            db.query("SELECT expenseTransactionId FROM shopping_lists WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
            }
            db.execSQL("DELETE FROM shopping_lists WHERE id = 1")
            db.query("SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }
}
