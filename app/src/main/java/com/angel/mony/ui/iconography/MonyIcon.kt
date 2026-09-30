package com.angel.mony.ui.iconography

import androidx.compose.ui.graphics.Color
import com.angel.mony.ui.iconography.vendor.iconpark.IconParkPalette

/** Meanings requested by Mony's UI, independent from the selected graphic library. */
enum class MonyIcon {
    Home,
    Fixed,
    Pending,
    Savings,
    Shopping,
    Statistics,
    Fortnight,
    History,
    Settings,
    Appearance,
    Navigation,
    Finance,
    Add,
    Edit,
    Delete,
    Back,
    Search,
    More,
    Check,
    Close,
    Warning,
    Info,
    Category,
    Completed,
    Expense,
    Income,
    Food,
    Debt,
    Education,
    Emergency,
    Entertainment,
    Family,
    Internet,
    Health,
    Services,
    Subscription,
    Phone,
    Transport,
    Housing,
    Other,
    Calendar,
    Lock,
    Unlock,
    Template,
    Save,
    Filter,
    Download,
    Upload,
    Share,
    Copy,
    Undo,
    Pin,
    Previous,
    Next,
    Error,
    Dropdown,
    ExpandMore,
    ExpandLess,
    Remove,
    Sort,
    Time,
    Notes,
    ListView,
    AlertsEnabled,
    AlertsDisabled,
    Reopen,
    Restore,
    ScanBarcode,
    ScanDocument,
    ScanPrice,
    TrendUp,
    TrendDown,
    TrendFlat,
}

enum class IconPack(val displayName: String) {
    MATERIAL("Material"),
    LUCIDE("Lucide"),
    PHOSPHOR("Phosphor"),
    MONY_COLOR("Mony Color"),
}

enum class IconColorMode(val displayName: String) {
    AUTOMATIC("Automático"),
    PRIMARY("Color principal"),
    CUSTOM("Personalizado"),
}

/**
 * IconPark theme for the Mony Color pack.
 *
 * IconPark authors every glyph once and derives its four official themes by remapping four colour
 * slots, so selecting a style only swaps the palette and never rebuilds the geometry. The order
 * matches IconPark's own runtime and the accents are its brand colours.
 */
enum class MonyColorStyle(val displayName: String) {
    MULTI_COLOR("Multicolor"),
    TWO_TONE("Bicolor"),
    OUTLINE("Contorno"),
    FILLED("Sólido"),
    ;

    internal companion object {
        /** IconPark's #2F88FF, used as the secondary slot in the two-tone and multi-color themes. */
        val Accent: Color = Color(0xFF2F88FF)

        /** IconPark's #43CCF8, used only as the fourth slot of the multi-color theme. */
        val Highlight: Color = Color(0xFF43CCF8)
    }
}

/**
 * Resolves this theme against the colour scheme.
 *
 * @param ink the themed outline colour, normally `onSurface`.
 * @param knockout the colour that cuts detail out of a solid body. It must match the surface the
 *   icon is drawn on, otherwise the filled theme renders a white-on-white body in dark mode.
 */
fun MonyColorStyle.palette(
    ink: Color,
    knockout: Color = Color.White,
): IconParkPalette = when (this) {
    MonyColorStyle.MULTI_COLOR -> IconParkPalette(
        outerStroke = ink,
        outerFill = MonyColorStyle.Accent,
        innerStroke = Color.White,
        innerFill = MonyColorStyle.Highlight,
    )
    MonyColorStyle.TWO_TONE -> IconParkPalette(
        outerStroke = ink,
        outerFill = MonyColorStyle.Accent,
        innerStroke = ink,
        innerFill = MonyColorStyle.Accent,
    )
    MonyColorStyle.OUTLINE -> IconParkPalette(
        outerStroke = ink,
        outerFill = null,
        innerStroke = ink,
        innerFill = null,
    )
    MonyColorStyle.FILLED -> IconParkPalette(
        outerStroke = ink,
        outerFill = ink,
        innerStroke = knockout,
        innerFill = knockout,
    )
}

enum class MonyIconRole {
    NORMAL,
    STATE,
}

fun semanticIconForCategory(iconKey: String, categoryName: String = ""): MonyIcon {
    val key = iconKey.lowercase()
    return when {
        key == "payments" || categoryName.equals("Salario", true) -> MonyIcon.Income
        key == "work" || key == "laptop" -> MonyIcon.Finance
        key == "sell" || key == "add_circle" -> MonyIcon.Income
        key == "restaurant" -> MonyIcon.Food
        key == "directions_car" -> MonyIcon.Transport
        key == "home" -> MonyIcon.Housing
        key == "receipt_long" -> MonyIcon.Services
        key == "wifi" -> MonyIcon.Internet
        key == "phone_android" -> MonyIcon.Phone
        key == "medical_services" -> MonyIcon.Health
        key == "school" -> MonyIcon.Education
        key == "movie" -> MonyIcon.Entertainment
        key == "shopping_cart" -> MonyIcon.Shopping
        key == "credit_card" -> MonyIcon.Debt
        key == "subscriptions" -> MonyIcon.Subscription
        key == "family_restroom" -> MonyIcon.Family
        key == "savings" -> MonyIcon.Savings
        key == "emergency" -> MonyIcon.Emergency
        else -> MonyIcon.Other
    }
}
