package com.angel.mony.presentation.pending

import com.angel.mony.domain.model.pendingReminderInstant
import com.angel.mony.domain.model.isPendingReminderInFuture
import com.angel.mony.domain.model.isPendingDateValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PendingReminderTest {
    @Test fun `combines pending date and manually selected time`() {
        assertEquals(
            Instant.parse("2026-08-29T12:30:00Z"),
            pendingReminderInstant(
                date = LocalDate.of(2026, 8, 29),
                time = LocalTime.of(8, 30),
                zoneId = ZoneId.of("America/Santo_Domingo"),
            ),
        )
    }

    @Test fun `rejects past dates and times but accepts a future time today`() {
        val zone = ZoneId.of("America/Santo_Domingo")
        val now = Instant.parse("2026-08-29T12:00:00Z")

        assertFalse(isPendingReminderInFuture(LocalDate.of(2026, 8, 28), LocalTime.of(8, 0), now, zone))
        assertFalse(isPendingReminderInFuture(LocalDate.of(2026, 8, 29), LocalTime.of(7, 59), now, zone))
        assertTrue(isPendingReminderInFuture(LocalDate.of(2026, 8, 29), LocalTime.of(8, 1), now, zone))
    }

    @Test fun `pending date accepts today and future but rejects past days`() {
        val today = LocalDate.of(2026, 8, 29)

        assertFalse(isPendingDateValid(today.minusDays(1), today))
        assertTrue(isPendingDateValid(today, today))
        assertTrue(isPendingDateValid(today.plusDays(1), today))
    }
}
