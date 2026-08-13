package com.example.personalfinancetracker.presentation.fixed

import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.FixedDateMode
import com.example.personalfinancetracker.domain.model.FixedScheduleMode
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.calculateNextRun
import com.example.personalfinancetracker.domain.model.manualPostingDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    @Test fun `manual modes resolve previous periods and specific date`() {
        val today = LocalDate.of(2026, 8, 20)
        val base = FixedEntry(
            type = TransactionType.EXPENSE,
            description = "Pago",
            amountInCents = 100,
            categoryId = 1,
            comment = null,
        )

        assertEquals(
            LocalDate.of(2026, 8, 15),
            base.copy(manualDateMode = FixedDateMode.PREVIOUS_FORTNIGHT).manualPostingDate(today),
        )
        assertEquals(
            LocalDate.of(2026, 7, 31),
            base.copy(manualDateMode = FixedDateMode.PREVIOUS_MONTH).manualPostingDate(today),
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            base.copy(
                manualDateMode = FixedDateMode.SPECIFIC_DATE,
                manualSpecificDate = LocalDate.of(2024, 2, 29),
            ).manualPostingDate(today),
        )
    }

    @Test fun `calculates next fortnight month and specific schedules`() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-08-13T10:00:00Z")

        assertEquals(
            Instant.parse("2026-08-16T09:00:00Z"),
            calculateNextRun(FixedScheduleMode.AFTER_FORTNIGHT, 9, null, now, zone),
        )
        assertEquals(
            Instant.parse("2026-09-01T09:00:00Z"),
            calculateNextRun(FixedScheduleMode.AFTER_MONTH, 9, null, now, zone),
        )
        assertEquals(
            Instant.parse("2026-08-14T18:00:00Z"),
            calculateNextRun(
                FixedScheduleMode.SPECIFIC_DATE_TIME,
                18,
                LocalDate.of(2026, 8, 14),
                now,
                zone,
            ),
        )
        assertEquals(
            null,
            calculateNextRun(
                FixedScheduleMode.SPECIFIC_DATE_TIME,
                9,
                LocalDate.of(2026, 8, 13),
                now,
                zone,
            ),
        )
    }
}
