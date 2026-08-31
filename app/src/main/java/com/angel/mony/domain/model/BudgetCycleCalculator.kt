package com.angel.mony.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

val DEFAULT_AUTOMATIC_CLOSE_TIME: LocalTime = LocalTime.of(21, 0)

enum class BudgetPeriodView {
    CURRENT,
    NEXT,
}

fun activeBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
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
    today: LocalDate = LocalDate.now(),
): DateRange = when (view) {
    BudgetPeriodView.CURRENT -> activeBudgetPeriod(budget, today)
    BudgetPeriodView.NEXT -> nextBudgetPeriod(budget, today)
}

fun nextBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): DateRange {
    val current = activeBudgetPeriod(budget, today)
    if (budget == null) return DateRange.current(BudgetPeriod.FORTNIGHTLY, current.endInclusive.plusDays(1))
    return configuredPeriodsAround(budget, current.endInclusive.plusDays(1))
        .filter { it.start.isAfter(current.start) }
        .minByOrNull(DateRange::start)
        ?: DateRange.current(budget.period, current.endInclusive.plusDays(1))
}

fun previousBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): DateRange {
    val current = activeBudgetPeriod(budget, today)
    if (budget == null) return DateRange.current(BudgetPeriod.FORTNIGHTLY, current.start.minusDays(1))
    return configuredPeriodsAround(budget, current.start.minusDays(1))
        .filter { it.endInclusive.isBefore(current.start) }
        .maxByOrNull(DateRange::endInclusive)
        ?: DateRange.current(budget.period, current.start.minusDays(1))
}

fun budgetPeriodForSchedule(
    schedule: BudgetCycleSchedule,
    today: LocalDate = LocalDate.now(),
): DateRange = schedule.toDateRange(YearMonth.from(today))

private fun configuredPeriodsAround(budget: BudgetConfig, date: LocalDate): List<DateRange> {
    val schedules = budget.cycleSchedules.ifEmpty { defaultCycleSchedules(budget.period) }
    val referenceMonth = YearMonth.from(date)
    return (-2..2).flatMap { offset ->
        val openingMonth = referenceMonth.plusMonths(offset.toLong())
        schedules.map { it.toDateRange(openingMonth) }
    }.distinct().sortedBy(DateRange::start)
}

private fun BudgetCycleSchedule.toDateRange(openingMonth: YearMonth): DateRange {
    val start = openingMonth.atClampedDay(openingDay)
    val closingMonth = if (closingDay < openingDay) openingMonth.plusMonths(1) else openingMonth
    return DateRange(start, closingMonth.atClampedDay(closingDay))
}

private fun YearMonth.atClampedDay(day: Int): LocalDate = atDay(day.coerceAtMost(lengthOfMonth()))

fun FinanceTransaction.belongsToActiveBudgetCycle(
    budget: BudgetConfig?,
    period: DateRange,
): Boolean {
    if (date !in period.start..period.endInclusive) return false
    val boundary = budget?.cycleStartedAt?.takeIf { budget.cycleStart == period.start } ?: return true
    // La fecha es la fuente de verdad para agrupar por ciclo.
    // El límite `cycleStartedAt` solo debe excluir movimientos creados antes del inicio
    // cuando su fecha coincide exactamente con el día de apertura (evita arrastrar
    // movimientos del ciclo anterior con la misma fecha). Los movimientos con fecha
    // posterior a la apertura pertenecen al ciclo por su fecha, aunque se hayan
    // creado antes del cierre del ciclo anterior (p.ej. un gasto con fecha futura).
    if (date.isAfter(period.start)) return true
    return !createdAt.isBefore(boundary)
}

fun availableForBudget(
    budget: BudgetConfig?,
    transactions: List<FinanceTransaction>,
    today: LocalDate = LocalDate.now(),
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
    today: LocalDate = LocalDate.now(),
): Boolean {
    if (budget == null || budget.cycleStart?.let { !it.isBefore(today) } == true) return false
    return activeBudgetPeriod(budget, today).endInclusive == today
}

fun shouldAutomaticallyCloseBudgetCycle(
    budget: BudgetConfig?,
    now: LocalDateTime = LocalDateTime.now(),
    closeTime: LocalTime = DEFAULT_AUTOMATIC_CLOSE_TIME,
): Boolean {
    if (budget == null || budget.cycleStart?.let { !it.isBefore(now.toLocalDate()) } == true) return false
    val today = now.toLocalDate()
    return activeBudgetPeriod(budget, today).endInclusive == today &&
        !now.toLocalTime().isBefore(closeTime)
}

fun budgetPeriodToClose(
    budget: BudgetConfig,
    today: LocalDate = LocalDate.now(),
): DateRange = activeBudgetPeriod(budget, today)
