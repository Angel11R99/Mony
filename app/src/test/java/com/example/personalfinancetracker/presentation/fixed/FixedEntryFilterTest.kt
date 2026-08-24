package com.example.personalfinancetracker.presentation.fixed

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FixedEntryFilterTest {
    private val categories = mapOf(
        10L to Category(10, "Servicios", TransactionType.EXPENSE, "receipt", true),
    )
    private val entries = listOf(
        entry(1, TransactionType.EXPENSE, "Alquiler", 250_000, 30, comment = null),
        entry(2, TransactionType.EXPENSE, "Internet hogar", 200_000, 10, comment = "Router nuevo"),
        entry(3, TransactionType.INCOME, "Salario", 800_000, 20),
    )

    @Test fun `filters by type`() {
        val result = filterFixedEntries(entries, TransactionType.EXPENSE, "", categories)

        assertEquals(listOf(1L, 2L), result.map(FixedEntry::id))
    }

    @Test fun `search matches description ignoring case and surrounding spaces`() {
        val result = filterFixedEntries(entries, TransactionType.EXPENSE, "  internet ", categories)

        assertEquals(listOf(2L), result.map(FixedEntry::id))
    }

    @Test fun `search matches category name`() {
        val result = filterFixedEntries(entries, TransactionType.EXPENSE, "servicios", categories)

        assertEquals(listOf(2L), result.map(FixedEntry::id))
    }

    @Test fun `search matches comment`() {
        val result = filterFixedEntries(entries, TransactionType.EXPENSE, "router", categories)

        assertEquals(listOf(2L), result.map(FixedEntry::id))
    }

    @Test fun `search matches amount digits without separators`() {
        assertEquals(
            listOf(1L),
            filterFixedEntries(entries, TransactionType.EXPENSE, "2,500.00", categories).map(FixedEntry::id),
        )
        assertEquals(
            listOf(3L),
            filterFixedEntries(entries, TransactionType.INCOME, "8000", categories).map(FixedEntry::id),
        )
    }

    @Test fun `blank query returns everything for the selected type`() {
        val all = filterFixedEntries(entries, TransactionType.INCOME, "   ", categories)

        assertEquals(listOf(3L), all.map(FixedEntry::id))
    }

    private fun entry(
        id: Long,
        type: TransactionType,
        description: String,
        amountInCents: Long,
        categoryId: Long,
        comment: String? = null,
    ) = FixedEntry(
        id = id,
        type = type,
        description = description,
        amountInCents = amountInCents,
        categoryId = categoryId,
        comment = comment,
    )
}
