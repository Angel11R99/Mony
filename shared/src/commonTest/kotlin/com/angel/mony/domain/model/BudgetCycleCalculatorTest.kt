package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class BudgetCycleCalculatorTest {
    private val today = LocalDate(2026, 8, 13)

    @Test
    fun explicitOpeningAndClosingDaysAreInclusive() {
        val config = customScheduleConfig()

        assertEquals(
            DateRange(LocalDate(2026, 8, 15), LocalDate(2026, 8, 29)),
            activeBudgetPeriod(config, LocalDate(2026, 8, 20)),
        )
        assertEquals(
            DateRange(LocalDate(2026, 8, 30), LocalDate(2026, 9, 14)),
            nextBudgetPeriod(config, LocalDate(2026, 8, 20)),
        )
    }

    @Test
    fun crossMonthCycleIncludesItsClosingDay() {
        val config = customScheduleConfig()

        assertEquals(
            DateRange(LocalDate(2026, 8, 30), LocalDate(2026, 9, 14)),
            activeBudgetPeriod(config, LocalDate(2026, 9, 14)),
        )
        assertEquals(
            DateRange(LocalDate(2026, 9, 15), LocalDate(2026, 9, 29)),
            nextBudgetPeriod(config, LocalDate(2026, 9, 14)),
        )
    }

    @Test
    fun day31IsClampedToEndOfShortMonths() {
        val config = BudgetConfig(
            amountInCents = 100_000,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleSchedules = listOf(BudgetCycleSchedule(1, 15), BudgetCycleSchedule(16, 31)),
        )

        assertEquals(
            DateRange(LocalDate(2026, 2, 16), LocalDate(2026, 2, 28)),
            activeBudgetPeriod(config, LocalDate(2026, 2, 20)),
        )
    }

    @Test
    fun nextFortnightMovesToFollowingMonth() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)

        assertEquals(
            DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 15)),
            budgetPeriodForView(config, BudgetPeriodView.NEXT, LocalDate(2026, 8, 20)),
        )
    }

    @Test
    fun scheduleSupportsCyclesCrossingMonths() {
        assertEquals(
            DateRange(LocalDate(2026, 8, 30), LocalDate(2026, 9, 14)),
            budgetPeriodForSchedule(BudgetCycleSchedule(30, 14), today),
        )
    }

    @Test
    fun manualCloseUsesInclusiveClosingDay() {
        val config = customScheduleConfig()

        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate(2026, 8, 28)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate(2026, 8, 29)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate(2026, 9, 14)))
        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate(2026, 9, 15)))
    }

    @Test
    fun automaticCloseRespectsConfiguredTime() {
        val config = customScheduleConfig()

        assertFalse(
            shouldAutomaticallyCloseBudgetCycle(
                config,
                LocalDateTime(2026, 8, 29, 14, 59),
                LocalTime(15, 0),
            ),
        )
        assertTrue(
            shouldAutomaticallyCloseBudgetCycle(
                config,
                LocalDateTime(2026, 8, 29, 15, 0),
                LocalTime(15, 0),
            ),
        )
    }

    @Test
    fun overdueCycleRemainsEligibleForClose() {
        val config = customScheduleConfig().copy(cycleStart = LocalDate(2026, 8, 15))

        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate(2026, 8, 30)))
        assertTrue(
            shouldAutomaticallyCloseBudgetCycle(
                config,
                LocalDateTime(2026, 8, 30, 8, 0),
                LocalTime(15, 0),
            ),
        )
        assertEquals(
            DateRange(LocalDate(2026, 8, 15), LocalDate(2026, 8, 29)),
            budgetPeriodToClose(config, LocalDate(2026, 8, 30)),
        )
    }

    private fun customScheduleConfig() = BudgetConfig(
        amountInCents = 100_000,
        period = BudgetPeriod.FORTNIGHTLY,
        cycleSchedules = listOf(
            BudgetCycleSchedule(openingDay = 15, closingDay = 29),
            BudgetCycleSchedule(openingDay = 30, closingDay = 14),
        ),
    )
}
