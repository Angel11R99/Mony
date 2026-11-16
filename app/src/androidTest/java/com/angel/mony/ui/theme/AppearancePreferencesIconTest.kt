package com.angel.mony.ui.theme

import androidx.test.platform.app.InstrumentationRegistry
import com.angel.mony.ui.iconography.IconColorMode
import com.angel.mony.ui.iconography.IconPack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppearancePreferencesIconTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun clearPreferences() {
        context.getSharedPreferences("appearance_preferences", 0).edit().clear().commit()
    }

    @Test
    fun iconPreferencesSurvivePreferenceRecreation() {
        val blue = monyColorPalette.single { it.id == "blue" }.argb
        AppearancePreferences(context).apply {
            setIconPack(IconPack.FLUENT_EMOJI_FLAT)
            setCustomIconColor(blue)
        }
        val restored = AppearancePreferences(context).settings.value
        assertEquals(IconPack.FLUENT_EMOJI_FLAT, restored.iconPack)
        assertEquals(IconColorMode.CUSTOM, restored.iconColorMode)
        assertEquals(blue, restored.customIconColorArgb)
    }

    @Test
    fun legacyMonyColorPreferenceFallsBackToMaterialAndIsRemoved() {
        context.getSharedPreferences("appearance_preferences", 0).edit()
            .putString("icon_pack", "MONY_COLOR")
            .putString("mony_color_style", "MULTI_COLOR")
            .commit()
        val preferences = AppearancePreferences(context)
        assertEquals(IconPack.MATERIAL, preferences.settings.value.iconPack)
        assertEquals(IconPack.MATERIAL.name,
            context.getSharedPreferences("appearance_preferences", 0).getString("icon_pack", null))
        assertEquals(false, context.getSharedPreferences("appearance_preferences", 0).contains("mony_color_style"))
    }
}
