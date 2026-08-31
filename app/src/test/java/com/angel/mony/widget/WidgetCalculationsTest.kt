package com.angel.mony.widget

import com.angel.mony.domain.model.SavingsGoal
import com.angel.mony.domain.model.SavingsGoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WidgetCalculationsTest {

    // ------------------------------------------------------------- daily pace

    @Test
    fun `dailyAllowanceInCents is null without budget`() {
        assertNull(dailyAllowanceInCents(budgetInCents = null, spentInCycleInCents = 100L, daysLeft = 5))
        assertNull(dailyAllowanceInCents(budgetInCents = 0L, spentInCycleInCents = 100L, daysLeft = 5))
    }

    @Test
    fun `dailyAllowanceInCents spreads remaining budget over days left`() {
        val allowance = dailyAllowanceInCents(
            budgetInCents = 30_000L,
            spentInCycleInCents = 10_000L,
            daysLeft = 4,
        )
        assertEquals(5_000L, allowance)
    }

    @Test
    fun `dailyAllowanceInCents treats zero days left as one day`() {
        val allowance = dailyAllowanceInCents(
            budgetInCents = 10_000L,
            spentInCycleInCents = 2_500L,
            daysLeft = 0,
        )
        assertEquals(7_500L, allowance)
    }

    @Test
    fun `dailyAllowanceInCents can be negative after overspending`() {
        val allowance = dailyAllowanceInCents(
            budgetInCents = 10_000L,
            spentInCycleInCents = 12_000L,
            daysLeft = 2,
        )
        assertEquals(-1_000L, allowance)
    }

    @Test
    fun `dailyPaceFraction is zero without allowance or non positive allowance`() {
        assertEquals(0f, dailyPaceFraction(todayExpenseInCents = 500L, allowanceInCents = null))
        assertEquals(0f, dailyPaceFraction(todayExpenseInCents = 500L, allowanceInCents = 0L))
    }

    @Test
    fun `dailyPaceFraction compares today spend against allowance`() {
        val fraction = dailyPaceFraction(todayExpenseInCents = 250L, allowanceInCents = 1_000L)
        assertEquals(0.25f, fraction, 0.0001f)
    }

    // --------------------------------------------------------- category limits

    private fun slice(name: String, amount: Long, limit: Long?) =
        CategorySlice(name = name, amountInCents = amount, fraction = 0f, limitInCents = limit)

    @Test
    fun `buildCategoryLimitUsages puts over-limit categories first`() {
        val usages = buildCategoryLimitUsages(
            listOf(
                slice("A", amount = 900L, limit = 1_000L),   // 90%
                slice("B", amount = 1_200L, limit = 1_000L), // over
                slice("C", amount = 50L, limit = 1_000L),    // 5%
            ),
        )
        assertEquals(listOf("B", "A", "C"), usages.map { it.slice.name })
    }

    @Test
    fun `buildCategoryLimitUsages keeps unlimited slices at the end`() {
        val usages = buildCategoryLimitUsages(
            listOf(
                slice("Sin límite", amount = 999L, limit = null),
                slice("Con límite", amount = 10L, limit = 5_000L),
            ),
        )
        assertEquals(listOf("Con límite", "Sin límite"), usages.map { it.slice.name })
        assertEquals(0f, usages.last().fraction)
    }

    @Test
    fun `CategoryLimitUsage computes fraction and over limit state`() {
        val usage = CategoryLimitUsage(slice("X", amount = 1_500L, limit = 1_000L), limitInCents = 1_000L)
        assertEquals(1.5f, usage.fraction, 0.0001f)
        assertTrue(usage.isOverLimit)

        val under = CategoryLimitUsage(slice("Y", amount = 400L, limit = 1_000L), limitInCents = 1_000L)
        assertFalse(under.isOverLimit)
    }

    // ---------------------------------------------------------- savings totals

    @Test
    fun `sumSavedTotals adds saved and target amounts across goals`() {
        fun goal(target: Long, saved: Long) = SavingsGoalProgress(
            goal = SavingsGoal(
                id = 0,
                name = "g",
                targetAmountInCents = target,
                createdAt = Instant.EPOCH,
            ),
            savedInCents = saved,
        )
        val (saved, target) = sumSavedTotals(listOf(goal(10_000L, 2_500L), goal(20_000L, 7_500L)))
        assertEquals(10_000L, saved)
        assertEquals(30_000L, target)
    }

    @Test
    fun `sumSavedTotals handles empty list`() {
        val (saved, target) = sumSavedTotals(emptyList())
        assertEquals(0L, saved)
        assertEquals(0L, target)
    }
}
