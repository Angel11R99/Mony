package com.angel.mony.core.time

import java.time.Instant as JavaInstant
import java.time.LocalDate as JavaLocalDate
import kotlinx.datetime.Instant as KotlinInstant
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class JavaTimeInteropTest {

    @Test
    fun conversionPreservesLeapDayInBothDirections() {
        val javaDate = JavaLocalDate.of(2024, 2, 29)
        val kotlinDate = KotlinLocalDate(2024, 2, 29)

        assertEquals(kotlinDate, javaDate.toKotlinLocalDate())
        assertEquals(javaDate, kotlinDate.toJavaLocalDate())
    }

    @Test
    fun conversionPreservesInstantNanosecondsInBothDirections() {
        val javaInstant = JavaInstant.parse("2026-10-02T15:30:45.123456789Z")
        val kotlinInstant = KotlinInstant.parse("2026-10-02T15:30:45.123456789Z")

        assertEquals(kotlinInstant, javaInstant.toKotlinInstant())
        assertEquals(javaInstant, kotlinInstant.toJavaInstant())
    }
}
