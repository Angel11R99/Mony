package com.example.personalfinancetracker.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppAppearanceTest {
    @Test fun `accepts hex colors with or without hash`() {
        assertEquals(0xFF2563EB.toInt(), parseHexColor("#2563EB"))
        assertEquals(0xFFFF6B73.toInt(), parseHexColor("ff6b73"))
    }

    @Test fun `rejects incomplete or invalid colors`() {
        assertNull(parseHexColor("#123"))
        assertNull(parseHexColor("#GG33AA"))
    }

    @Test fun `formats colors without exposing alpha`() {
        assertEquals("#7C3AED", 0xFF7C3AED.toInt().toHexColor())
    }
}
