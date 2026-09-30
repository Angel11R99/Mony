package com.angel.mony.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsNavigationTest {
    @Test
    fun `every settings destination returns to settings index`() {
        SettingsRoutes.destinations.forEach { route ->
            assertEquals(SettingsRoutes.ROOT, SettingsRoutes.parentOf(route))
        }
    }

    @Test
    fun `settings index has no settings parent`() {
        assertNull(SettingsRoutes.parentOf(SettingsRoutes.ROOT))
    }

    @Test
    fun `personalization and categories are independent destinations`() {
        assertEquals("settings/personalization", SettingsRoutes.PERSONALIZATION)
        assertEquals("settings/categories", SettingsRoutes.CATEGORIES)
    }
}
