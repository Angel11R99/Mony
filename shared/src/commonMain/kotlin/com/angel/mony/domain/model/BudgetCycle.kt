package com.angel.mony.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class BudgetCycle(
    val id: Long = 0,
    val period: BudgetPeriod,
    val budgetAmountInCents: Long,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val closedAt: Instant,
) {
    val remainingInCents: Long get() = budgetAmountInCents - expenseInCents
}
