package com.example.personalfinancetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test fun `formats thousands with comma separators`() {
        assertEquals("RD$25,000.00", MoneyFormatter.format(2_500_000L))
    }

    @Test fun `parses dot and comma decimals exactly`() {
        assertEquals(12_345L, MoneyFormatter.parseToCents("123.45"))
        assertEquals(12_345L, MoneyFormatter.parseToCents("123,45"))
    }

    @Test fun `rejects invalid precision`() {
        assertNull(MoneyFormatter.parseToCents("12.345"))
        assertNull(MoneyFormatter.parseToCents("texto"))
    }
}
