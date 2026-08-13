package com.example.personalfinancetracker.presentation.home

import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BudgetCycleTest {
    private val today = LocalDate.of(2026, 8, 13)

    @Test fun `manual close starts a new cycle on closing date`() {
        val config = BudgetConfig(
            amountInCents = 100_000,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleStart = today,
            cycleStartedAt = Instant.parse("2026-08-13T12:00:00Z"),
        )

        val period = activeBudgetPeriod(config, today)

        assertEquals(today, period.start)
        assertEquals(LocalDate.of(2026, 8, 15), period.endInclusive)
    }

    @Test fun `transactions before close are excluded and new ones are included`() {
        val boundary = Instant.parse("2026-08-13T12:00:00Z")
        val config = BudgetConfig(
            amountInCents = 100_000,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleStart = today,
            cycleStartedAt = boundary,
        )
        val period = activeBudgetPeriod(config, today)

        assertFalse(transaction(Instant.parse("2026-08-13T11:59:59Z")).belongsToActiveBudgetCycle(config, period))
        assertTrue(transaction(Instant.parse("2026-08-13T12:00:01Z")).belongsToActiveBudgetCycle(config, period))
    }

    private fun transaction(createdAt: Instant) = FinanceTransaction(
        id = 1,
        amountInCents = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = 1,
        description = null,
        date = today,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
