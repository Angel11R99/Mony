package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class FortnightPeriodTest {
    private val defaultSchedules = defaultCycleSchedules(BudgetPeriod.FORTNIGHTLY)

    @Test
    fun defaultCalendarUsesFirstAndSecondHalfOfMonth() {
        assertEquals(
            DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-01"), null),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-16"), null),
        )
    }

    @Test
    fun firstSlotIncludesDayFifteen() {
        assertEquals(
            LocalDate.parse("2026-10-15"),
            fortnightPeriodContaining(LocalDate.parse("2026-10-15"), null).endInclusive,
        )
    }

    @Test
    fun februaryClampsToLastAvailableDay() {
        assertEquals(
            LocalDate.parse("2026-02-28"),
            fortnightPeriodContaining(LocalDate.parse("2026-02-20"), null).endInclusive,
        )
    }

    @Test
    fun customCycleReplacesDefaultCalendar() {
        val budget = BudgetConfig(
            amountInCents = 50_000L,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleSchedules = listOf(BudgetCycleSchedule(10, 24), BudgetCycleSchedule(25, 9)),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-24")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-12"), budget),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09")),
            fortnightPeriodContaining(LocalDate.parse("2026-11-02"), budget),
        )
    }

    @Test
    fun monthlyBudgetCreatesSingleMonthlyPeriod() {
        val budget = BudgetConfig(50_000L, BudgetPeriod.MONTHLY)
        assertEquals(FortnightPeriodStyle.MONTHLY, fortnightPeriodStyle(budget))
        assertEquals(listOf(BudgetCycleSchedule(1, 31)), fortnightSchedules(budget))
        assertEquals(
            DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-12"), budget),
        )
        assertEquals(
            FortnightSlot.FIRST,
            fortnightSlotFor(DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")), budget),
        )
    }

    @Test
    fun monthlyUserNavigatesMonthByMonth() {
        val budget = BudgetConfig(50_000L, BudgetPeriod.MONTHLY)
        val current = fortnightPeriodContaining(LocalDate.parse("2026-10-12"), budget)
        assertEquals(
            DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")),
            nextFortnightPeriod(current, budget),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30")),
            previousFortnightPeriod(current, budget),
        )
    }

    @Test
    fun customMonthlyCycleRemainsSinglePeriod() {
        val budget = BudgetConfig(
            amountInCents = 50_000L,
            period = BudgetPeriod.MONTHLY,
            cycleSchedules = listOf(BudgetCycleSchedule(26, 25)),
        )
        val period = DateRange(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-11-25"))
        assertEquals(period, fortnightPeriodContaining(LocalDate.parse("2026-11-02"), budget))
        assertEquals(
            DateRange(LocalDate.parse("2026-11-26"), LocalDate.parse("2026-12-25")),
            nextFortnightPeriod(period, budget),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-09-26"), LocalDate.parse("2026-10-25")),
            previousFortnightPeriod(period, budget),
        )
    }

    @Test
    fun secondSlotCanCrossIntoNextMonth() {
        val period = DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09"))
        assertEquals(FortnightSlot.SECOND, fortnightSlotFor(period, defaultSchedules))
    }

    @Test
    fun nextPeriodContinuesAcrossMonthBoundary() {
        val first = DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"))
        val second = nextFortnightPeriod(first, defaultSchedules)
        val third = nextFortnightPeriod(second, defaultSchedules)
        assertEquals(DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")), second)
        assertEquals(DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-15")), third)
        assertTrue(second.start > first.start)
        assertTrue(third.start > second.start)
    }

    @Test
    fun previousPeriodMovesBackCorrectly() {
        val current = DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-15"))
        assertEquals(
            DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")),
            previousFortnightPeriod(current, defaultSchedules),
        )
    }

    @Test
    fun slotReflectsConfiguredCalendar() {
        val schedules = listOf(BudgetCycleSchedule(10, 24), BudgetCycleSchedule(25, 9))
        assertEquals(
            FortnightSlot.FIRST,
            fortnightSlotFor(DateRange(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-24")), schedules),
        )
        assertEquals(
            FortnightSlot.SECOND,
            fortnightSlotFor(DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09")), schedules),
        )
    }
}
