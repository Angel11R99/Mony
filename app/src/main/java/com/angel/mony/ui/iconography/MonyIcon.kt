package com.angel.mony.ui.iconography

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
