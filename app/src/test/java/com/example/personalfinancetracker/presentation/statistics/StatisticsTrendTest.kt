package com.example.personalfinancetracker.presentation.statistics

import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.previousBudgetPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StatisticsTrendTest {
    private val today = LocalDate.of(2026, 8, 24)

    @Test fun `trend delta detects increase decrease flat and new`() {
        assertEquals(TrendDirection.UP, trendDelta(current = 150, previous = 100).direction)
        assertEquals(50, trendDelta(current = 150, previous = 100).percent)
        assertEquals(TrendDirection.DOWN, trendDelta(current = 75, previous = 100).direction)
        assertEquals(-25, trendDelta(current = 75, previous = 100).percent)
        assertEquals(TrendDirection.FLAT, trendDelta(current = 100, previous = 100).direction)
        assertEquals(0, trendDelta(current = 100, previous = 100).percent)
        assertEquals(TrendDirection.NEW, trendDelta(current = 500, previous = 0).direction)
        assertNull(trendDelta(current = 500, previous = 0).percent)
    }

    @Test fun `trend delta handles both empty and truncates percent`() {
        assertEquals(TrendDirection.FLAT, trendDelta(current = 0, previous = 0).direction)
        assertNull(trendDelta(current = 0, previous = 0).percent)
        assertEquals(1, trendDelta(current = 101, previous = 100).percent)
        assertEquals(-1, trendDelta(current = 99, previous = 100).percent)
    }

    @Test fun `previous period for month and year`() {
        val month = statisticsPeriod(StatisticsRange.CURRENT_MONTH, today)
        val previousMonth = previousStatisticsPeriod(
            StatisticsRange.CURRENT_MONTH, null, null, month, today,
        )!!
        assertEquals(LocalDate.of(2026, 7, 1), previousMonth.startDate)
        assertEquals(LocalDate.of(2026, 7, 31), previousMonth.endDate)

        val year = statisticsPeriod(StatisticsRange.CURRENT_YEAR, today)
        val previousYear = previousStatisticsPeriod(
            StatisticsRange.CURRENT_YEAR, null, null, year, today,
        )!!
        assertEquals(LocalDate.of(2025, 1, 1), previousYear.startDate)
        assertEquals(LocalDate.of(2025, 12, 31), previousYear.endDate)
    }

    @Test fun `previous period for budget cycle uses configured windows`() {
        val budget = BudgetConfig(
            amountInCents = 10_000_00,
            period = BudgetPeriod.FORTNIGHTLY,
        )
        val current = statisticsPeriod(StatisticsRange.CURRENT_BUDGET, today, budget)
        assertEquals(LocalDate.of(2026, 8, 16), current.startDate)
        val previous = previousStatisticsPeriod(
            StatisticsRange.CURRENT_BUDGET, null, budget, current, today,
        )!!
        assertEquals(LocalDate.of(2026, 8, 1), previous.startDate)
        assertEquals(LocalDate.of(2026, 8, 15), previous.endDate)
    }

    @Test fun `previous period for a selected cycle shifts one month back`() {
        val schedule = BudgetCycleSchedule(16, 31)
        val current = statisticsPeriod(schedule, today)
        assertEquals(LocalDate.of(2026, 8, 16), current.startDate)
        val previous = previousStatisticsPeriod(
            StatisticsRange.CURRENT_BUDGET, schedule, null, current, today,
        )!!
        assertEquals(LocalDate.of(2026, 7, 16), previous.startDate)
        assertEquals(LocalDate.of(2026, 7, 31), previous.endDate)
    }

    @Test fun `all time and custom ranges have no comparable previous period`() {
        val allTime = statisticsPeriod(StatisticsRange.ALL_TIME)
        assertNull(previousStatisticsPeriod(StatisticsRange.ALL_TIME, null, null, allTime, today))
        val custom = StatisticsPeriod(today.minusDays(6), today)
        assertNull(previousStatisticsPeriod(StatisticsRange.CUSTOM, null, null, custom, today))
    }

    @Test fun `previousBudgetPeriod falls back without config`() {
        val previous = previousBudgetPeriod(null, today)
        assertEquals(LocalDate.of(2026, 8, 1), previous.start)
        assertEquals(LocalDate.of(2026, 8, 15), previous.endInclusive)
    }

    @Test fun `custom range label formats dates`() {
        assertEquals("Personalizado", customRangeLabel(null, null))
        assertEquals("01/08/2026 – 24/08/2026", customRangeLabel(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 24)))
        assertEquals("Desde 05/08/2026", customRangeLabel(LocalDate.of(2026, 8, 5), null))
        assertEquals("Hasta 20/08/2026", customRangeLabel(null, LocalDate.of(2026, 8, 20)))
    }
}
