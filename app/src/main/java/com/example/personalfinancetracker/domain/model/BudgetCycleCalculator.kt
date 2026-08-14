package com.example.personalfinancetracker.domain.model

import java.time.LocalDate
import java.time.YearMonth

fun activeBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): DateRange {
    val calendarPeriod = DateRange.current(budget?.period ?: BudgetPeriod.FORTNIGHTLY, today)
    val activeCycleStart = budget?.cycleStart?.takeIf {
        it in calendarPeriod.start..calendarPeriod.endInclusive
    }
    return DateRange(activeCycleStart ?: calendarPeriod.start, calendarPeriod.endInclusive)
}

fun FinanceTransaction.belongsToActiveBudgetCycle(
    budget: BudgetConfig?,
    period: DateRange,
): Boolean {
    if (date !in period.start..period.endInclusive) return false
    val boundary = budget?.cycleStartedAt?.takeIf { budget.cycleStart == period.start }
    return boundary == null || !createdAt.isBefore(boundary)
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
    val period = activeBudgetPeriod(budget, today)
    val expenses = transactions
        .filter { it.type == TransactionType.EXPENSE && it.belongsToActiveBudgetCycle(budget, period) }
        .sumOf(FinanceTransaction::amountInCents)
    return budget.amountInCents - expenses
}

fun canManuallyCloseBudgetCycle(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): Boolean {
    if (budget == null || budget.cycleStart == today) return false
    return today.dayOfMonth in budget.closingDays
}

fun shouldAutomaticallyCloseBudgetCycle(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): Boolean {
    if (budget == null || budget.cycleStart == today) return false
    return today.dayOfMonth in budget.closingDays
}

fun budgetCyclePeriodToClose(
    period: BudgetPeriod,
    closingDays: List<Int>,
    today: LocalDate = LocalDate.now(),
): DateRange {
    val days = closingDays.filter { it in 1..31 }.distinct().sorted()
    if (days.isEmpty()) {
        return when (period) {
            BudgetPeriod.FORTNIGHTLY -> if (today.dayOfMonth == 16) {
                DateRange(today.withDayOfMonth(1), today.withDayOfMonth(15))
            } else {
                DateRange(today.withDayOfMonth(16), today)
            }
            BudgetPeriod.MONTHLY -> if (today.dayOfMonth == 1) {
                YearMonth.from(today).minusMonths(1).let {
                    DateRange(it.atDay(1), it.atEndOfMonth())
                }
            } else {
                DateRange(today.withDayOfMonth(1), today)
            }
        }
    }
    val todayDay = today.dayOfMonth
    val prevThisMonth = days.lastOrNull { it < todayDay }
    val startDate = if (prevThisMonth != null) {
        today.withDayOfMonth(prevThisMonth)
    } else {
        YearMonth.from(today).minusMonths(1).atDay(days.last())
    }
    return DateRange(startDate, today.minusDays(1))
}
