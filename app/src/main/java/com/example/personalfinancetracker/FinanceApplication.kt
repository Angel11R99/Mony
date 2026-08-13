package com.example.personalfinancetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.personalfinancetracker.presentation.fixed.FixedEntryScheduler

@HiltAndroidApp
class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FixedEntryScheduler.ensureScheduled(this)
    }
}
