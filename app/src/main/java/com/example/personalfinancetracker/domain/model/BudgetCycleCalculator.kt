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
    return when (budget.period) {
        BudgetPeriod.FORTNIGHTLY -> today.dayOfMonth in setOf(1, 16, 31)
        BudgetPeriod.MONTHLY -> today.dayOfMonth == 1 || today.dayOfMonth == today.lengthOfMonth()
    }
}

fun shouldAutomaticallyCloseBudgetCycle(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): Boolean {
    if (budget == null || budget.cycleStart == today) return false
    return when (budget.period) {
        BudgetPeriod.FORTNIGHTLY -> today.dayOfMonth == 1 || today.dayOfMonth == 16
        BudgetPeriod.MONTHLY -> today.dayOfMonth == 1
    }
}

fun budgetCyclePeriodToClose(
    period: BudgetPeriod,
    today: LocalDate = LocalDate.now(),
): DateRange = when (period) {
    BudgetPeriod.FORTNIGHTLY -> when (today.dayOfMonth) {
        1 -> YearMonth.from(today).minusMonths(1).let {
            DateRange(it.atDay(16), it.atEndOfMonth())
        }
        16 -> DateRange(today.withDayOfMonth(1), today.withDayOfMonth(15))
        else -> DateRange(today.withDayOfMonth(16), today)
    }
    BudgetPeriod.MONTHLY -> if (today.dayOfMonth == 1) {
        YearMonth.from(today).minusMonths(1).let {
            DateRange(it.atDay(1), it.atEndOfMonth())
        }
    } else {
        DateRange(today.withDayOfMonth(1), today)
    }
}
