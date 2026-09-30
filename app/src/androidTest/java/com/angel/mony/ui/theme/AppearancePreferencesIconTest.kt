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
        AppearancePreferences(context).apply {
            setIconPack(IconPack.PHOSPHOR)
            setCustomIconColor(0xFF1565C0.toInt())
        }

        val restored = AppearancePreferences(context).settings.value

        assertEquals(IconPack.PHOSPHOR, restored.iconPack)
        assertEquals(IconColorMode.CUSTOM, restored.iconColorMode)
        assertEquals(0xFF1565C0.toInt(), restored.customIconColorArgb)
    }
}
