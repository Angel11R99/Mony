package com.example.personalfinancetracker.core

import android.content.Context
import com.example.personalfinancetracker.domain.model.BudgetPeriodView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

class CyclePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableAutomaticClose = MutableStateFlow(
        preferences.getBoolean(KEY_AUTOMATIC_CLOSE, false),
    )
    private val mutableAutomaticCloseTime = MutableStateFlow(
        LocalTime.ofSecondOfDay(preferences.getInt(KEY_AUTOMATIC_CLOSE_TIME, DEFAULT_CLOSE_MINUTES) * 60L)
    )
    private val mutablePinnedBudgetView = MutableStateFlow(
        preferences.getString(KEY_PINNED_BUDGET_VIEW, null)
            ?.let { runCatching { BudgetPeriodView.valueOf(it) }.getOrNull() }
            ?: BudgetPeriodView.CURRENT,
    )
    val automaticClose: StateFlow<Boolean> = mutableAutomaticClose
    val automaticCloseTime: StateFlow<LocalTime> = mutableAutomaticCloseTime
    val pinnedBudgetView: StateFlow<BudgetPeriodView> = mutablePinnedBudgetView

    fun setAutomaticClose(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC_CLOSE, enabled).apply()
        mutableAutomaticClose.value = enabled
    }

    fun setAutomaticCloseTime(time: LocalTime) {
        preferences.edit().putInt(KEY_AUTOMATIC_CLOSE_TIME, time.toSecondOfDay() / 60).apply()
        mutableAutomaticCloseTime.value = time
    }

    fun setPinnedBudgetView(view: BudgetPeriodView) {
        preferences.edit().putString(KEY_PINNED_BUDGET_VIEW, view.name).apply()
        mutablePinnedBudgetView.value = view
    }

    private companion object {
        const val PREFERENCES_NAME = "cycle_preferences"
        const val KEY_AUTOMATIC_CLOSE = "automatic_close"
        const val KEY_AUTOMATIC_CLOSE_TIME = "automatic_close_time"
        const val KEY_PINNED_BUDGET_VIEW = "pinned_budget_view"
        const val DEFAULT_CLOSE_MINUTES = 21 * 60
    }
}
