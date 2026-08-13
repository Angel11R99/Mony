package com.example.personalfinancetracker.presentation.transactions

import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HistoryFilterTest {
    private val transactions = listOf(
        transaction(1, TransactionType.EXPENSE, 10, LocalDate.of(2026, 8, 10), 1_000),
        transaction(2, TransactionType.EXPENSE, 20, LocalDate.of(2026, 8, 11), 2_000),
        transaction(3, TransactionType.INCOME, 10, LocalDate.of(2026, 8, 12), 3_000),
    )

    @Test fun `filters by type and category`() {
        val result = filterTransactions(
            transactions,
            type = TransactionType.EXPENSE,
            categoryId = 10,
            startDate = null,
            endDate = null,
        )

        assertEquals(listOf(1L), result.map(FinanceTransaction::id))
    }

    @Test fun `date range includes first and last day`() {
        val result = filterTransactions(
            transactions,
            type = null,
            categoryId = null,
            startDate = LocalDate.of(2026, 8, 10),
            endDate = LocalDate.of(2026, 8, 11),
        )

        assertEquals(listOf(1L, 2L), result.map(FinanceTransaction::id))
    }

    @Test fun `remembers latest category separately for expense and income`() {
        assertEquals(10L, lastCategoryForType(transactions, TransactionType.EXPENSE))
        assertEquals(10L, lastCategoryForType(transactions, TransactionType.INCOME))
    }

    @Test fun `category search finds transport case insensitively`() {
        val categories = listOf(
            Category(1, "Transporte", TransactionType.EXPENSE, "car", true),
            Category(2, "Ahorro", TransactionType.EXPENSE, "savings", true),
        )

        assertEquals(listOf("Transporte"), searchCategories(categories, "transPOR" ).map(Category::name))
    }

    private fun transaction(
        id: Long,
        type: TransactionType,
        categoryId: Long,
        date: LocalDate,
        amount: Long,
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
