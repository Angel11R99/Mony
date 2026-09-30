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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
    fun outlineStyleDropsIconParkFills() {
        val outlineColors = colorsOfSavings(MonyColorStyle.OUTLINE, tag = "outline-savings")
        val multiColors = colorsOfSavings(MonyColorStyle.MULTI_COLOR, tag = "multi-savings")

        assertFalse(
            "Contorno no debe rellenar con el azul de IconPark",
            outlineColors.contains(ICONPARK_BLUE),
        )
        assertFalse(
            "Contorno no debe rellenar con el cian de IconPark",
            outlineColors.contains(ICONPARK_CYAN),
        )
        assertNotEquals(
            "Cambiar de diseño debe cambiar el relleno",
            multiColors,
            outlineColors,
        )
    }

    @Test
    fun twoToneStyleKeepsAccentButDropsTheCyanHighlight() {
        val colors = colorsOfSavings(MonyColorStyle.TWO_TONE, tag = "two-tone-savings")

        assertTrue(
            "Bicolor conserva el azul de IconPark",
            colors.contains(ICONPARK_BLUE),
        )
        assertFalse(
            "Bicolor no usa el cian reservado al diseño multicolor",
            colors.contains(ICONPARK_CYAN),
        )
    }

    @Test
    fun filledStyleCutsItsDetailOutOfTheBackgroundInDarkTheme() {
        var background = Color.Unspecified
        val colors = colorsOfSavings(
            style = MonyColorStyle.FILLED,
            tag = "filled-savings",
            darkTheme = true,
        ) { background = it }

        assertNotEquals("El fondo debe estar resuelto", Color.Unspecified, background)
        assertFalse(
            "Sólido no debe conservar los rellenos ilustrados de multicolor",
            colors.contains(ICONPARK_BLUE) || colors.contains(ICONPARK_CYAN),
        )
        assertTrue(
            "Sólido debe pintar el cuerpo y recortar el detalle contra el fondo",
            colors.contains(background.toArgb()) && colors.size >= 2,
        )
    }

    @Test
    fun everyStyleIsSelectableAndProducesArtwork() {
        MonyColorStyle.entries.forEach { style ->
            val vector = MonyIconResolver.resolveMonyColor(
                MonyIcon.Savings,
                style.palette(Color.Black),
            )

            assertNotNull("$style debe producir artwork", vector)
            assertEquals("IconPark.strongbox", vector!!.name)
        }
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

    /**
     * Renders [MonyIcon.Savings] with the given IconPark design and returns its opaque colours.
     *
     * @param onBackground receives the theme background so a test can assert the filled design
     *   cuts its detail out with the surrounding surface instead of a hardcoded white.
     */
    private fun colorsOfSavings(
        style: MonyColorStyle,
        tag: String,
        darkTheme: Boolean = false,
        onBackground: (Color) -> Unit = {},
    ): Set<Int> {
        composeRule.setContent {
            PersonalFinanceTrackerTheme(
                darkTheme = darkTheme,
                iconPack = IconPack.MONY_COLOR,
                monyColorStyle = style,
            ) {
                onBackground(MaterialTheme.colorScheme.background)
                MonyIcon(
                    icon = MonyIcon.Savings,
                    contentDescription = "Ahorro $style",
                    modifier = Modifier.testTag(tag),
                    size = 64.dp,
                )
            }
        }

        return opaqueColorsOf(tag)
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
