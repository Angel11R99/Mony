package com.example.personalfinancetracker.presentation.pending

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PendingEntryFilterTest {
    private val categories = mapOf(
        10L to Category(10, "Servicios", TransactionType.EXPENSE, "receipt", true),
        20L to Category(20, "Ventas", TransactionType.INCOME, "payments", true),
    )
    private val entries = listOf(
        entry(1, PendingType.PAYMENT, "Pago de luz", 150_000, 10, LocalDate.of(2026, 8, 5), isDone = true),
        entry(2, PendingType.PAYMENT, "Internet", 200_000, 10, LocalDate.of(2026, 8, 12), comment = "Router nuevo"),
        entry(3, PendingType.PAYMENT, "Seguro vehículo", 350_000, 30, LocalDate.of(2026, 8, 20)),
        entry(4, PendingType.COLLECTION, "Venta celular", 500_000, 20, LocalDate.of(2026, 9, 2)),
    )

    @Test fun `filters by type and keeps pending first sorted by date`() {
        val result = filterPendingEntries(entries, PendingType.PAYMENT, null, "", categories)

        assertEquals(listOf(2L, 3L, 1L), result.map(PendingEntry::id))
    }

    @Test fun `period range includes first and last day`() {
        val range = DateRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 31))

        val result = filterPendingEntries(entries, PendingType.PAYMENT, range, "", categories)

        assertEquals(listOf(2L, 3L), result.map(PendingEntry::id))
    }

    @Test fun `search matches description ignoring case and surrounding spaces`() {
        val result = filterPendingEntries(entries, PendingType.PAYMENT, null, "  seguro ", categories)

        assertEquals(listOf(3L), result.map(PendingEntry::id))
    }

    @Test fun `search matches category name`() {
        val result = filterPendingEntries(entries, PendingType.PAYMENT, null, "servicios", categories)

        assertEquals(listOf(2L, 1L), result.map(PendingEntry::id))
    }

    @Test fun `search matches comment`() {
        val result = filterPendingEntries(entries, PendingType.PAYMENT, null, "router", categories)

        assertEquals(listOf(2L), result.map(PendingEntry::id))
    }

    @Test fun `search matches amount digits without separators`() {
        assertEquals(
            listOf(3L),
            filterPendingEntries(entries, PendingType.PAYMENT, null, "3,500.00", categories).map(PendingEntry::id),
        )
        assertEquals(
            listOf(4L),
            filterPendingEntries(entries, PendingType.COLLECTION, null, "5000", categories).map(PendingEntry::id),
        )
    }

    @Test fun `blank query returns everything for the selected type`() {
        val all = filterPendingEntries(entries, PendingType.COLLECTION, null, "   ", categories)

        assertEquals(listOf(4L), all.map(PendingEntry::id))
    }

    private fun entry(
        id: Long,
        type: PendingType,
        description: String,
        amountInCents: Long,
        categoryId: Long,
        date: LocalDate,
        isDone: Boolean = false,
        comment: String? = null,
    ) = PendingEntry(
        id = id,
        type = type,
        description = description,
        amountInCents = amountInCents,
        categoryId = categoryId,
        date = date,
        comment = comment,
        isDone = isDone,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
