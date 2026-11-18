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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
            Result.success(androidx.work.workDataOf(KEY_PROGRESS to 100))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(androidx.work.workDataOf(KEY_ERROR to failure.userMessage()))
        }
    }

    private fun Exception.userMessage(): String {
        val detail = message.orEmpty()
        return when {
            detail.contains("espacio", ignoreCase = true) -> detail
            detail.contains("incompleta", ignoreCase = true) || detail.contains("verificación", ignoreCase = true) -> detail
            detail.contains("servidor", ignoreCase = true) || detail.contains("archivo del modelo", ignoreCase = true) -> detail
            this is UnknownHostException -> "No se encontró el servidor de descarga. Comprueba tu conexión a Internet e inténtalo de nuevo."
            this is SocketTimeoutException -> "La descarga tardó demasiado y se interrumpió. Comprueba tu conexión y reanúdala."
            this is IOException -> "Se perdió la conexión durante la descarga. Comprueba Internet y reanúdala."
            detail.startsWith("No se pudo", ignoreCase = true) || detail.startsWith("Libera", ignoreCase = true) -> detail
            else -> "Ocurrió un error al descargar el modelo. Comprueba tu conexión y el espacio disponible; puedes reanudar la descarga."
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
        const val KEY_ERROR = "download_error"
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
