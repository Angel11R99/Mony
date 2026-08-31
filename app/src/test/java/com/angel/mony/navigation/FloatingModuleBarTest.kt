package com.angel.mony.navigation

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
        assertTrue(isModuleSelected("list", "list"))
        assertFalse(isModuleSelected("home", "history"))
    }

    @Test fun `every destination has a unique route`() {
        val routes = moduleDestinations.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
        assertTrue(routes.containsAll(listOf("home", "fixed", "pending", "savings", "list", "statistics", "history")))
    }

    @Test fun `adds list only to old default preferences`() {
        val oldDefault = setOf("home", "fixed", "pending", "savings", "statistics", "history")
        assertTrue(FloatingModuleBarPreferences.includeListInLegacyDefaults(null).contains("list"))
        assertTrue(FloatingModuleBarPreferences.includeListInLegacyDefaults(oldDefault).contains("list"))
        assertEquals(setOf("home"), FloatingModuleBarPreferences.includeListInLegacyDefaults(setOf("home")))
    }

    @Test fun `module bar is only visible on top level routes`() {
        assertTrue(shouldShowModuleBar("list"))
        assertTrue(shouldShowModuleBar("home"))
        assertFalse(shouldShowModuleBar("list/{listId}"))
        assertFalse(shouldShowModuleBar("settings"))
        assertFalse(shouldShowModuleBar(null))
    }

    @Test fun `list is an accepted initial destination`() {
        assertTrue(isValidInitialDestination("list"))
        assertFalse(isValidInitialDestination("list/42"))
    }
}
