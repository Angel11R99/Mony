package com.example.personalfinancetracker.presentation.statistics

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StatisticsReportTest {
    private val food = Category(1, "Alimentación", TransactionType.EXPENSE, "food", true)
    private val transport = Category(2, "Transporte", TransactionType.EXPENSE, "car", true)

    @Test fun `calculates totals averages and sorted category distribution`() {
        val report = calculateStatistics(
            transactions = listOf(
                transaction(1, 10_000, TransactionType.INCOME, 3, LocalDate.of(2026, 8, 1)),
                transaction(2, 3_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 8, 2)),
                transaction(3, 1_000, TransactionType.EXPENSE, transport.id, LocalDate.of(2026, 8, 3)),
            ),
            categories = mapOf(food.id to food, transport.id to transport),
            startDate = LocalDate.of(2026, 8, 1),
        )

        assertEquals(10_000, report.incomeInCents)
        assertEquals(4_000, report.expenseInCents)
        assertEquals(6_000, report.balanceInCents)
        assertEquals(2_000, report.averageExpenseInCents)
        assertEquals(listOf("Alimentación", "Transporte"), report.expenseByCategory.map { it.category.name })
    }

    @Test fun `excludes transactions before selected period`() {
        val report = calculateStatistics(
            transactions = listOf(
                transaction(1, 5_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 7, 31)),
                transaction(2, 2_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 8, 1)),
            ),
            categories = mapOf(food.id to food),
            startDate = LocalDate.of(2026, 8, 1),
        )

        assertEquals(2_000, report.expenseInCents)
        assertEquals(1, report.transactionCount)
    }

    @Test fun `date range includes its first and last day`() {
        val report = calculateStatistics(
            transactions = listOf(
                transaction(1, 1_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 8, 1)),
                transaction(2, 2_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 8, 31)),
                transaction(3, 4_000, TransactionType.EXPENSE, food.id, LocalDate.of(2026, 9, 1)),
            ),
            categories = mapOf(food.id to food),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 31),
        )

        assertEquals(3_000, report.expenseInCents)
        assertEquals(2, report.transactionCount)
    }

    @Test fun `creates expected quick filter periods`() {
        val today = LocalDate.of(2026, 8, 13)

        assertEquals(
            StatisticsPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            statisticsPeriod(StatisticsRange.CURRENT_MONTH, today),
        )
        assertEquals(
            StatisticsPeriod(LocalDate.of(2026, 1, 1), today),
            statisticsPeriod(StatisticsRange.CURRENT_YEAR, today),
        )
        assertEquals(
            StatisticsPeriod(null, null),
            statisticsPeriod(StatisticsRange.ALL_TIME, today),
        )
    }

    @Test fun `current budget filter follows the active fortnight`() {
        val today = LocalDate.of(2026, 8, 20)
        val budget = BudgetConfig(
            amountInCents = 1_500_000,
            period = BudgetPeriod.FORTNIGHTLY,
        )

        assertEquals(
            StatisticsPeriod(LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31)),
            statisticsPeriod(StatisticsRange.CURRENT_BUDGET, today, budget),
        )
        assertEquals("Esta quincena", StatisticsRange.CURRENT_BUDGET.displayLabel(budget))
    }

    @Test fun `custom cycle filter uses its current month occurrence and label`() {
        val cycle = BudgetCycleSchedule(30, 14)

        assertEquals(
            StatisticsPeriod(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 14)),
            statisticsPeriod(cycle, LocalDate.of(2026, 8, 13)),
        )
        assertEquals("Ciclo 2 · días 30-14", cycle.displayLabel(1))
    }

    private fun transaction(
        id: Long,
        amount: Long,
        type: TransactionType,
        categoryId: Long,
        date: LocalDate,
    ) = FinanceTransaction(
        id = id,
        amountInCents = amount,
        type = type,
        categoryId = categoryId,
        description = null,
        date = date,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
