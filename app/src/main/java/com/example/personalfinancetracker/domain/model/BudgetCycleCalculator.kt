package com.example.personalfinancetracker.domain.model

import java.time.LocalDate

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
