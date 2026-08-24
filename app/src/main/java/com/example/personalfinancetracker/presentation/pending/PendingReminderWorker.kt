package com.example.personalfinancetracker.presentation.pending

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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.personalfinancetracker.MainActivity
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.pendingReminderInstant
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class PendingReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val entryId = inputData.getLong(KEY_ENTRY_ID, 0)
        val expectedAt = inputData.getLong(KEY_EXPECTED_AT, 0)
        if (entryId == 0L || expectedAt == 0L) return@runCatching

        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            PendingReminderEntryPoint::class.java,
        ).pendingEntries()
        val entry = repository.get(entryId) ?: return@runCatching
        val reminderTime = entry.reminderTime ?: return@runCatching
        if (entry.isDone || pendingReminderInstant(entry.date, reminderTime).toEpochMilli() != expectedAt) {
            return@runCatching
        }
        showNotification(entry)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )

    private fun showNotification(entry: PendingEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        PendingReminderScheduler.createNotificationChannel(applicationContext)
        val openPending = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINATION, "pending")
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            entry.id.hashCode(),
            openPending,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val action = if (entry.type == PendingType.PAYMENT) "Recordatorio de pago" else "Recordatorio de cobro"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$action para hoy")
            .setContentText("${entry.description} · ${MoneyFormatter.format(entry.amountInCents)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                listOfNotNull(entry.description, MoneyFormatter.format(entry.amountInCents), entry.comment)
                    .joinToString(" · ")
            ))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(entry.id.hashCode(), notification)
    }

    companion object {
        const val KEY_ENTRY_ID = "pending_entry_id"
        const val KEY_EXPECTED_AT = "pending_expected_at"
        const val CHANNEL_ID = "pending_reminders"
    }
}

object PendingReminderScheduler {
    fun schedule(context: Context, entry: PendingEntry) {
        val reminderTime = entry.reminderTime
        if (entry.id == 0L || entry.isDone || reminderTime == null) {
            cancel(context, entry.id)
            return
        }
        val reminderAt = pendingReminderInstant(entry.date, reminderTime)
        val delayMillis = Duration.between(Instant.now(), reminderAt).toMillis()
        if (delayMillis <= 0) {
            cancel(context, entry.id)
            return
        }
        val request = OneTimeWorkRequestBuilder<PendingReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder()
                .putLong(PendingReminderWorker.KEY_ENTRY_ID, entry.id)
                .putLong(PendingReminderWorker.KEY_EXPECTED_AT, reminderAt.toEpochMilli())
                .build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(entry.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, entryId: Long) {
        if (entryId != 0L) WorkManager.getInstance(context).cancelUniqueWork(workName(entryId))
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            PendingReminderWorker.CHANNEL_ID,
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alertas de pagos y cobros programados"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun workName(entryId: Long) = "pending-reminder-$entryId"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PendingReminderEntryPoint {
    fun pendingEntries(): PendingEntryRepository
}
