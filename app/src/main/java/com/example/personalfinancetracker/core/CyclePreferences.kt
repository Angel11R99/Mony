package com.example.personalfinancetracker.core

import android.content.Context
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
    val automaticClose: StateFlow<Boolean> = mutableAutomaticClose
    val automaticCloseTime: StateFlow<LocalTime> = mutableAutomaticCloseTime

    fun setAutomaticClose(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC_CLOSE, enabled).apply()
        mutableAutomaticClose.value = enabled
    }

    fun setAutomaticCloseTime(time: LocalTime) {
        preferences.edit().putInt(KEY_AUTOMATIC_CLOSE_TIME, time.toSecondOfDay() / 60).apply()
        mutableAutomaticCloseTime.value = time
    }

    private companion object {
        const val PREFERENCES_NAME = "cycle_preferences"
        const val KEY_AUTOMATIC_CLOSE = "automatic_close"
        const val KEY_AUTOMATIC_CLOSE_TIME = "automatic_close_time"
        const val DEFAULT_CLOSE_MINUTES = 21 * 60
    }
}
