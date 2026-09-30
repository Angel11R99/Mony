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
            setIconPack(IconPack.MONY_COLOR)
            setCustomIconColor(blue)
            assertEquals(blue, settings.value.customIconColorArgb)
        }

        val restored = AppearancePreferences(context).settings.value

        assertEquals(IconPack.MONY_COLOR, restored.iconPack)
        assertEquals(IconColorMode.CUSTOM, restored.iconColorMode)
        assertEquals(blue, restored.customIconColorArgb)
    }

    @Test
    fun appearanceDefaultsRemainCompatibleForExistingUsers() {
        val restored = AppearancePreferences(context).settings.value

        assertEquals(AppThemeMode.SYSTEM, restored.themeMode)
        assertEquals(IconPack.MATERIAL, restored.iconPack)
        assertEquals(IconColorMode.AUTOMATIC, restored.iconColorMode)
    }

    @Test
    fun themePrimaryAndIconPreferencesSurvivePreferenceRecreation() {
        val purple = monyColorPalette.single { it.id == "purple" }.argb
        AppearancePreferences(context).apply {
            setThemeMode(AppThemeMode.DARK)
            setPrimaryColor(purple)
            setIconPack(IconPack.LUCIDE)
            setIconColorMode(IconColorMode.PRIMARY)
        }

        val restored = AppearancePreferences(context).settings.value

        assertEquals(AppThemeMode.DARK, restored.themeMode)
        assertEquals(purple, restored.primaryArgb)
        assertEquals(IconPack.LUCIDE, restored.iconPack)
        assertEquals(IconColorMode.PRIMARY, restored.iconColorMode)
    }
}
