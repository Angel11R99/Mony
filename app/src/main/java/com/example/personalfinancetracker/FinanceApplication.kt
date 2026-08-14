package com.example.personalfinancetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.personalfinancetracker.presentation.fixed.FixedEntryScheduler
import com.example.personalfinancetracker.presentation.pending.PendingReminderScheduler

@HiltAndroidApp
class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FixedEntryScheduler.ensureScheduled(this)
        PendingReminderScheduler.createNotificationChannel(this)
    }
}
