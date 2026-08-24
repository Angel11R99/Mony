package com.example.personalfinancetracker.domain.model

import java.time.Instant

data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmountInCents: Long,
    val createdAt: Instant,
)

data class SavingsGoalProgress(
    val goal: SavingsGoal,
    val savedInCents: Long,
) {
    val percent: Int get() = savingsProgressPercent(savedInCents, goal.targetAmountInCents)
    val isCompleted: Boolean get() =
        goal.targetAmountInCents > 0 && savedInCents >= goal.targetAmountInCents
}

fun savingsProgressPercent(savedInCents: Long, targetInCents: Long): Int {
    if (targetInCents <= 0) return 0
    return ((savedInCents * 100) / targetInCents).toInt()
}
