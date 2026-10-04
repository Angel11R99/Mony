package com.angel.mony.core

import com.angel.mony.data.local.entity.ExpenseFundingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullBackupExporterTest {
    @Test
    fun expenseFundingSurvivesJsonRoundTrip() {
        val funding = ExpenseFundingEntity(
            id = 9L,
            transactionId = 4L,
            amountInCents = 12_500L,
            sourceDescription = "Cuenta de ahorros",
            dateEpochDay = 20_000L,
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
        )
        val snapshot = FullBackupSnapshot(
            categories = emptyList(),
            transactions = emptyList(),
            fixedEntries = emptyList(),
            pendingEntries = emptyList(),
            budgetConfig = null,
            budgetCycles = emptyList(),
            savingsGoals = emptyList(),
            shoppingLists = emptyList(),
            shoppingItems = emptyList(),
            shoppingAdjustments = emptyList(),
            expenseFunding = listOf(funding),
        )

        val content = FullBackupExporter.buildFullBackupJson(snapshot)
        val restored = FullBackupExporter.parseJsonBackup(content)

        assertTrue(content.contains("\"expenseFunding\""))
        assertEquals(listOf(funding), restored.expenseFunding)
    }
}
