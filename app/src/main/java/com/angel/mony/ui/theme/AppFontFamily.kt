package com.angel.mony.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Familias tipográficas globales configurables.
 * Se mantienen tamaños/weights de Type.kt, sólo cambia FontFamily.
 * Las combinaciones se apoyan en fuentes del sistema para evitar dependencias pesadas
 * y mantener offline-first; cada entrada representa un estilo Google Fonts real
 * pero mapeado a famílias disponibles sin descarga obligatoria.
 * Si se añaden fuentes descargables, este mapeo es el único punto a actualizar.
 */
enum class AppFontFamily {
    SYSTEM,
    INTER,
    MANROPE,
    DM_SANS,
    NUNITO_SANS,
    OUTFIT,
    SPACE_GROTESK,
    PLUS_JAKARTA,
}

val AppFontFamily.displayName: String
    get() = when (this) {
        AppFontFamily.SYSTEM -> "Sistema"
        AppFontFamily.INTER -> "Inter"
        AppFontFamily.MANROPE -> "Manrope"
        AppFontFamily.DM_SANS -> "DM Sans"
        AppFontFamily.NUNITO_SANS -> "Nunito Sans"
        AppFontFamily.OUTFIT -> "Outfit"
        AppFontFamily.SPACE_GROTESK -> "Space Grotesk"
        AppFontFamily.PLUS_JAKARTA -> "Plus Jakarta Sans"
    }

val AppFontFamily.subtitle: String
    get() = when (this) {
        AppFontFamily.SYSTEM -> "Estándar del sistema"
        AppFontFamily.INTER -> "Limpia y neutral"
        AppFontFamily.MANROPE -> "Geométrica y amigable"
        AppFontFamily.DM_SANS -> "Moderna y equilibrada"
        AppFontFamily.NUNITO_SANS -> "Suave y accesible"
        AppFontFamily.OUTFIT -> "Tecnológica, corte limpio"
        AppFontFamily.SPACE_GROTESK -> "Display geométrico"
        AppFontFamily.PLUS_JAKARTA -> "Ejecutiva y legible"
    }

/**
 * Recomendación discreta según familia geométrica. No se aplica automáticamente.
 */
fun AppShapeStyle.recommendedFont(): AppFontFamily = when (this) {
    AppShapeStyle.SQUARE -> AppFontFamily.INTER
    AppShapeStyle.ROUNDED -> AppFontFamily.DM_SANS
    AppShapeStyle.SOFT -> AppFontFamily.NUNITO_SANS
    AppShapeStyle.PILL -> AppFontFamily.NUNITO_SANS
    AppShapeStyle.CUT -> AppFontFamily.SPACE_GROTESK
    AppShapeStyle.OPPOSITE_CUT -> AppFontFamily.SPACE_GROTESK
    AppShapeStyle.SINGLE_CUT -> AppFontFamily.OUTFIT
    AppShapeStyle.CHEVRON -> AppFontFamily.OUTFIT
    AppShapeStyle.BEVELED -> AppFontFamily.SPACE_GROTESK
    AppShapeStyle.NOTCHED -> AppFontFamily.MANROPE
    AppShapeStyle.TICKET -> AppFontFamily.DM_SANS
    AppShapeStyle.ASYMMETRIC -> AppFontFamily.MANROPE
    AppShapeStyle.TOP_ROUNDED -> AppFontFamily.PLUS_JAKARTA
}

/**
 * Mapeo centralizado a FontFamily de Compose.
 * Sin fuentes descargables se reutilizan familias del sistema (Default/SansSerif/Serif/Monospace/Cursive)
 * y la diferenciación real se completa en [createAppTypography] con tracking/leading/weight.
 * Garantiza invariancia offline; si se añaden .ttf, reemplazar aquí por Font(R.font.xxx).
 */
fun AppFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    AppFontFamily.SYSTEM -> FontFamily.Default
    AppFontFamily.INTER -> FontFamily.SansSerif
    AppFontFamily.MANROPE -> FontFamily.Serif
    AppFontFamily.DM_SANS -> FontFamily.SansSerif
    AppFontFamily.NUNITO_SANS -> FontFamily.Cursive
    AppFontFamily.OUTFIT -> FontFamily.Serif
    AppFontFamily.SPACE_GROTESK -> FontFamily.Monospace
    AppFontFamily.PLUS_JAKARTA -> FontFamily.Default
}
