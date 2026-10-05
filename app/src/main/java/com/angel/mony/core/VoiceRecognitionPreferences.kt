package com.angel.mony.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoiceRecognitionPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSkipConventionalNotice = MutableStateFlow(
        preferences.getBoolean(KEY_SKIP_CONVENTIONAL_NOTICE, false),
    )
    val skipConventionalNotice: StateFlow<Boolean> = mutableSkipConventionalNotice.asStateFlow()

    fun setSkipConventionalNotice(skip: Boolean) {
        preferences.edit().putBoolean(KEY_SKIP_CONVENTIONAL_NOTICE, skip).apply()
        mutableSkipConventionalNotice.value = skip
    }

    fun widgetExampleRemainingKeys(): List<String> =
        preferences.getString(KEY_WIDGET_EXAMPLE_REMAINING, null)
            ?.split(KEY_SEPARATOR)
            ?.filter(String::isNotBlank)
            .orEmpty()

    fun widgetExampleLastKey(): String? =
        preferences.getString(KEY_WIDGET_EXAMPLE_LAST, null)

    fun saveWidgetExampleRotation(remainingKeys: List<String>, lastKey: String?) {
        preferences.edit()
            .putString(KEY_WIDGET_EXAMPLE_REMAINING, remainingKeys.joinToString(KEY_SEPARATOR))
            .putString(KEY_WIDGET_EXAMPLE_LAST, lastKey)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "voice_recognition_preferences"
        const val KEY_SKIP_CONVENTIONAL_NOTICE = "skip_conventional_notice"
        const val KEY_WIDGET_EXAMPLE_REMAINING = "widget_example_remaining"
        const val KEY_WIDGET_EXAMPLE_LAST = "widget_example_last"
        const val KEY_SEPARATOR = ","
    }
}
