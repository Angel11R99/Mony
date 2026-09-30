package com.angel.mony.ui.iconography

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.angel.mony.ui.theme.PersonalFinanceTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MonyColorIconTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingsKeepsMultipleInternalColorsWithCustomTintActive() {
        composeRule.setContent {
            PersonalFinanceTrackerTheme(
                iconPack = IconPack.MONY_COLOR,
                iconColorMode = IconColorMode.CUSTOM,
                customIconColor = Color.Blue,
            ) {
                MonyIcon(
                    icon = MonyIcon.Savings,
                    contentDescription = "Ahorro Mony Color",
                    modifier = Modifier.testTag("mony-color-savings"),
                    tint = Color.Red,
                    size = 64.dp,
                )
            }
        }

        val pixels = composeRule.onNodeWithTag("mony-color-savings").captureToImage().toPixelMap()
        val opaqueColors = buildSet {
            for (x in 0 until pixels.width) {
                for (y in 0 until pixels.height) {
                    val color = pixels[x, y]
                    if (color.alpha > 0.9f) add(color.toArgb())
                }
            }
        }

        assertTrue(opaqueColors.contains(MonyColorPalette.Pink.toArgb()))
        assertTrue(opaqueColors.contains(MonyColorPalette.Yellow.toArgb()))
        assertTrue("El icono debe conservar varios colores internos", opaqueColors.size >= 4)
    }

    @Test
    fun savingsRemainsMulticolorInDarkTheme() {
        composeRule.setContent {
            PersonalFinanceTrackerTheme(darkTheme = true, iconPack = IconPack.MONY_COLOR) {
                MonyIcon(
                    icon = MonyIcon.Savings,
                    contentDescription = "Ahorro oscuro",
                    modifier = Modifier.testTag("dark-savings"),
                    size = 64.dp,
                )
            }
        }

        val pixels = composeRule.onNodeWithTag("dark-savings").captureToImage().toPixelMap()
        val opaqueColors = buildSet {
            for (x in 0 until pixels.width) {
                for (y in 0 until pixels.height) {
                    val color = pixels[x, y]
                    if (color.alpha > 0.9f) add(color.toArgb())
                }
            }
        }

        assertTrue(opaqueColors.contains(MonyColorPalette.Pink.toArgb()))
        assertTrue(opaqueColors.contains(MonyColorPalette.Yellow.toArgb()))
        assertTrue("Dark Mode debe conservar rellenos y contorno", opaqueColors.size >= 4)
    }

    @Test
    fun defaultSizeMatchesMonochromePacks() {
        composeRule.setContent {
            PersonalFinanceTrackerTheme {
                MonyIcon(
                    icon = MonyIcon.Savings,
                    contentDescription = "Material",
                    modifier = Modifier.testTag("material-size"),
                    packOverride = IconPack.MATERIAL,
                )
                MonyIcon(
                    icon = MonyIcon.Savings,
                    contentDescription = "Mony Color",
                    modifier = Modifier.testTag("mony-color-size"),
                    packOverride = IconPack.MONY_COLOR,
                )
            }
        }

        composeRule.onNodeWithTag("material-size")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
        composeRule.onNodeWithTag("mony-color-size")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }
}
