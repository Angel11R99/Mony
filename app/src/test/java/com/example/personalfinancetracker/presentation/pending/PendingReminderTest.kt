package com.example.personalfinancetracker.presentation.pending

import com.example.personalfinancetracker.domain.model.pendingReminderInstant
import org.junit.Assert.assertEquals
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
}
