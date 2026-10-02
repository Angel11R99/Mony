package com.angel.mony.domain.model

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
