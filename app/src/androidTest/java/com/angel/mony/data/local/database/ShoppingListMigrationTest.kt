package com.angel.mony.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.angel.mony.di.DatabaseModule
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
            db.execSQL("PRAGMA foreign_keys=ON")
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

    @Test
    fun migration14To15PreservesLinksAndAddsPurchaseMetadata() {
        helper.createDatabase("$databaseName-15", 14).apply {
            execSQL("INSERT INTO categories (id, name, type, icon, isActive, createdAtEpochMillis, budgetLimitInCents) VALUES (42, 'Compras', 'EXPENSE', 'shopping_cart', 1, 1000, NULL)")
            execSQL("INSERT INTO transactions (id, amountInCents, type, categoryId, description, dateEpochDay, createdAtEpochMillis, updatedAtEpochMillis, fixedEntryId, savingsGoalId) VALUES (7, 15000, 'EXPENSE', 42, 'Compra', 20000, 1000, 1000, NULL, NULL)")
            execSQL("INSERT INTO shopping_lists (id, name, status, budgetInCents, expenseTransactionId, createdAtEpochMillis, updatedAtEpochMillis, completedAtEpochMillis) VALUES (1, 'Compra', 'COMPLETED', NULL, 7, 1000, 1000, 1000)")
            close()
        }

        helper.runMigrationsAndValidate(
            "$databaseName-15",
            15,
            true,
            DatabaseModule.migration14To15,
        ).use { db ->
            db.query("SELECT expenseTransactionId, payableId, purchaseDateEpochDay, paymentMethod, expenseCategoryId FROM shopping_lists WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(7L, cursor.getLong(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(20000L, cursor.getLong(2))
                assertEquals("DEBIT", cursor.getString(3))
                assertEquals(42L, cursor.getLong(4))
            }
            db.query("SELECT COUNT(*) FROM product_recognition_aliases").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM pragma_table_info('pending_entries') WHERE name = 'sourceShoppingListId'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }
}
