package com.angel.mony.ui.theme

/**
 * Catálogo de familias geométricas coherentes para la UI interna.
 * Cada estilo representa un lenguaje visual, no una sola forma literal.
 * Los componentes adaptan la silueta (button/card/chip/dialog) dentro de la misma familia
 * para mantener legibilidad y accesibilidad.
 */
enum class AppShapeStyle {
    SQUARE,
    ROUNDED,
    SOFT,
    PILL,
    CUT,
    OPPOSITE_CUT,
    SINGLE_CUT,
    CHEVRON,
    BEVELED,
    NOTCHED,
    TICKET,
    ASYMMETRIC,
    TOP_ROUNDED,
}

val AppShapeStyle.displayName: String
    get() = when (this) {
        AppShapeStyle.SQUARE -> "Cuadrado"
        AppShapeStyle.ROUNDED -> "Redondeado"
        AppShapeStyle.SOFT -> "Suave"
        AppShapeStyle.PILL -> "Cápsula"
        AppShapeStyle.CUT -> "Corte"
        AppShapeStyle.OPPOSITE_CUT -> "Diagonal doble"
        AppShapeStyle.SINGLE_CUT -> "Diagonal simple"
        AppShapeStyle.CHEVRON -> "Chevron"
        AppShapeStyle.BEVELED -> "Biselado"
        AppShapeStyle.NOTCHED -> "Muesca"
        AppShapeStyle.TICKET -> "Ticket"
        AppShapeStyle.ASYMMETRIC -> "Asimétrico"
        AppShapeStyle.TOP_ROUNDED -> "Superior redondeado"
    }

val AppShapeStyle.description: String
    get() = when (this) {
        AppShapeStyle.SQUARE -> "Bordes rectos, máxima neutralidad"
        AppShapeStyle.ROUNDED -> "Equilibrio moderno y limpio"
        AppShapeStyle.SOFT -> "Muy redondeado, amigable"
        AppShapeStyle.PILL -> "Extremos totalmente redondeados"
        AppShapeStyle.CUT -> "Esquinas cortadas"
        AppShapeStyle.OPPOSITE_CUT -> "Cortes opuestos en diagonal"
        AppShapeStyle.SINGLE_CUT -> "Un único corte diagonal"
        AppShapeStyle.CHEVRON -> "Laterales angulares"
        AppShapeStyle.BEVELED -> "Micro-biseles tecnológicos"
        AppShapeStyle.NOTCHED -> "Pequeña muesca lateral"
        AppShapeStyle.TICKET -> "Silueta de ticket discreta"
        AppShapeStyle.ASYMMETRIC -> "Radios alternados"
        AppShapeStyle.TOP_ROUNDED -> "Arriba redondeado, abajo recto"
    }
