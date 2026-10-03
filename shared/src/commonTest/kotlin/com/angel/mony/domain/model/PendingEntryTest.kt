package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

private val santoDomingo = TimeZone.of("America/Santo_Domingo")

class PendingEntryTest {
    @Test
    fun combinesPendingDateAndManuallySelectedTime() {
        assertEquals(
            Instant.parse("2026-08-29T12:30:00Z"),
            pendingReminderInstant(LocalDate(2026, 8, 29), LocalTime(8, 30), santoDomingo),
        )
    }

    @Test
    fun dateValidationAcceptsTodayAndFutureDates() {
        val today = LocalDate(2026, 8, 29)

        assertFalse(isPendingDateValid(today.plus(-1, DateTimeUnit.DAY), today))
        assertTrue(isPendingDateValid(today, today))
        assertTrue(isPendingDateValid(today.plus(1, DateTimeUnit.DAY), today))
    }

    @Test
    fun reminderValidationUsesTheProvidedTimeZone() {
        val now = Instant.parse("2026-08-29T12:00:00Z")

        assertFalse(isPendingReminderInFuture(LocalDate(2026, 8, 28), LocalTime(8, 0), now, santoDomingo))
        assertFalse(isPendingReminderInFuture(LocalDate(2026, 8, 29), LocalTime(7, 59), now, santoDomingo))
        assertTrue(isPendingReminderInFuture(LocalDate(2026, 8, 29), LocalTime(8, 1), now, santoDomingo))
    }
}
