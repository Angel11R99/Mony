package com.angel.mony.ui.theme

import androidx.compose.ui.graphics.Color

val BrandPurple = Color(0xFF7C3AED)
val BrandPurpleLight = Color(0xFFA78BFA)
val DarkBackground = Color(0xFF1C1C1C)
val SurfaceDark = Color(0xFF26262A)
val SurfaceRaised = Color(0xFF303035)
val WarmWhite = Color(0xFFF7F4EF)
val NeutralGray = Color(0xFFD1CED3)
val DarkBorder = Color(0xFF4E4E55)
val ExpenseRed = Color(0xFFFF6B73)

val LightBackground = Color(0xFFF8F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceRaised = Color(0xFFF0ECF4)
val LightText = Color(0xFF1C1722)
val LightTextMuted = Color(0xFF6D6574)
val LightBorder = Color(0xFFCBC3D2)
val LightPurple = Color(0xFF6D28D9)
val LightPurpleMuted = Color(0xFF7043A8)
val LightExpenseRed = Color(0xFFC5283D)

data class AppColorOption(
    val id: String,
    val displayName: String,
    val argb: Int,
)

/** Paleta compartida por la personalización principal y de iconos. */
val monyColorPalette: List<AppColorOption> = listOf(
    AppColorOption("red", "Rojo", 0xFFEF4444.toInt()),
    AppColorOption("pink", "Rosa", 0xFFEC4899.toInt()),
    AppColorOption("purple", "Morado", 0xFF9333EA.toInt()),
    AppColorOption("violet", "Violeta", 0xFF7C3AED.toInt()),
    AppColorOption("blue", "Azul", 0xFF3B82F6.toInt()),
    AppColorOption("cyan", "Azul claro", 0xFF0891B2.toInt()),
    AppColorOption("turquoise", "Turquesa", 0xFF0D9488.toInt()),
    AppColorOption("green", "Verde", 0xFF16A34A.toInt()),
    AppColorOption("lime", "Lima", 0xFF65A30D.toInt()),
    AppColorOption("yellow", "Amarillo", 0xFFEAB308.toInt()),
    AppColorOption("amber", "Ámbar", 0xFFD97706.toInt()),
    AppColorOption("orange", "Naranja", 0xFFEA580C.toInt()),
    AppColorOption("white", "Blanco", 0xFFFFFFFF.toInt()),
    AppColorOption("gray", "Gris", 0xFF71717A.toInt()),
    AppColorOption("black", "Negro", 0xFF000000.toInt()),
)
