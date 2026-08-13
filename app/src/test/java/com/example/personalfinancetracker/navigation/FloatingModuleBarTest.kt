package com.example.personalfinancetracker.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingModuleBarTest {
    @Test fun `selects each fixed module`() {
        assertTrue(isModuleSelected("home", "home"))
        assertTrue(isModuleSelected("history", "history"))
        assertTrue(isModuleSelected("statistics", "statistics"))
        assertTrue(isModuleSelected("fixed", "fixed"))
        assertFalse(isModuleSelected("home", "history"))
    }
}
