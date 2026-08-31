package com.angel.mony.presentation.home

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.angel.mony.MainActivity
import com.angel.mony.R
import com.angel.mony.core.BudgetAlertPreferences
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.BudgetAlertEvaluator
import com.angel.mony.domain.model.BudgetAlertLevel
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.model.budgetUsagePercent
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class BudgetAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val preferences = BudgetAlertPreferences(applicationContext)
        if (!preferences.alertsEnabled.value) return@runCatching

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BudgetAlertEntryPoint::class.java,
        )
        val budget = entryPoint.budgets().observe().first() ?: return@runCatching
        val transactions = entryPoint.transactions().observeAll().first()

        val today = LocalDate.now()
        val period = activeBudgetPeriod(budget, today)
        val percent = budgetUsagePercent(budget, transactions, period)
        val currentLevel = BudgetAlertLevel.forUsagePercent(percent)
        val previousLevel = preferences.lastLevel(period.start.toEpochDay())
        val decision = BudgetAlertEvaluator.evaluate(previousLevel, currentLevel)
        preferences.saveLevel(period.start.toEpochDay(), decision.levelToStore)

        if (decision.shouldNotify) {
            showNotification(currentLevel, percent, budget.amountInCents)
        }
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )

    private fun showNotification(level: BudgetAlertLevel, percent: Int, budgetInCents: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        BudgetAlertScheduler.createNotificationChannel(applicationContext)
        val openApp = Intent(applicationContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (level == BudgetAlertLevel.EXCEEDED) "Presupuesto excedido"
        else "Ya usaste el $percent% de tu presupuesto"
        val text = if (level == BudgetAlertLevel.EXCEEDED) {
            "Cuidado: superaste tu presupuesto de ${MoneyFormatter.format(budgetInCents)}."
        } else {
            "Queda poco disponible en este ciclo. Revisa tus gastos."
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        private const val REQUEST_CODE = 4001
        private const val NOTIFICATION_ID = 4001
    }
}

object BudgetAlertScheduler {
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            BudgetAlertWorker.CHANNEL_ID,
            "Alertas de presupuesto",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos al acercarte o superar tu presupuesto del ciclo"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private const val WORK_NAME = "budget-alert-scheduler"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetAlertEntryPoint {
    fun budgets(): BudgetRepository
    fun transactions(): TransactionRepository
}
