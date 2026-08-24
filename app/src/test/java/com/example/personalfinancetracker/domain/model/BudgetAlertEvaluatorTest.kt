package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BudgetAlertEvaluatorTest {
    private fun transaction(
        type: TransactionType,
        amountInCents: Long,
        date: LocalDate = LocalDate.of(2026, 8, 10),
    ) = FinanceTransaction(
        id = 0,
        amountInCents = amountInCents,
        type = type,
        categoryId = 1,
        description = null,
        date = date,
        createdAt = Instant.parse("2026-08-10T12:00:00Z"),
    )

    private fun budget(amountInCents: Long) = BudgetConfig(
        amountInCents = amountInCents,
        period = BudgetPeriod.FORTNIGHTLY,
    )

    @Test fun `maps usage percent to alert levels`() {
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.forUsagePercent(0))
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.forUsagePercent(74))
        assertEquals(BudgetAlertLevel.WARNING, BudgetAlertLevel.forUsagePercent(75))
        assertEquals(BudgetAlertLevel.WARNING, BudgetAlertLevel.forUsagePercent(99))
        assertEquals(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.forUsagePercent(100))
        assertEquals(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.forUsagePercent(250))
    }

    @Test fun `notifies when level increases and stores current`() {
        val decision = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.NONE, BudgetAlertLevel.WARNING)
        assertTrue(decision.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, decision.levelToStore)

        val exceeded = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.WARNING, BudgetAlertLevel.EXCEEDED)
        assertTrue(exceeded.shouldNotify)
        assertEquals(BudgetAlertLevel.EXCEEDED, exceeded.levelToStore)
    }

    @Test fun `does not notify for same or lower level but re-arms`() {
        val same = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.WARNING, BudgetAlertLevel.WARNING)
        assertFalse(same.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, same.levelToStore)

        val dropped = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING)
        assertFalse(dropped.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, dropped.levelToStore)

        val reArmed = BudgetAlertEvaluator.evaluate(dropped.levelToStore, BudgetAlertLevel.EXCEEDED)
        assertTrue(reArmed.shouldNotify)
    }

    @Test fun `resets stored level when usage returns below thresholds`() {
        val reset = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.NONE)
        assertFalse(reset.shouldNotify)
        assertEquals(BudgetAlertLevel.NONE, reset.levelToStore)

        val warnedAgain = BudgetAlertEvaluator.evaluate(reset.levelToStore, BudgetAlertLevel.WARNING)
        assertTrue(warnedAgain.shouldNotify)
    }

    @Test fun `computes usage percent from cycle expenses`() {
        val config = budget(10_000_00)
        val period = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15))
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, 7_500_00),
            transaction(TransactionType.EXPENSE, 500_00),
            transaction(TransactionType.INCOME, 9_999_00),
            transaction(TransactionType.EXPENSE, 1_000_00, date = LocalDate.of(2026, 7, 31)),
        )
        assertEquals(80, budgetUsagePercent(config, transactions, period))
    }

    @Test fun `guards against zero budget`() {
        val period = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15))
        assertEquals(0, budgetUsagePercent(budget(0), listOf(transaction(TransactionType.EXPENSE, 100)), period))
    }
}
