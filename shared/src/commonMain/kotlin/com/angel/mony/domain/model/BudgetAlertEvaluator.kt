package com.angel.mony.domain.model

enum class BudgetAlertLevel(val percent: Int) {
    NONE(0),
    WARNING(75),
    EXCEEDED(100);

    companion object {
        fun forUsagePercent(percent: Int): BudgetAlertLevel = when {
            percent >= EXCEEDED.percent -> EXCEEDED
            percent >= WARNING.percent -> WARNING
            else -> NONE
        }
    }
}

data class BudgetAlertDecision(
    val levelToStore: BudgetAlertLevel,
    val shouldNotify: Boolean,
)

object BudgetAlertEvaluator {
    fun evaluate(previous: BudgetAlertLevel, current: BudgetAlertLevel): BudgetAlertDecision =
        BudgetAlertDecision(
            levelToStore = current,
            shouldNotify = current.ordinal > previous.ordinal,
        )
}
