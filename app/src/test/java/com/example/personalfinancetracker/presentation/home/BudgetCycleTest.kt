package com.example.personalfinancetracker.presentation.home

import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.activeBudgetPeriod
import com.example.personalfinancetracker.domain.model.availableForBudget
import com.example.personalfinancetracker.domain.model.belongsToActiveBudgetCycle
import com.example.personalfinancetracker.domain.model.canManuallyCloseBudgetCycle
import com.example.personalfinancetracker.domain.model.shouldAutomaticallyCloseBudgetCycle
import com.example.personalfinancetracker.domain.model.budgetCyclePeriodToClose
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

    @Test fun `available is budget minus active cycle expenses`() {
        val config = BudgetConfig(
            amountInCents = 2_500_000,
            period = BudgetPeriod.FORTNIGHTLY,
        )
        val expense = transaction(Instant.parse("2026-08-13T12:00:01Z")).copy(amountInCents = 500_000)

        assertEquals(2_000_000, availableForBudget(config, listOf(expense), today))
    }

    @Test fun `budget income description identifies its period`() {
        assertEquals("Ingreso quincenal", budgetIncomeDescription(BudgetPeriod.FORTNIGHTLY))
        assertEquals("Ingreso mensual", budgetIncomeDescription(BudgetPeriod.MONTHLY))
    }

    @Test fun `cycle only closes manually on configured days`() {
        val config = BudgetConfig(
            100_000,
            BudgetPeriod.FORTNIGHTLY,
            closingDays = listOf(1, 16, 31),
        )

        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 1)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 16)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 31)))
        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 13)))
    }

    @Test fun `new budget defaults to closing on the fifteenth`() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)

        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 15)))
        assertTrue(shouldAutomaticallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 15)))
        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 16)))
    }

    @Test fun `automatic close only runs on configured days`() {
        val config = BudgetConfig(
            100_000,
            BudgetPeriod.FORTNIGHTLY,
            closingDays = listOf(1, 16),
        )

        assertTrue(shouldAutomaticallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 1)))
        assertTrue(shouldAutomaticallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 16)))
        assertFalse(shouldAutomaticallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 31)))
    }

    @Test fun `closing on sixteenth archives period since previous closing day`() {
        assertEquals(
            com.example.personalfinancetracker.domain.model.DateRange(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
            ),
            budgetCyclePeriodToClose(
                BudgetPeriod.FORTNIGHTLY,
                listOf(1, 16),
                LocalDate.of(2026, 8, 16),
            ),
        )
    }

    @Test fun `closing on first archives previous period`() {
        assertEquals(
            com.example.personalfinancetracker.domain.model.DateRange(
                LocalDate.of(2026, 7, 16),
                LocalDate.of(2026, 7, 31),
            ),
            budgetCyclePeriodToClose(
                BudgetPeriod.FORTNIGHTLY,
                listOf(1, 16),
                LocalDate.of(2026, 8, 1),
            ),
        )
    }

    @Test fun `closing on custom day archives since previous custom day`() {
        assertEquals(
            com.example.personalfinancetracker.domain.model.DateRange(
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 19),
            ),
            budgetCyclePeriodToClose(
                BudgetPeriod.FORTNIGHTLY,
                listOf(5, 20),
                LocalDate.of(2026, 8, 20),
            ),
        )
    }

    @Test fun `closing on default fifteenth archives since previous fifteenth`() {
        assertEquals(
            com.example.personalfinancetracker.domain.model.DateRange(
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 8, 14),
            ),
            budgetCyclePeriodToClose(
                BudgetPeriod.FORTNIGHTLY,
                listOf(15),
                LocalDate.of(2026, 8, 15),
            ),
        )
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
