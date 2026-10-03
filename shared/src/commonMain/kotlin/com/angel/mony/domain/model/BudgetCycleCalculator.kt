package com.angel.mony.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

val DEFAULT_AUTOMATIC_CLOSE_TIME: LocalTime = LocalTime(21, 0)

enum class BudgetPeriodView {
    CURRENT,
    NEXT,
}

fun activeBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = currentLocalDate(),
): DateRange {
    if (budget == null) return DateRange.current(BudgetPeriod.FORTNIGHTLY, today)
    return configuredPeriodsAround(budget, today)
        .filter { today in it.start..it.endInclusive }
        .maxByOrNull(DateRange::start)
        ?: DateRange.current(budget.period, today)
}

fun budgetPeriodForView(
    budget: BudgetConfig?,
    view: BudgetPeriodView,
    today: LocalDate = currentLocalDate(),
): DateRange = when (view) {
    BudgetPeriodView.CURRENT -> activeBudgetPeriod(budget, today)
    BudgetPeriodView.NEXT -> nextBudgetPeriod(budget, today)
}

fun nextBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = currentLocalDate(),
): DateRange {
    val current = activeBudgetPeriod(budget, today)
    val nextDay = current.endInclusive.plus(1, DateTimeUnit.DAY)
    if (budget == null) return DateRange.current(BudgetPeriod.FORTNIGHTLY, nextDay)
    return configuredPeriodsAround(budget, nextDay)
        .filter { it.start > current.start }
        .minByOrNull(DateRange::start)
        ?: DateRange.current(budget.period, nextDay)
}

fun previousBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = currentLocalDate(),
): DateRange {
    val current = activeBudgetPeriod(budget, today)
    val previousDay = current.start.minus(1, DateTimeUnit.DAY)
    if (budget == null) return DateRange.current(BudgetPeriod.FORTNIGHTLY, previousDay)
    return configuredPeriodsAround(budget, previousDay)
        .filter { it.endInclusive < current.start }
        .maxByOrNull(DateRange::endInclusive)
        ?: DateRange.current(budget.period, previousDay)
}

fun budgetPeriodForSchedule(
    schedule: BudgetCycleSchedule,
    today: LocalDate = currentLocalDate(),
): DateRange = schedule.toDateRange(LocalDate(today.year, today.monthNumber, 1))

private fun configuredPeriodsAround(budget: BudgetConfig, date: LocalDate): List<DateRange> {
    val schedules = budget.cycleSchedules.ifEmpty { defaultCycleSchedules(budget.period) }
    val referenceMonth = LocalDate(date.year, date.monthNumber, 1)
    return (-2..2).flatMap { offset ->
        val openingMonth = referenceMonth.plus(offset, DateTimeUnit.MONTH)
        schedules.map { it.toDateRange(openingMonth) }
    }.distinct().sortedBy(DateRange::start)
}

fun BudgetCycleSchedule.toDateRange(openingMonth: LocalDate): DateRange {
    val firstDay = LocalDate(openingMonth.year, openingMonth.monthNumber, 1)
    val start = firstDay.atClampedDay(openingDay)
    val closingMonth = if (closingDay < openingDay) {
        firstDay.plus(1, DateTimeUnit.MONTH)
    } else {
        firstDay
    }
    return DateRange(start, closingMonth.atClampedDay(closingDay))
}

private fun LocalDate.atClampedDay(day: Int): LocalDate =
    LocalDate(year, monthNumber, day.coerceAtMost(endOfMonth().dayOfMonth))

fun FinanceTransaction.belongsToActiveBudgetCycle(
    @Suppress("UNUSED_PARAMETER") budget: BudgetConfig?,
    period: DateRange,
): Boolean = date in period.start..period.endInclusive

fun availableForBudget(
    budget: BudgetConfig?,
    transactions: List<FinanceTransaction>,
    today: LocalDate = currentLocalDate(),
): Long {
    if (budget == null) {
        return transactions.sumOf {
            if (it.type == TransactionType.INCOME) it.amountInCents else -it.amountInCents
        }
    }
    return availableForBudget(budget, transactions, activeBudgetPeriod(budget, today))
}

fun availableForBudget(
    budget: BudgetConfig?,
    transactions: List<FinanceTransaction>,
    period: DateRange,
): Long {
    if (budget == null) {
        return transactions.filter { it.date in period.start..period.endInclusive }.sumOf {
            if (it.type == TransactionType.INCOME) it.amountInCents else -it.amountInCents
        }
    }
    return budget.amountInCents - budgetCycleExpenses(budget, transactions, period)
}

fun budgetCycleExpenses(
    budget: BudgetConfig,
    transactions: List<FinanceTransaction>,
    period: DateRange,
): Long = transactions
    .filter { it.type == TransactionType.EXPENSE && it.belongsToActiveBudgetCycle(budget, period) }
    .sumOf(FinanceTransaction::amountInCents)

fun budgetUsagePercent(
    budget: BudgetConfig,
    transactions: List<FinanceTransaction>,
    period: DateRange,
): Int {
    if (budget.amountInCents <= 0) return 0
    val expenses = budgetCycleExpenses(budget, transactions, period)
    return ((expenses * 100) / budget.amountInCents).toInt()
}

fun canManuallyCloseBudgetCycle(
    budget: BudgetConfig?,
    today: LocalDate = currentLocalDate(),
): Boolean {
    if (budget == null || budget.cycleStart?.let { it >= today } == true) return false
    return budgetPeriodToClose(budget, today).endInclusive <= today
}

fun shouldAutomaticallyCloseBudgetCycle(
    budget: BudgetConfig?,
    now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    closeTime: LocalTime = DEFAULT_AUTOMATIC_CLOSE_TIME,
): Boolean {
    if (budget == null || budget.cycleStart?.let { it >= now.date } == true) return false
    val periodToClose = budgetPeriodToClose(budget, now.date)
    return periodToClose.endInclusive < now.date ||
        (periodToClose.endInclusive == now.date && now.time >= closeTime)
}

fun budgetPeriodToClose(
    budget: BudgetConfig,
    today: LocalDate = currentLocalDate(),
): DateRange = activeBudgetPeriod(budget, budget.cycleStart ?: today)
