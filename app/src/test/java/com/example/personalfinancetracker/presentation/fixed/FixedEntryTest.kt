package com.example.personalfinancetracker.presentation.fixed

import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FixedEntryTest {
    @Test fun `creates movement using click date and preserves description and comment`() {
        val date = LocalDate.of(2026, 8, 13)
        val now = Instant.parse("2026-08-13T15:00:00Z")
        val entry = FixedEntry(
            id = 9,
            type = TransactionType.EXPENSE,
            description = "BHD Crédito",
            amountInCents = 1_100_000,
            categoryId = 3,
            comment = "Pago de la tarjeta",
        )

        val transaction = entry.toTransaction(date, now)

        assertEquals(0, transaction.id)
        assertEquals(date, transaction.date)
        assertEquals(now, transaction.createdAt)
        assertEquals("BHD Crédito · Pago de la tarjeta", transaction.description)
        assertEquals(1_100_000, transaction.amountInCents)
        assertEquals(TransactionType.EXPENSE, transaction.type)
    }
}
