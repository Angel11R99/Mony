package com.example.personalfinancetracker.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FinanceDarkColors = darkColorScheme(
    primary = BrandPurple,
    onPrimary = WarmWhite,
    primaryContainer = Color(0xFF32175E),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = BrandPurpleLight,
    onSecondary = BackgroundBlack,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = WarmWhite,
    tertiary = BrandPurpleLight,
    onTertiary = BackgroundBlack,
    background = BackgroundBlack,
    onBackground = WarmWhite,
    surface = SurfaceDark,
    onSurface = WarmWhite,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = NeutralGray,
    outline = DarkBorder,
    outlineVariant = Color(0xFF27272F),
    error = ExpenseRed,
    onError = BackgroundBlack,
    errorContainer = Color(0xFF481B20),
    onErrorContainer = Color(0xFFFFDADD),
    scrim = Color.Black,
)

private val FinanceLightColors = lightColorScheme(
    primary = LightPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF2B0758),
    secondary = LightPurpleMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE2F8),
    onSecondaryContainer = Color(0xFF29143B),
    tertiary = Color(0xFF5D4B76),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    outlineVariant = Color(0xFFE1DAE6),
    error = LightExpenseRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF41000A),
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
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FinanceDarkColors else FinanceLightColors,
        typography = Typography,
        shapes = FinanceShapes,
        content = content,
    )
}
