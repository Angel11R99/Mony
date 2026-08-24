package com.example.personalfinancetracker.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingModuleBarTest {
    @Test fun `selects each fixed module`() {
        assertTrue(isModuleSelected("home", "home"))
        assertTrue(isModuleSelected("history", "history"))
        assertTrue(isModuleSelected("statistics", "statistics"))
        assertTrue(isModuleSelected("fixed", "fixed"))
        assertTrue(isModuleSelected("savings", "savings"))
        assertFalse(isModuleSelected("home", "history"))
    }

    @Test fun `every destination has a unique route`() {
        val routes = moduleDestinations.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
        assertTrue(routes.containsAll(listOf("home", "fixed", "pending", "savings", "statistics", "history")))
    }
}
