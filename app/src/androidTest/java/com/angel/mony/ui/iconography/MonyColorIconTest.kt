package com.angel.mony.ui.iconography

import androidx.compose.material3.MaterialTheme
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Colores literales del artwork IconPark que el pack Mony Color no debe perder. */
private val ICONPARK_BLUE: Int = 0xFF2F88FF.toInt()
private val ICONPARK_CYAN: Int = 0xFF43CCF8.toInt()

class MonyColorIconTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingsKeepsIconParkFillsWithCustomTintActive() {
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

        val opaqueColors = opaqueColorsOf("mony-color-savings")

        assertTrue(
            "El relleno azul de IconPark debe conservarse",
            opaqueColors.contains(ICONPARK_BLUE),
        )
        assertTrue(
            "El relleno cian de IconPark debe conservarse",
            opaqueColors.contains(ICONPARK_CYAN),
        )
        assertFalse(
            "El tinte solicitado no debe reemplazar la paleta del artwork",
            opaqueColors.contains(Color.Red.toArgb()),
        )
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

        val opaqueColors = opaqueColorsOf("dark-savings")

        assertTrue(opaqueColors.contains(ICONPARK_BLUE))
        assertTrue(opaqueColors.contains(ICONPARK_CYAN))
        assertTrue(
            "Dark Mode debe conservar rellenos y contorno",
            opaqueColors.size >= 3,
        )
    }

    @Test
    fun semanticStatesKeepTheirColorInsteadOfIconParkArtwork() {
        var errorColor = Color.Unspecified
        composeRule.setContent {
            PersonalFinanceTrackerTheme(iconPack = IconPack.MONY_COLOR) {
                errorColor = MaterialTheme.colorScheme.error
                MonyIcon(
                    icon = MonyIcon.Delete,
                    contentDescription = "Eliminar",
                    modifier = Modifier.testTag("mony-color-delete"),
                    tint = errorColor,
                    role = MonyIconRole.STATE,
                    size = 64.dp,
                )
            }
        }

        val opaqueColors = opaqueColorsOf("mony-color-delete")

        assertTrue(
            "Los estados semánticos conservan el color de error",
            opaqueColors.any { it == errorColor.toArgb() },
        )
        assertFalse(
            "Los estados semánticos no usan el artwork de IconPark",
            opaqueColors.contains(ICONPARK_BLUE),
        )
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

    private fun opaqueColorsOf(tag: String): Set<Int> {
        val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        return buildSet {
            for (x in 0 until pixels.width) {
                for (y in 0 until pixels.height) {
                    val color = pixels[x, y]
                    if (color.alpha > 0.9f) add(color.toArgb())
                }
            }
        }
    }
}
