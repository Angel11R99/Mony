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

    @Test fun `module transition follows bar order`() {
        assertTrue(isForwardModuleTransition("home", "statistics"))
        assertFalse(isForwardModuleTransition("history", "fixed"))
        assertTrue(isForwardModuleTransition("list?query=arroz", "statistics"))
    }

    @Test fun `settings index and destinations use animated navigation`() {
        assertTrue(isAnimatedNavigationTransition("home", "settings"))
        assertTrue(isAnimatedNavigationTransition("settings", "settings/personalization"))
        assertTrue(isAnimatedNavigationTransition("settings/finance", "settings"))
        assertFalse(isAnimatedNavigationTransition("settings/finance", "edit/EXPENSE/1"))
    }

    @Test fun `transaction forms use animated navigation in both directions`() {
        assertTrue(isAnimatedNavigationTransition("home", "add/{type}"))
        assertTrue(isAnimatedNavigationTransition("home", "add/EXPENSE"))
        assertTrue(isAnimatedNavigationTransition("history", "edit/INCOME/12"))
        assertTrue(isAnimatedNavigationTransition("edit/{type}/{transactionId}", "history"))
        assertFalse(isAnimatedNavigationTransition("settings", "add/EXPENSE"))
    }

    @Test fun `settings navigation direction follows its hierarchy`() {
        assertTrue(isForwardNavigationTransition("home", "settings"))
        assertTrue(isForwardNavigationTransition("settings", "settings/categories"))
        assertFalse(isForwardNavigationTransition("settings/navigation", "settings"))
        assertFalse(isForwardNavigationTransition("settings", "home"))
    }

    @Test fun `transaction form opens forward and closes backward`() {
        assertTrue(isForwardNavigationTransition("home", "add/{type}"))
        assertTrue(isForwardNavigationTransition("history", "edit/EXPENSE/7"))
        assertFalse(isForwardNavigationTransition("add/{type}", "home"))
        assertFalse(isForwardNavigationTransition("edit/INCOME/7", "history"))
    }

    @Test fun `fade remains the default transition and none is available`() {
        assertEquals(ModuleTransitionStyle.FADE, FloatingModuleBarConfig().transitionStyle)
        assertTrue(ModuleTransitionStyle.entries.contains(ModuleTransitionStyle.NONE))
    }
}
