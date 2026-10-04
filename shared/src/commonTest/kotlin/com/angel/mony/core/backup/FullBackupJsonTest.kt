package com.angel.mony.core.backup

import com.angel.mony.domain.model.BackupBudgetConfig
import com.angel.mony.domain.model.BackupBudgetCycle
import com.angel.mony.domain.model.BackupCategory
import com.angel.mony.domain.model.BackupExpenseFunding
import com.angel.mony.domain.model.BackupFixedEntry
import com.angel.mony.domain.model.BackupFortnightItem
import com.angel.mony.domain.model.BackupFortnightPayment
import com.angel.mony.domain.model.BackupFortnightPlan
import com.angel.mony.domain.model.BackupFortnightTemplate
import com.angel.mony.domain.model.BackupKnownProduct
import com.angel.mony.domain.model.BackupPendingEntry
import com.angel.mony.domain.model.BackupProductAlias
import com.angel.mony.domain.model.BackupSavingsGoal
import com.angel.mony.domain.model.BackupShoppingAdjustment
import com.angel.mony.domain.model.BackupShoppingItem
import com.angel.mony.domain.model.BackupShoppingList
import com.angel.mony.domain.model.BackupTransaction
import com.angel.mony.domain.model.FullBackupSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullBackupJsonTest {
    @Test
    fun completeSnapshotRoundTripsWithoutLosingRelationships() {
        val snapshot = completeSnapshot()

        val content = FullBackupJson.encode(snapshot, exportedAtEpochMillis = 123_456L)
        val restored = FullBackupJson.decode(content)

        assertEquals(snapshot, restored)
        assertTrue(content.contains("\"version\": 3"))
        assertTrue(content.contains("\"expenseFunding\""))
        assertEquals(1L, restored.expenseFunding.single().transactionId)
    }

    @Test
    fun versionTwoWithoutNewCollectionsUsesEmptyDefaults() {
        val content = """
            {
              "version": 2,
              "exportedAt": 1,
              "categories": [],
              "transactions": [],
              "fixedEntries": [],
              "pendingEntries": [],
              "budgetConfig": null,
              "budgetCycles": [],
              "savingsGoals": [],
              "shoppingLists": [],
              "shoppingItems": [],
              "shoppingAdjustments": []
            }
        """.trimIndent()

        val restored = FullBackupJson.decode(content)

        assertTrue(restored.fortnightPlans.isEmpty())
        assertTrue(restored.expenseFunding.isEmpty())
    }

    @Test
    fun futureVersionWithUnknownFieldsKeepsKnownData() {
        val content = FullBackupJson.encode(completeSnapshot(), exportedAtEpochMillis = 1L)
            .replace("\"version\": 3", "\"version\": 4")
            .replaceFirst("{", "{\n  \"futureField\": true,")

        val restored = FullBackupJson.decode(content)

        assertEquals(completeSnapshot(), restored)
    }

    private fun completeSnapshot() = FullBackupSnapshot(
        categories = listOf(
            BackupCategory(1L, "Salario", "INCOME", "payments", true, 10L, 50_000L)
        ),
        transactions = listOf(
            BackupTransaction(1L, 100_000L, "INCOME", 1L, "Nómina", 20L, 21L, 22L, null, null)
        ),
        fixedEntries = listOf(
            BackupFixedEntry(
                id = 1L,
                type = "EXPENSE",
                description = "Renta",
                amountInCents = 30_000L,
                categoryId = 1L,
                comment = "Mensual",
                isActive = true,
                manualDateMode = "TODAY",
                manualSpecificDateEpochDay = null,
                scheduleMode = "MONTHLY",
                scheduleHour = 9,
                scheduleSpecificDateEpochDay = null,
                nextRunAtEpochMillis = 30L,
                lastAddedAtEpochMillis = 31L,
                lastAddedDateEpochDay = 20L,
            )
        ),
        pendingEntries = listOf(
            BackupPendingEntry(
                id = 1L,
                type = "PAYMENT",
                description = "Factura",
                amountInCents = 2_000L,
                categoryId = 1L,
                dateEpochDay = 20L,
                reminderMinutesOfDay = 540,
                comment = null,
                isDone = true,
                doneAtEpochMillis = 40L,
                transactionId = 1L,
                createdAtEpochMillis = 41L,
                updatedAtEpochMillis = 42L,
                sourceShoppingListId = 1L,
            )
        ),
        budgetConfig = BackupBudgetConfig(1, 100_000L, "MONTHLY", 1L, 2L, 1L, "15"),
        budgetCycles = listOf(
            BackupBudgetCycle(1L, "MONTHLY", 100_000L, 100_000L, 20_000L, 1L, 30L, 50L)
        ),
        savingsGoals = listOf(BackupSavingsGoal(1L, "Emergencia", 500_000L, 60L, null)),
        shoppingLists = listOf(
            BackupShoppingList(1L, "Supermercado", "COMPLETED", 10_000L, 1L, null, 20L, "DEBIT", 1L, 70L, 71L, 72L)
        ),
        shoppingItems = listOf(
            BackupShoppingItem(1L, 1L, "Arroz", 2, 100L, 120L, "123", true, true, null, 73L, 74L)
        ),
        shoppingAdjustments = listOf(
            BackupShoppingAdjustment(1L, 1L, "Descuento", false, 50L, 75L)
        ),
        knownProducts = listOf(BackupKnownProduct("123", "Arroz", 120L, 76L)),
        productAliases = listOf(
            BackupProductAlias(1L, "ARROZ 1LB", "arroz 1lb", "Arroz", "123", 2, 77L)
        ),
        fortnightTemplates = listOf(
            BackupFortnightTemplate(1L, "Ahorro", 1_000L, 2_000L, 1L, "SAVINGS", null, true, 80L, 81L)
        ),
        fortnightPlans = listOf(
            BackupFortnightPlan(1L, 1L, 15L, "FIRST", 50_000L, "OPEN", 82L, null)
        ),
        fortnightItems = listOf(
            BackupFortnightItem(1L, 1L, 1L, "Ahorro", 1_000L, 1L, "SAVINGS", 1L, null, 0, 83L, 84L)
        ),
        fortnightPayments = listOf(
            BackupFortnightPayment(1L, 1L, 500L, 10L, 1L, 85L)
        ),
        expenseFunding = listOf(
            BackupExpenseFunding(1L, 1L, 500L, "Ahorros", 10L, 86L, 87L)
        ),
    )
}
