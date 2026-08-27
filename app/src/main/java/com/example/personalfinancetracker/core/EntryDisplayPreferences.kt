package com.example.personalfinancetracker.core

import android.content.Context
import com.example.personalfinancetracker.domain.model.EntryCardSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EntryDisplayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutablePendingCardSize = MutableStateFlow(
        preferences.getString(KEY_PENDING_CARD_SIZE, null)
            ?.let { runCatching { EntryCardSize.valueOf(it) }.getOrNull() }
            ?: EntryCardSize.NORMAL,
    )
    private val mutableFixedCardSize = MutableStateFlow(
        preferences.getString(KEY_FIXED_CARD_SIZE, null)
            ?.let { runCatching { EntryCardSize.valueOf(it) }.getOrNull() }
            ?: EntryCardSize.NORMAL,
    )
    private val mutableSavingsCardSize = MutableStateFlow(
        preferences.getString(KEY_SAVINGS_CARD_SIZE, null)
            ?.let { runCatching { EntryCardSize.valueOf(it) }.getOrNull() }
            ?: EntryCardSize.NORMAL,
    )
    private val mutableListCardSize = MutableStateFlow(
        preferences.getString(KEY_LIST_CARD_SIZE, null)
            ?.let { runCatching { EntryCardSize.valueOf(it) }.getOrNull() }
            ?: EntryCardSize.NORMAL,
    )
    val pendingCardSize: StateFlow<EntryCardSize> = mutablePendingCardSize
    val fixedCardSize: StateFlow<EntryCardSize> = mutableFixedCardSize
    val savingsCardSize: StateFlow<EntryCardSize> = mutableSavingsCardSize
    val listCardSize: StateFlow<EntryCardSize> = mutableListCardSize

    fun setPendingCardSize(size: EntryCardSize) {
        preferences.edit().putString(KEY_PENDING_CARD_SIZE, size.name).apply()
        mutablePendingCardSize.value = size
    }

    fun setFixedCardSize(size: EntryCardSize) {
        preferences.edit().putString(KEY_FIXED_CARD_SIZE, size.name).apply()
        mutableFixedCardSize.value = size
    }

    fun setSavingsCardSize(size: EntryCardSize) {
        preferences.edit().putString(KEY_SAVINGS_CARD_SIZE, size.name).apply()
        mutableSavingsCardSize.value = size
    }

    fun setListCardSize(size: EntryCardSize) {
        preferences.edit().putString(KEY_LIST_CARD_SIZE, size.name).apply()
        mutableListCardSize.value = size
    }

    private companion object {
        const val PREFERENCES_NAME = "entry_display_preferences"
        const val KEY_PENDING_CARD_SIZE = "pending_card_size"
        const val KEY_FIXED_CARD_SIZE = "fixed_card_size"
        const val KEY_SAVINGS_CARD_SIZE = "savings_card_size"
        const val KEY_LIST_CARD_SIZE = "list_card_size"
    }
}
