package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class DateRangeTest {
    @Test
    fun firstFortnightEndsOnDay15() {
        val range = DateRange.currentFortnight(LocalDate(2026, 8, 15))
        assertEquals(LocalDate(2026, 8, 1), range.start)
        assertEquals(LocalDate(2026, 8, 15), range.endInclusive)
    }

    @Test
    fun secondFortnightEndsOnLastDayOfMonth() {
        val range = DateRange.currentFortnight(LocalDate(2024, 2, 16))
        assertEquals(LocalDate(2024, 2, 16), range.start)
        assertEquals(LocalDate(2024, 2, 29), range.endInclusive)
    }

    @Test
    fun currentMonthCoversTheCompleteMonth() {
        val range = DateRange.currentMonth(LocalDate(2024, 2, 10))
        assertEquals(LocalDate(2024, 2, 1), range.start)
        assertEquals(LocalDate(2024, 2, 29), range.endInclusive)
    }
}
