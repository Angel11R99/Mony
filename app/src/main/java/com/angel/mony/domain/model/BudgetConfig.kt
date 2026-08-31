package com.angel.mony.domain.model

import java.time.Instant
import java.time.LocalDate

data class BudgetConfig(
    val amountInCents: Long,
    val period: BudgetPeriod,
    val cycleStart: LocalDate? = null,
    val cycleStartedAt: Instant? = null,
    val incomeTransactionId: Long? = null,
    val cycleSchedules: List<BudgetCycleSchedule> = defaultCycleSchedules(period),
)

data class BudgetCycleSchedule(
    val openingDay: Int,
    val closingDay: Int,
) {
    init {
        require(openingDay in 1..31)
        require(closingDay in 1..31)
    }
}

fun defaultCycleSchedules(period: BudgetPeriod): List<BudgetCycleSchedule> = when (period) {
    BudgetPeriod.FORTNIGHTLY -> listOf(
        BudgetCycleSchedule(openingDay = 1, closingDay = 15),
        BudgetCycleSchedule(openingDay = 16, closingDay = 31),
    )
    BudgetPeriod.MONTHLY -> listOf(BudgetCycleSchedule(openingDay = 1, closingDay = 31))
}

enum class BudgetPeriod {
    MONTHLY,
    FORTNIGHTLY,
}
