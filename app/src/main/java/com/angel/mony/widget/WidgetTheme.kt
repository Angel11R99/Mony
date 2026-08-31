package com.angel.mony.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import com.angel.mony.R
import com.angel.mony.ui.theme.AppearancePreferences
import com.angel.mony.ui.theme.AppThemeMode

data class WidgetTheme(
    val dark: Boolean,
    val background: ImageProvider,
    val primaryColor: Color,
    val primary: ColorProvider,
    val accent: ColorProvider,
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val track: ColorProvider,
    val chipSurface: ColorProvider,
) {
    /** Readable content color drawn on top of [primary]. */
    fun onPrimary(): ColorProvider =
        ColorProvider(if (primaryColor.luminance() > 0.48f) Color(0xFF121016) else Color.White)

    companion object {
        fun of(context: Context): WidgetTheme {
            val appearance = AppearancePreferences.load(context)
            val systemDark = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val dark = when (appearance.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val primary = Color(appearance.primaryArgb)
            val accent = Color(appearance.accentArgb)
            return WidgetTheme(
                dark = dark,
                background = ImageProvider(
                    if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light,
                ),
                primaryColor = primary,
                primary = ColorProvider(primary),
                accent = ColorProvider(accent),
                primaryText = ColorProvider(if (dark) Color(0xFFF7F4EF) else Color(0xFF1C1722)),
                secondaryText = ColorProvider(if (dark) Color(0xFFD1CED3) else Color(0xFF6D6574)),
                track = ColorProvider(if (dark) Color(0xFF464148) else Color(0xFFE6E1E8)),
                chipSurface = ColorProvider(if (dark) Color(0xFF37333C) else Color(0xFFF1EDF4)),
            )
        }
    }
}
