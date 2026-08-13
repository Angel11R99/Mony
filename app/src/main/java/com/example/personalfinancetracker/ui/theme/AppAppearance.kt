package com.example.personalfinancetracker.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class AppAppearance(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val primaryArgb: Int = DEFAULT_PRIMARY_ARGB,
    val accentArgb: Int = DEFAULT_ACCENT_ARGB,
)

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read(preferences))
    val settings: StateFlow<AppAppearance> = mutableSettings

    fun setThemeMode(mode: AppThemeMode) = update(mutableSettings.value.copy(themeMode = mode))
    fun setPrimaryColor(argb: Int) = update(mutableSettings.value.copy(primaryArgb = argb or OPAQUE_ALPHA))
    fun setAccentColor(argb: Int) = update(mutableSettings.value.copy(accentArgb = argb or OPAQUE_ALPHA))
    fun reset() = update(AppAppearance())

    private fun update(value: AppAppearance) {
        preferences.edit()
            .putString(KEY_THEME, value.themeMode.name)
            .putLong(KEY_PRIMARY, value.primaryArgb.toLong())
            .putLong(KEY_ACCENT, value.accentArgb.toLong())
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
