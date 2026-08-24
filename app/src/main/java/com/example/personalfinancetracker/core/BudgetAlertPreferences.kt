package com.example.personalfinancetracker.core

import android.content.Context
import com.example.personalfinancetracker.domain.model.BudgetAlertLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BudgetAlertPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableAlertsEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_ALERTS_ENABLED, true),
    )
    val alertsEnabled: StateFlow<Boolean> = mutableAlertsEnabled

    fun setAlertsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        mutableAlertsEnabled.value = enabled
    }

    fun lastLevel(cycleStartEpochDay: Long): BudgetAlertLevel {
        val ordinal = preferences.getInt(levelKey(cycleStartEpochDay), 0)
        return BudgetAlertLevel.entries.getOrNull(ordinal) ?: BudgetAlertLevel.NONE
    }

    fun saveLevel(cycleStartEpochDay: Long, level: BudgetAlertLevel) {
        val key = levelKey(cycleStartEpochDay)
        val editor = preferences.edit().putInt(key, level.ordinal)
        preferences.all.keys
            .filter { it.startsWith(KEY_LEVEL_PREFIX) && it != key }
            .forEach(editor::remove)
        editor.apply()
    }

    private fun levelKey(cycleStartEpochDay: Long) = "$KEY_LEVEL_PREFIX$cycleStartEpochDay"

    private companion object {
        const val PREFERENCES_NAME = "budget_alert_preferences"
        const val KEY_ALERTS_ENABLED = "alerts_enabled"
        const val KEY_LEVEL_PREFIX = "alert_level_"
    }
}
