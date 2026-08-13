package com.example.personalfinancetracker.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingModuleBarTest {
    @Test fun `selects each fixed module`() {
        assertTrue(isModuleSelected("home", "home", null))
        assertTrue(isModuleSelected("history", "history", null))
        assertFalse(isModuleSelected("home", "history", null))
    }

    @Test fun `distinguishes expense and income routes`() {
        assertTrue(isModuleSelected("add/EXPENSE", "add/{type}", "EXPENSE"))
        assertFalse(isModuleSelected("add/INCOME", "add/{type}", "EXPENSE"))
        assertTrue(isModuleSelected("add/INCOME", "edit/{type}/{transactionId}", "INCOME"))
    }
}
