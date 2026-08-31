package com.angel.mony.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

/**
 * Tipografía centralizada parametrizada por familia.
 * Cada [AppFontFamily] produce una Typography con la misma escala pero con
 * tracking/leading/weight sutilmente distintos para que el cambio sea
 * perceptible incluso cuando varias familias comparten FontFamily del sistema
 * (offline-first sin .ttf). Cuando se añadan fuentes descargables, este es
 * el único punto a ajustar.
 */
fun createAppTypography(fontFamily: AppFontFamily): Typography {
    val family = fontFamily.toComposeFontFamily()
    // Ajustes por familia para diferenciación visual inmediata.
    // Valores inspirados en las personalidades reales de cada Google Font.
    val (displayTracking, headlineTracking, titleTracking, bodySpacing, labelTracking) = when (fontFamily) {
        AppFontFamily.SYSTEM -> listOf(-1.4.sp, -0.5.sp, -0.2.sp, 0.sp, 0.2.sp)
        AppFontFamily.INTER -> listOf(-1.1.sp, -0.4.sp, -0.15.sp, 0.sp, 0.15.sp) // neutra, compacta
        AppFontFamily.MANROPE -> listOf(-0.8.sp, -0.2.sp, 0.sp, 0.15.sp, 0.35.sp) // geométrica, aireada
        AppFontFamily.DM_SANS -> listOf(-1.0.sp, -0.35.sp, -0.1.sp, 0.05.sp, 0.25.sp) // equilibrada
        AppFontFamily.NUNITO_SANS -> listOf(-0.6.sp, -0.15.sp, 0.05.sp, 0.2.sp, 0.4.sp) // suave, redondeada
        AppFontFamily.OUTFIT -> listOf(-1.3.sp, -0.6.sp, -0.25.sp, -0.05.sp, 0.1.sp) // tecnológica, condensada
        AppFontFamily.SPACE_GROTESK -> listOf(-0.9.sp, -0.3.sp, 0.1.sp, 0.1.sp, 0.6.sp) // display, tracking amplio
        AppFontFamily.PLUS_JAKARTA -> listOf(-1.2.sp, -0.45.sp, -0.12.sp, 0.08.sp, 0.3.sp) // ejecutiva
    }
    // Pesos ligeramente distintos para reforzar personalidad sin romper jerarquía.
    val displayWeight = when (fontFamily) {
        AppFontFamily.NUNITO_SANS -> FontWeight.Bold
        AppFontFamily.SPACE_GROTESK -> FontWeight.Black
        else -> FontWeight.ExtraBold
    }
    val bodyWeight = when (fontFamily) {
        AppFontFamily.INTER -> FontWeight.Normal
        AppFontFamily.SPACE_GROTESK -> FontWeight.Medium
        AppFontFamily.NUNITO_SANS -> FontWeight.Normal
        else -> FontWeight.Medium
    }

    return Typography(
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = displayWeight,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = displayTracking,
            lineBreak = LineBreak.Heading,
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = displayWeight,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            letterSpacing = headlineTracking,
            lineBreak = LineBreak.Heading,
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            letterSpacing = titleTracking,
            lineBreak = LineBreak.Heading,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            letterSpacing = bodySpacing,
            lineBreak = LineBreak.Heading,
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            letterSpacing = bodySpacing,
            lineBreak = LineBreak.Heading,
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = bodyWeight,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = bodySpacing * 0.3f,
            lineBreak = LineBreak.Paragraph,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = bodySpacing * 0.2f,
            lineBreak = LineBreak.Paragraph,
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = bodySpacing * 0.2f,
            lineBreak = LineBreak.Paragraph,
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = labelTracking,
        ),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = labelTracking),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = (labelTracking.value + 0.6).sp),
    )
}

// Mantener Typography por defecto para compatibilidad (equivale a SYSTEM)
val Typography: Typography = createAppTypography(AppFontFamily.SYSTEM)
