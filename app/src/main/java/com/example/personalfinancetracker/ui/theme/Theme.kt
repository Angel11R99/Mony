package com.example.personalfinancetracker.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FinanceColors = darkColorScheme(
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

val FinanceShapes = Shapes(
    extraSmall = CutCornerShape(topEnd = 4.dp),
    small = CutCornerShape(topEnd = 6.dp),
    medium = CutCornerShape(topEnd = 10.dp),
    large = CutCornerShape(topEnd = 14.dp),
    extraLarge = CutCornerShape(topEnd = 18.dp),
)

@Composable
fun PersonalFinanceTrackerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FinanceColors,
        typography = Typography,
        shapes = FinanceShapes,
        content = content,
    )
}
