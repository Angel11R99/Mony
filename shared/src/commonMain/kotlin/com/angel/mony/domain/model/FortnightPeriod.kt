package com.angel.mony.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class FortnightPeriodStyle {
    FORTNIGHTLY,
    MONTHLY,
}

fun fortnightPeriodStyle(budget: BudgetConfig?): FortnightPeriodStyle =
    when (budget?.period) {
        BudgetPeriod.MONTHLY -> FortnightPeriodStyle.MONTHLY
        else -> FortnightPeriodStyle.FORTNIGHTLY
    }

fun fortnightSchedules(budget: BudgetConfig?): List<BudgetCycleSchedule> {
    if (budget == null) return defaultCycleSchedules(BudgetPeriod.FORTNIGHTLY)
    val schedules = budget.cycleSchedules.distinct().ifEmpty {
        defaultCycleSchedules(budget.period)
    }
    return when (fortnightPeriodStyle(budget)) {
        FortnightPeriodStyle.MONTHLY -> listOf(schedules.first())
        FortnightPeriodStyle.FORTNIGHTLY -> schedules
    }
}

private fun fortnightPeriodsAround(
    date: LocalDate,
    schedules: List<BudgetCycleSchedule>,
): List<DateRange> {
    val referenceMonth = LocalDate(date.year, date.monthNumber, 1)
    return (-1..1)
        .flatMap { offset ->
            val openingMonth = referenceMonth.plus(offset, DateTimeUnit.MONTH)
            schedules.map { it.toDateRange(openingMonth) }
        }
        .distinct()
        .sortedBy(DateRange::start)
}

fun fortnightPeriodContaining(
    date: LocalDate,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(date, schedules)
    .firstOrNull { date in it.start..it.endInclusive }
    ?: schedules.first().toDateRange(LocalDate(date.year, date.monthNumber, 1))

fun fortnightPeriodContaining(
    date: LocalDate,
    budget: BudgetConfig?,
): DateRange = fortnightPeriodContaining(date, fortnightSchedules(budget))

fun nextFortnightPeriod(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(period.start, schedules)
    .firstOrNull { it.start > period.start }
    ?: schedules.first().toDateRange(
        LocalDate(period.start.year, period.start.monthNumber, 1).plus(1, DateTimeUnit.MONTH),
    )

fun nextFortnightPeriod(period: DateRange, budget: BudgetConfig?): DateRange =
    nextFortnightPeriod(period, fortnightSchedules(budget))

fun previousFortnightPeriod(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(period.start, schedules)
    .filter { it.endInclusive < period.start }
    .maxByOrNull(DateRange::endInclusive)
    ?: schedules.last().toDateRange(
        LocalDate(period.start.year, period.start.monthNumber, 1).minus(1, DateTimeUnit.MONTH),
    )

fun previousFortnightPeriod(period: DateRange, budget: BudgetConfig?): DateRange =
    previousFortnightPeriod(period, fortnightSchedules(budget))

fun fortnightSlotFor(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): FortnightSlot {
    val openingMonth = LocalDate(period.start.year, period.start.monthNumber, 1)
    val index = schedules.indexOfFirst { it.toDateRange(openingMonth) == period }
    return when (index) {
        1 -> FortnightSlot.SECOND
        0 -> FortnightSlot.FIRST
        else -> if (period.start.dayOfMonth <= 15) FortnightSlot.FIRST else FortnightSlot.SECOND
    }
}

fun fortnightSlotFor(period: DateRange, budget: BudgetConfig?): FortnightSlot =
    fortnightSlotFor(period, fortnightSchedules(budget))
