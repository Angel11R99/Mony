package com.angel.mony.presentation.fixed

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.angel.mony.domain.model.FixedScheduleMode
import com.angel.mony.domain.model.calculateNextRun
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class FixedEntryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            FixedWorkerEntryPoint::class.java,
        ).fixedEntries()
        val now = Instant.now()
        val dueEntries = repository.observeAll().first().filter { entry ->
            entry.isActive && entry.scheduleMode != FixedScheduleMode.MANUAL &&
                entry.nextRunAt?.let { it <= now } == true
        }
        dueEntries.forEach { entry ->
            val scheduledAt = entry.nextRunAt ?: return@forEach
            val postingDate = scheduledAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val oneTime = entry.scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME
            val nextRun = if (oneTime) null else calculateNextRun(
                mode = entry.scheduleMode,
                hour = entry.scheduleHour,
                specificDate = entry.scheduleSpecificDate,
                after = now.plusSeconds(1),
            )
            repository.post(
                entry = entry.copy(
                    isActive = if (oneTime) false else entry.isActive,
                    nextRunAt = nextRun,
                    lastAddedAt = now,
                    lastAddedDate = postingDate,
                ),
                transaction = entry.toTransaction(postingDate, now),
            )
        }
        if (dueEntries.isNotEmpty()) updateAllFinanceWidgets(applicationContext)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}

object FixedEntryScheduler {
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<FixedEntryWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private const val WORK_NAME = "fixed-entry-scheduler"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FixedWorkerEntryPoint {
    fun fixedEntries(): FixedEntryRepository
}
