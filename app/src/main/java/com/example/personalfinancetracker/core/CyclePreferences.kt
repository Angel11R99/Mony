package com.example.personalfinancetracker.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CyclePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableAutomaticClose = MutableStateFlow(
        preferences.getBoolean(KEY_AUTOMATIC_CLOSE, false),
    )
    val automaticClose: StateFlow<Boolean> = mutableAutomaticClose

    fun setAutomaticClose(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC_CLOSE, enabled).apply()
        mutableAutomaticClose.value = enabled
    }

    private companion object {
        const val PREFERENCES_NAME = "cycle_preferences"
        const val KEY_AUTOMATIC_CLOSE = "automatic_close"
    }
}
