package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateRangeTest {
    @Test
    fun `first fortnight ends on day 15`() {
        val range = DateRange.currentFortnight(LocalDate.of(2026, 8, 15))
        assertEquals(LocalDate.of(2026, 8, 1), range.start)
        assertEquals(LocalDate.of(2026, 8, 15), range.endInclusive)
    }

    @Test
    fun `second fortnight ends on last day of month`() {
        val range = DateRange.currentFortnight(LocalDate.of(2024, 2, 16))
        assertEquals(LocalDate.of(2024, 2, 16), range.start)
        assertEquals(LocalDate.of(2024, 2, 29), range.endInclusive)
    }

    @Test
    fun `current month covers the complete month`() {
        val range = DateRange.currentMonth(LocalDate.of(2024, 2, 10))
        assertEquals(LocalDate.of(2024, 2, 1), range.start)
        assertEquals(LocalDate.of(2024, 2, 29), range.endInclusive)
    }
}
