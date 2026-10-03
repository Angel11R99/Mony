package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class FixedEntryScheduleTest {
    private val entry = FixedEntry(
        type = TransactionType.EXPENSE,
        description = "Pago",
        amountInCents = 100,
        categoryId = 1,
        comment = null,
    )

    @Test
    fun manualModesResolvePreviousPeriodsAndSpecificDate() {
        val today = LocalDate(2026, 8, 20)

        assertEquals(
            LocalDate(2026, 8, 15),
            entry.copy(manualDateMode = FixedDateMode.PREVIOUS_FORTNIGHT).manualPostingDate(today),
        )
        assertEquals(
            LocalDate(2026, 7, 31),
            entry.copy(manualDateMode = FixedDateMode.PREVIOUS_MONTH).manualPostingDate(today),
        )
        assertEquals(
            LocalDate(2024, 2, 29),
            entry.copy(
                manualDateMode = FixedDateMode.SPECIFIC_DATE,
                manualSpecificDate = LocalDate(2024, 2, 29),
            ).manualPostingDate(today),
        )
    }

    @Test
    fun calculatesRecurringAndSpecificSchedules() {
        val utc = TimeZone.UTC
        val now = Instant.parse("2026-08-13T10:00:00Z")

        assertEquals(
            Instant.parse("2026-08-16T09:00:00Z"),
            calculateNextRun(FixedScheduleMode.AFTER_FORTNIGHT, 9, null, now, utc),
        )
        assertEquals(
            Instant.parse("2026-09-01T09:00:00Z"),
            calculateNextRun(FixedScheduleMode.AFTER_MONTH, 9, null, now, utc),
        )
        assertEquals(
            Instant.parse("2026-08-14T18:00:00Z"),
            calculateNextRun(
                FixedScheduleMode.SPECIFIC_DATE_TIME,
                18,
                LocalDate(2026, 8, 14),
                now,
                utc,
            ),
        )
        assertNull(
            calculateNextRun(
                FixedScheduleMode.SPECIFIC_DATE_TIME,
                9,
                LocalDate(2026, 8, 13),
                now,
                utc,
            ),
        )
    }
}
