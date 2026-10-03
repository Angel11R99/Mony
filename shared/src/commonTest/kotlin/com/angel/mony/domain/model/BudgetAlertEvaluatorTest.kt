package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class BudgetAlertEvaluatorTest {
    private fun transaction(
        type: TransactionType,
        amountInCents: Long,
        date: LocalDate = LocalDate(2026, 8, 10),
    ) = FinanceTransaction(
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

    @Test
    fun mapsUsagePercentToAlertLevels() {
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.forUsagePercent(0))
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.forUsagePercent(74))
        assertEquals(BudgetAlertLevel.WARNING, BudgetAlertLevel.forUsagePercent(75))
        assertEquals(BudgetAlertLevel.WARNING, BudgetAlertLevel.forUsagePercent(99))
        assertEquals(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.forUsagePercent(100))
        assertEquals(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.forUsagePercent(250))
    }

    @Test
    fun notifiesWhenLevelIncreasesAndStoresCurrentLevel() {
        val warning = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.NONE, BudgetAlertLevel.WARNING)
        assertTrue(warning.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, warning.levelToStore)

        val exceeded = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.WARNING, BudgetAlertLevel.EXCEEDED)
        assertTrue(exceeded.shouldNotify)
        assertEquals(BudgetAlertLevel.EXCEEDED, exceeded.levelToStore)
    }

    @Test
    fun sameOrLowerLevelDoesNotNotifyAndRearms() {
        val same = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.WARNING, BudgetAlertLevel.WARNING)
        assertFalse(same.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, same.levelToStore)

        val dropped = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING)
        assertFalse(dropped.shouldNotify)
        assertEquals(BudgetAlertLevel.WARNING, dropped.levelToStore)

        val rearmed = BudgetAlertEvaluator.evaluate(dropped.levelToStore, BudgetAlertLevel.EXCEEDED)
        assertTrue(rearmed.shouldNotify)
    }

    @Test
    fun returnsBelowThresholdResetStoredLevel() {
        val reset = BudgetAlertEvaluator.evaluate(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.NONE)
        assertFalse(reset.shouldNotify)
        assertEquals(BudgetAlertLevel.NONE, reset.levelToStore)

        val warnedAgain = BudgetAlertEvaluator.evaluate(reset.levelToStore, BudgetAlertLevel.WARNING)
        assertTrue(warnedAgain.shouldNotify)
    }

    @Test
    fun computesUsagePercentFromCycleExpenses() {
        val period = DateRange(LocalDate(2026, 8, 1), LocalDate(2026, 8, 15))
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, 7_500_00),
            transaction(TransactionType.EXPENSE, 500_00),
            transaction(TransactionType.INCOME, 9_999_00),
            transaction(TransactionType.EXPENSE, 1_000_00, LocalDate(2026, 7, 31)),
        )

        assertEquals(80, budgetUsagePercent(budget(10_000_00), transactions, period))
    }

    @Test
    fun zeroBudgetHasZeroUsage() {
        val period = DateRange(LocalDate(2026, 8, 1), LocalDate(2026, 8, 15))
        assertEquals(
            0,
            budgetUsagePercent(budget(0), listOf(transaction(TransactionType.EXPENSE, 100)), period),
        )
    }
}
