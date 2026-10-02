package com.angel.mony.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class BudgetConfig(
    val amountInCents: Long,
    val period: BudgetPeriod,
    val cycleStart: LocalDate? = null,
    val cycleStartedAt: Instant? = null,
    val incomeTransactionId: Long? = null,
    val cycleSchedules: List<BudgetCycleSchedule> = defaultCycleSchedules(period),
)

