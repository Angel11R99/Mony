package com.example.personalfinancetracker.navigation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FloatingModuleBarConfig(
    val visibleRoutes: Set<String> = setOf("home", "fixed", "pending", "savings", "statistics", "history"),
    val showLabels: Boolean = true,
    val labelTextSize: Float = 10f,
)

class FloatingModuleBarPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableConfig = MutableStateFlow(read(preferences))
    val config: StateFlow<FloatingModuleBarConfig> = mutableConfig

    fun setVisibleRoutes(routes: Set<String>) {
        if (routes.isEmpty()) return
        update(mutableConfig.value.copy(visibleRoutes = routes))
    }

    fun setShowLabels(show: Boolean) = update(mutableConfig.value.copy(showLabels = show))

    fun setLabelTextSize(size: Float) = update(mutableConfig.value.copy(labelTextSize = size.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)))

    private fun update(value: FloatingModuleBarConfig) {
        preferences.edit()
            .putStringSet(KEY_VISIBLE_ROUTES, value.visibleRoutes)
            .putBoolean(KEY_SHOW_LABELS, value.showLabels)
            .putFloat(KEY_LABEL_TEXT_SIZE, value.labelTextSize)
            .apply()
        mutableConfig.value = value
    }

    companion object {
        const val MIN_TEXT_SIZE = 8f
        const val MAX_TEXT_SIZE = 14f
        private val DEFAULT_VISIBLE_ROUTES = setOf(
            "home", "fixed", "pending", "savings", "statistics", "history",
        )
        private const val PREFERENCES_NAME = "floating_module_bar_preferences"
        private const val KEY_VISIBLE_ROUTES = "visible_routes"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_LABEL_TEXT_SIZE = "label_text_size"

        fun load(context: Context): FloatingModuleBarConfig = read(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )

        private fun read(preferences: android.content.SharedPreferences) = FloatingModuleBarConfig(
            visibleRoutes = preferences.getStringSet(KEY_VISIBLE_ROUTES, null)
                ?.toSet()
                ?: DEFAULT_VISIBLE_ROUTES,
            showLabels = preferences.getBoolean(KEY_SHOW_LABELS, true),
            labelTextSize = preferences.getFloat(KEY_LABEL_TEXT_SIZE, 10f),
        )
    }
}
