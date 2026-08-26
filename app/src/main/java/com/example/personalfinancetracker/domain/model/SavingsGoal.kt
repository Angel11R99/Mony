package com.example.personalfinancetracker.domain.model

import java.time.Instant

data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmountInCents: Long,
    val createdAt: Instant,
    val completedAt: Instant? = null,
)

data class SavingsGoalProgress(
    val goal: SavingsGoal,
    val savedInCents: Long,
) {
    val percent: Int get() = savingsProgressPercent(savedInCents, goal.targetAmountInCents)
    val isActive: Boolean get() = goal.completedAt == null
    val isCompleted: Boolean get() = goal.completedAt != null ||
        (goal.targetAmountInCents > 0 && savedInCents >= goal.targetAmountInCents)
    val canComplete: Boolean get() =
        goal.completedAt == null && goal.targetAmountInCents > 0 && savedInCents >= goal.targetAmountInCents
    val excessInCents: Long get() =
        if (goal.targetAmountInCents > 0 && savedInCents > goal.targetAmountInCents)
            savedInCents - goal.targetAmountInCents else 0
    val savedForGoalInCents: Long get() =
        if (goal.targetAmountInCents > 0) savedInCents.coerceAtMost(goal.targetAmountInCents) else savedInCents
}

fun savingsProgressPercent(savedInCents: Long, targetInCents: Long): Int {
    if (targetInCents <= 0) return 0
    return ((savedInCents * 100) / targetInCents).toInt()
}
