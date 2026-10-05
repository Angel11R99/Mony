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

    private companion object {
        const val PREFERENCES_NAME = "voice_recognition_preferences"
        const val KEY_SKIP_CONVENTIONAL_NOTICE = "skip_conventional_notice"
    }
}
