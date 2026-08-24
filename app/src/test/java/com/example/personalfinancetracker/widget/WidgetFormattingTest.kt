package com.example.personalfinancetracker.widget

import com.example.personalfinancetracker.domain.model.SavingsGoal
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WidgetFormattingTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 24)

    // ------------------------------------------------------------------ dates

    @Test
    fun `formatShortDate renders day and abbreviated month`() {
        assertEquals("24 ago", formatShortDate(LocalDate.of(2026, 8, 24)))
        assertEquals("1 ene", formatShortDate(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `cycleDaysLeft counts days until cycle end inclusive`() {
        val period = com.example.personalfinancetracker.domain.model.DateRange(
            start = today,
            endInclusive = today.plusDays(5),
        )
        assertEquals(5, cycleDaysLeft(period, today))
    }

    @Test
    fun `cycleDaysLeft is zero when the cycle already ended`() {
        val period = com.example.personalfinancetracker.domain.model.DateRange(
            start = today.minusDays(10),
            endInclusive = today.minusDays(2),
        )
        assertEquals(0, cycleDaysLeft(period, today))
    }

    @Test
    fun `cycleDaysLeft is zero on closing day`() {
        val period = com.example.personalfinancetracker.domain.model.DateRange(
            start = today.minusDays(14),
            endInclusive = today,
        )
        assertEquals(0, cycleDaysLeft(period, today))
    }

    // ------------------------------------------------------- pending reminders

    @Test
    fun `pendingDayLabel detects today tomorrow future and overdue`() {
        assertEquals(PendingDayKind.TODAY, pendingDayLabel(today, today).kind)
        assertEquals(PendingDayKind.TOMORROW, pendingDayLabel(today.plusDays(1), today).kind)
        assertEquals(PendingDayKind.ON_DATE, pendingDayLabel(today.plusDays(9), today).kind)
        assertEquals(PendingDayKind.OVERDUE, pendingDayLabel(today.minusDays(3), today).kind)
    }

    @Test
    fun `pendingDayLabel keeps formatted date for non relative days`() {
        val label = pendingDayLabel(LocalDate.of(2026, 8, 30), today)
        assertFalse(label.kind == PendingDayKind.TODAY)
        assertEquals(formatShortDate(LocalDate.of(2026, 8, 30)), label.dateText)
    }

    // ------------------------------------------------------------ trend delta

    @Test
    fun `expenseTrend returns null without previous data`() {
        assertNull(expenseTrend(currentExpenseInCents = 5_000L, previousExpenseInCents = null))
        assertNull(expenseTrend(currentExpenseInCents = 5_000L, previousExpenseInCents = 0L))
    }

    @Test
    fun `expenseTrend reports up with positive percent`() {
        val trend = expenseTrend(currentExpenseInCents = 11_000L, previousExpenseInCents = 10_000L)
        assertTrue(trend!!.direction == ExpenseTrend.Direction.UP)
        assertEquals(10, trend.percent)
    }

    @Test
    fun `expenseTrend reports down using absolute percent`() {
        val trend = expenseTrend(currentExpenseInCents = 8_000L, previousExpenseInCents = 10_000L)
        assertTrue(trend!!.direction == ExpenseTrend.Direction.DOWN)
        assertEquals(20, trend.percent)
    }

    @Test
    fun `expenseTrend reports flat when equal`() {
        val trend = expenseTrend(currentExpenseInCents = 10_000L, previousExpenseInCents = 10_000L)
        assertTrue(trend!!.direction == ExpenseTrend.Direction.FLAT)
        assertEquals(0, trend.percent)
    }

    @Test
    fun `signedAmountLabel prefixes income and expense differently`() {
        assertTrue(signedAmountLabel(isIncome = true, amountInCents = 12_345L).startsWith("+"))
        assertTrue(signedAmountLabel(isIncome = false, amountInCents = 12_345L).startsWith("−"))
    }

    // ---------------------------------------------------------- savings goals

    private fun goal(id: Long, target: Long, saved: Long) = SavingsGoalProgress(
        goal = SavingsGoal(
            id = id,
            name = "Meta $id",
            targetAmountInCents = target,
            createdAt = Instant.EPOCH,
        ),
        savedInCents = saved,
    )

    @Test
    fun `sortSavingsGoals puts in progress goals first ordered by percent`() {
        val sorted = sortSavingsGoals(
            listOf(
                goal(id = 1, target = 100_000L, saved = 100_000L), // completed 100%
                goal(id = 2, target = 100_000L, saved = 25_000L), // 25%
                goal(id = 3, target = 100_000L, saved = 50_000L), // 50%
            ),
        )
        assertEquals(listOf(3L, 2L, 1L), sorted.map { it.goal.id })
    }

    @Test
    fun `sortSavingsGoals breaks percent ties by saved amount`() {
        val sorted = sortSavingsGoals(
            listOf(
                goal(id = 1, target = 400_000L, saved = 100_000L), // 25%
                goal(id = 2, target = 200_000L, saved = 50_000L), // 25%
                goal(id = 3, target = 800_000L, saved = 200_000L), // 25%
            ),
        )
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.goal.id })
    }
}
