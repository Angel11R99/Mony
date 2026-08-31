package com.angel.mony

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.angel.mony.presentation.fixed.FixedEntryScheduler
import com.angel.mony.presentation.home.BudgetAlertScheduler
import com.angel.mony.presentation.pending.PendingReminderScheduler
import com.angel.mony.widget.WidgetRefreshScheduler

@HiltAndroidApp
class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FixedEntryScheduler.ensureScheduled(this)
        BudgetAlertScheduler.ensureScheduled(this)
        PendingReminderScheduler.createNotificationChannel(this)
        BudgetAlertScheduler.createNotificationChannel(this)
        WidgetRefreshScheduler.ensureScheduled(this)
    }
}
