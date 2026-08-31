package com.angel.mony.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class AppAppearance(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val primaryArgb: Int = DEFAULT_PRIMARY_ARGB,
    val accentArgb: Int = DEFAULT_ACCENT_ARGB,
    val shapeStyle: AppShapeStyle = AppShapeStyle.CUT,
    val fontFamily: AppFontFamily = AppFontFamily.SYSTEM,
)

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read(preferences))
    val settings: StateFlow<AppAppearance> = mutableSettings

    fun setThemeMode(mode: AppThemeMode) = update(mutableSettings.value.copy(themeMode = mode))

    /**
     * Cambia el modo de tema y, si el color principal o secundario actual no tiene
     * suficiente contraste con el tema destino, lo reemplaza por un preset compatible
     * elegido de forma aleatoria. Evita que colores como blanco en tema claro
     * queden invisibles tras el cambio.
     */
    fun setThemeModeWithAutoCorrection(mode: AppThemeMode, isDarkTheme: Boolean): AutoCorrectionResult {
        val current = mutableSettings.value
        var newPrimary = current.primaryArgb
        var newAccent = current.accentArgb
        var primaryChanged = false
        var accentChanged = false
        if (!isColorCompatible(newPrimary, isDarkTheme)) {
            newPrimary = randomCompatiblePreset(primaryPresets, isDarkTheme)
            primaryChanged = true
        }
        if (!isColorCompatible(newAccent, isDarkTheme)) {
            newAccent = randomCompatibleAccentPreset(isDarkTheme)
            accentChanged = true
        }
        val newAppearance = current.copy(
            themeMode = mode,
            primaryArgb = newPrimary or OPAQUE_ALPHA,
            accentArgb = newAccent or OPAQUE_ALPHA,
        )
        update(newAppearance)
        return AutoCorrectionResult(primaryChanged, accentChanged, newAppearance)
    }

    fun ensureColorsCompatible(isDarkTheme: Boolean): AutoCorrectionResult? {
        val current = mutableSettings.value
        var newPrimary = current.primaryArgb
        var newAccent = current.accentArgb
        var primaryChanged = false
        var accentChanged = false
        if (!isColorCompatible(newPrimary, isDarkTheme)) {
            newPrimary = randomCompatiblePreset(primaryPresets, isDarkTheme)
            primaryChanged = true
        }
        if (!isColorCompatible(newAccent, isDarkTheme)) {
            newAccent = randomCompatibleAccentPreset(isDarkTheme)
            accentChanged = true
        }
        if (!primaryChanged && !accentChanged) return null
        val newAppearance = current.copy(
            primaryArgb = newPrimary or OPAQUE_ALPHA,
            accentArgb = newAccent or OPAQUE_ALPHA,
        )
        update(newAppearance)
        return AutoCorrectionResult(primaryChanged, accentChanged, newAppearance)
    }

    fun setPrimaryColor(argb: Int) = update(mutableSettings.value.copy(primaryArgb = argb or OPAQUE_ALPHA))
    fun setAccentColor(argb: Int) = update(mutableSettings.value.copy(accentArgb = argb or OPAQUE_ALPHA))
    fun setShapeStyle(style: AppShapeStyle) = update(mutableSettings.value.copy(shapeStyle = style))
    fun setFontFamily(family: AppFontFamily) = update(mutableSettings.value.copy(fontFamily = family))
    fun reset() = update(AppAppearance())

    private fun update(value: AppAppearance) {
        preferences.edit()
            .putString(KEY_THEME, value.themeMode.name)
            .putLong(KEY_PRIMARY, value.primaryArgb.toLong())
            .putLong(KEY_ACCENT, value.accentArgb.toLong())
            .putString(KEY_SHAPE, value.shapeStyle.name)
            .putString(KEY_FONT, value.fontFamily.name)
            .apply()
        mutableSettings.value = value
    }

    companion object {
        fun load(context: Context): AppAppearance = read(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )

        private fun read(preferences: android.content.SharedPreferences) = AppAppearance(
            themeMode = preferences.getString(KEY_THEME, null)
                ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            primaryArgb = preferences.getLong(KEY_PRIMARY, DEFAULT_PRIMARY_ARGB.toLong()).toInt(),
            accentArgb = preferences.getLong(KEY_ACCENT, DEFAULT_ACCENT_ARGB.toLong()).toInt(),
            shapeStyle = preferences.getString(KEY_SHAPE, null)
                ?.let { runCatching { AppShapeStyle.valueOf(it) }.getOrNull() }
                ?: AppShapeStyle.CUT,
            fontFamily = preferences.getString(KEY_FONT, null)
                ?.let { runCatching { AppFontFamily.valueOf(it) }.getOrNull() }
                ?: AppFontFamily.SYSTEM,
        )
    }
}

internal const val DEFAULT_PRIMARY_ARGB: Int = 0xFF7C3AED.toInt()
internal const val DEFAULT_ACCENT_ARGB: Int = 0xFFFF6B73.toInt()
private const val OPAQUE_ALPHA: Int = 0xFF000000.toInt()
private const val PREFERENCES_NAME = "appearance_preferences"
private const val KEY_THEME = "theme_mode"
private const val KEY_PRIMARY = "primary_color"
private const val KEY_ACCENT = "accent_color"
private const val KEY_SHAPE = "shape_style"
private const val KEY_FONT = "font_family"

const val DARK_INCOMPATIBLE_LUMINANCE_THRESHOLD: Float = 0.18f
const val LIGHT_INCOMPATIBLE_LUMINANCE_THRESHOLD: Float = 0.65f

fun isColorCompatible(argb: Int, isDarkTheme: Boolean): Boolean {
    val luminance = Color(argb).luminance()
    return if (isDarkTheme) luminance >= DARK_INCOMPATIBLE_LUMINANCE_THRESHOLD
    else luminance <= LIGHT_INCOMPATIBLE_LUMINANCE_THRESHOLD
}

val primaryPresets: List<Int> = listOf(
    0xFF7C3AED.toInt(), 0xFF2563EB.toInt(), 0xFF0891B2.toInt(), 0xFF059669.toInt(),
    0xFFCA8A04.toInt(), 0xFFEA580C.toInt(), 0xFFDB2777.toInt(), 0xFF52525B.toInt(),
    0xFFFFFFFF.toInt(), 0xFF78350F.toInt(),
)

val accentPresets: List<Int> = listOf(
    0xFFFF6B73.toInt(), 0xFFDC2626.toInt(), 0xFFF97316.toInt(), 0xFFDB2777.toInt(),
    0xFF9333EA.toInt(), 0xFF2563EB.toInt(), 0xFF0D9488.toInt(), 0xFF52525B.toInt(),
    0xFFFFFFFF.toInt(), 0xFF65A30D.toInt(),
)

fun randomCompatiblePreset(presets: List<Int>, isDarkTheme: Boolean): Int {
    val compatible = presets.filter { isColorCompatible(it, isDarkTheme) }
    if (compatible.isNotEmpty()) return compatible.random()
    return if (isDarkTheme) DEFAULT_PRIMARY_ARGB else DEFAULT_PRIMARY_ARGB
}

fun randomCompatibleAccentPreset(isDarkTheme: Boolean): Int {
    val compatible = accentPresets.filter { isColorCompatible(it, isDarkTheme) }
    if (compatible.isNotEmpty()) return compatible.random()
    return DEFAULT_ACCENT_ARGB
}

data class AutoCorrectionResult(
    val primaryChanged: Boolean,
    val accentChanged: Boolean,
    val newAppearance: AppAppearance,
) {
    val anyChanged: Boolean get() = primaryChanged || accentChanged
}
