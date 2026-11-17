package com.angel.mony.data.nlp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.angel.mony.R
import com.angel.mony.domain.model.LocalLanguageModelManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

class LanguageModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val manager = EntryPointAccessors.fromApplication(applicationContext, LanguageModelWorkerEntryPoint::class.java)
            .modelManager()
        val model = manager.models.firstOrNull { it.id == modelId } ?: return Result.failure()
        return try {
            setForeground(foregroundInfo(model.name, 0))
            var lastProgress = -1
            manager.downloadModel(modelId) { percent ->
                if (percent != lastProgress) {
                    lastProgress = percent
                    setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to percent)).get()
                    setForegroundAsync(foregroundInfo(model.name, percent))
                }
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun foregroundInfo(name: String, progress: Int): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Descargas de modelos", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Descargando $name")
            .setContentText("$progress% · Puedes seguir usando Mony")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"
        const val CHANNEL_ID = "language_model_downloads"
        const val NOTIFICATION_ID = 7142
        fun uniqueWorkName(modelId: String) = "language-model-download-$modelId"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LanguageModelWorkerEntryPoint {
    fun modelManager(): LocalLanguageModelManager
}
