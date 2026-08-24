package com.example.personalfinancetracker.core

import android.content.Context
import com.example.personalfinancetracker.domain.model.PendingCardSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PendingDisplayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableCardSize = MutableStateFlow(
        preferences.getString(KEY_CARD_SIZE, null)
            ?.let { runCatching { PendingCardSize.valueOf(it) }.getOrNull() }
            ?: PendingCardSize.NORMAL,
    )
    val cardSize: StateFlow<PendingCardSize> = mutableCardSize

    fun setCardSize(size: PendingCardSize) {
        preferences.edit().putString(KEY_CARD_SIZE, size.name).apply()
        mutableCardSize.value = size
    }

    private companion object {
        const val PREFERENCES_NAME = "pending_display_preferences"
        const val KEY_CARD_SIZE = "card_size"
    }
}
