package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SavingsGoalTest {
    private fun goal(target: Long) = SavingsGoal(
        id = 1,
        name = "Celular nuevo",
        targetAmountInCents = target,
        createdAt = Instant.parse("2026-08-01T12:00:00Z"),
    )

    @Test fun `computes progress percent with guards`() {
        assertEquals(0, savingsProgressPercent(0, 10_000))
        assertEquals(50, savingsProgressPercent(5_000, 10_000))
        assertEquals(100, savingsProgressPercent(10_000, 10_000))
        assertEquals(150, savingsProgressPercent(15_000, 10_000))
        assertEquals(0, savingsProgressPercent(500, 0))
        assertEquals(0, savingsProgressPercent(500, -100))
    }

    @Test fun `progress reports completion only at or above target`() {
        val progress = SavingsGoalProgress(goal(10_000), savedInCents = 9_999)
        assertFalse(progress.isCompleted)
        assertEquals(99, progress.percent)

        val completed = SavingsGoalProgress(goal(10_000), savedInCents = 10_000)
        assertTrue(completed.isCompleted)
    }
}
