package com.example.personalfinancetracker.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

private fun financeDarkColors(primarySeed: Color, accentSeed: Color) = darkColorScheme(
    primary = primarySeed,
    onPrimary = contentColorFor(primarySeed),
    primaryContainer = shiftTone(primarySeed, saturationFactor = 0.82f, value = 0.35f),
    onPrimaryContainer = shiftTone(primarySeed, saturationFactor = 0.22f, value = 1f),
    secondary = shiftTone(primarySeed, saturationFactor = 0.55f, value = 0.92f),
    onSecondary = DarkBackground,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = WarmWhite,
    tertiary = shiftTone(primarySeed, saturationFactor = 0.42f, value = 0.9f),
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = WarmWhite,
    surface = SurfaceDark,
    onSurface = WarmWhite,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = NeutralGray,
    outline = DarkBorder,
    outlineVariant = Color(0xFF3F3F45),
    error = accentSeed,
    onError = contentColorFor(accentSeed),
    errorContainer = shiftTone(accentSeed, saturationFactor = 0.78f, value = 0.3f),
    onErrorContainer = shiftTone(accentSeed, saturationFactor = 0.2f, value = 1f),
    scrim = Color.Black,
)

private fun financeLightColors(primarySeed: Color, accentSeed: Color) = lightColorScheme(
    primary = primarySeed,
    onPrimary = contentColorFor(primarySeed),
    primaryContainer = shiftTone(primarySeed, saturationFactor = 0.18f, value = 0.98f),
    onPrimaryContainer = shiftTone(primarySeed, saturationFactor = 0.9f, value = 0.3f),
    secondary = shiftTone(primarySeed, saturationFactor = 0.72f, value = 0.66f),
    onSecondary = Color.White,
    secondaryContainer = shiftTone(primarySeed, saturationFactor = 0.12f, value = 0.97f),
    onSecondaryContainer = shiftTone(primarySeed, saturationFactor = 0.72f, value = 0.28f),
    tertiary = shiftTone(primarySeed, saturationFactor = 0.45f, value = 0.58f),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    outlineVariant = Color(0xFFE1DAE6),
    error = accentSeed,
    onError = contentColorFor(accentSeed),
    errorContainer = shiftTone(accentSeed, saturationFactor = 0.16f, value = 1f),
    onErrorContainer = shiftTone(accentSeed, saturationFactor = 0.88f, value = 0.28f),
    scrim = Color.Black,
)

val FinanceShapes = Shapes(
    extraSmall = CutCornerShape(topEnd = 4.dp),
    small = CutCornerShape(topEnd = 6.dp),
    medium = CutCornerShape(topEnd = 10.dp),
    large = CutCornerShape(topEnd = 14.dp),
    extraLarge = CutCornerShape(topEnd = 18.dp),
)

@Composable
fun PersonalFinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primarySeed: Color = BrandPurple,
    accentSeed: Color = ExpenseRed,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) financeDarkColors(primarySeed, accentSeed)
        else financeLightColors(primarySeed, accentSeed),
        typography = Typography,
        shapes = FinanceShapes,
        content = content,
    )
}

internal fun shiftTone(seed: Color, saturationFactor: Float, value: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    hsv[1] = (hsv[1] * saturationFactor).coerceIn(0f, 1f)
    hsv[2] = value.coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.48f) Color(0xFF121016) else Color.White
