package com.example.personalfinancetracker.domain.model

data class BudgetConfig(
    val amountInCents: Long,
    val period: BudgetPeriod,
)

enum class BudgetPeriod {
    MONTHLY,
    FORTNIGHTLY,
}
