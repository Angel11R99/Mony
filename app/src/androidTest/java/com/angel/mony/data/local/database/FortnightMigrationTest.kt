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
class FortnightMigrationTest {
    private val databaseName = "fortnight-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinanceDatabase::class.java,
    )

    @Test
    fun migration15To16PreservesFinanceDataAndCreatesFortnightTables() {
        helper.createDatabase(databaseName, 15).apply {
            execSQL(
                """INSERT INTO categories
                    (id, name, type, icon, isActive, createdAtEpochMillis, budgetLimitInCents)
                    VALUES (42, 'Alimentación', 'EXPENSE', 'restaurant', 1, 1000, NULL)""",
            )
            execSQL(
                """INSERT INTO transactions
                    (id, amountInCents, type, categoryId, description, dateEpochDay,
                     createdAtEpochMillis, updatedAtEpochMillis, fixedEntryId, savingsGoalId)
                    VALUES (7, 150000, 'EXPENSE', 42, 'Compra anterior', 20000,
                            1000, 1000, NULL, NULL)""",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            16,
            true,
            DatabaseModule.migration15To16,
        ).use { db ->
            db.query("SELECT amountInCents FROM transactions WHERE id = 7").use { cursor ->
                cursor.moveToFirst()
                assertEquals(150000L, cursor.getLong(0))
            }

            db.execSQL(
                """INSERT INTO fortnight_templates
                    (id, description, firstFortnightAmountInCents, secondFortnightAmountInCents,
                     categoryId, type, note, isActive, createdAtEpochMillis, updatedAtEpochMillis)
                    VALUES (1, 'Mercado básico', 150000, NULL, 42, 'EXPENSE',
                            'Semanal', 1, 1000, 1000)""",
            )
            db.execSQL(
                """INSERT INTO fortnight_plans
                    (id, startDateEpochDay, endDateEpochDay, slot, budgetInCents,
                     status, createdAtEpochMillis, closedAtEpochMillis)
                    VALUES (1, 20000, 20014, 'FIRST', 300000, 'OPEN', 1000, NULL)""",
            )
            db.execSQL(
                """INSERT INTO fortnight_items
                    (id, planId, templateId, description, plannedAmountInCents,
                     categoryId, type, savingsGoalId, note, position,
                     createdAtEpochMillis, updatedAtEpochMillis)
                    VALUES (1, 1, 1, 'Mercado básico', 150000, 42, 'EXPENSE',
                            NULL, NULL, 0, 1000, 1000)""",
            )
            db.execSQL(
                """INSERT INTO fortnight_payments
                    (id, itemId, amountInCents, dateEpochDay, transactionId, createdAtEpochMillis)
                    VALUES (1, 1, 150000, 20001, 7, 2000)""",
            )
            db.query("SELECT description FROM fortnight_items WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Mercado básico", cursor.getString(0))
            }

            db.execSQL("PRAGMA foreign_keys=ON")
            db.execSQL("DELETE FROM transactions WHERE id = 7")
            db.query("SELECT transactionId FROM fortnight_payments WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
            }
            db.execSQL("DELETE FROM fortnight_plans WHERE id = 1")
            db.query("SELECT COUNT(*) FROM fortnight_items WHERE planId = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM fortnight_payments WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM fortnight_templates WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }
}