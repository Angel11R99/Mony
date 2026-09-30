package com.angel.mony.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.angel.mony.ui.theme.PersonalFinanceTrackerTheme
import com.angel.mony.ui.theme.monyColorPalette
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IconColorPaletteSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingBlueUpdatesTheVisualSelectionImmediately() {
        val purple = monyColorPalette.single { it.id == "purple" }.argb
        val blue = monyColorPalette.single { it.id == "blue" }.argb
        var selectedArgb = purple

        composeRule.setContent {
            var selected by remember { mutableIntStateOf(purple) }
            selectedArgb = selected
            PersonalFinanceTrackerTheme {
                IconColorPaletteSelector(
                    selectedArgb = selected,
                    onSelect = { selected = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Azul").performClick()
        composeRule.onNodeWithContentDescription("Azul").assertIsSelected()
        composeRule.runOnIdle { assertEquals(blue, selectedArgb) }
    }
}
