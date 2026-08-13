package com.example.personalfinancetracker.presentation.statistics

import com.example.personalfinancetracker.domain.model.Category
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
