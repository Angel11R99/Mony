package com.example.personalfinancetracker.domain.model

import java.time.Instant
import java.time.LocalDate

data class BudgetConfig(
    val amountInCents: Long,
    val period: BudgetPeriod,
    val cycleStart: LocalDate? = null,
    val cycleStartedAt: Instant? = null,
    val incomeTransactionId: Long? = null,
)

enum class BudgetPeriod {
    MONTHLY,
    FORTNIGHTLY,
}
