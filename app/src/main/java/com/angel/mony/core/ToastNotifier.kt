package com.angel.mony.core

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AppToastTone {
    SUCCESS,
    ERROR,
    INFO,
}

data class AppToastMessage(
    val text: String,
    val tone: AppToastTone,
    val durationMillis: Long,
)

object AppToastNotifications {
    private val mutableMessages = MutableSharedFlow<AppToastMessage>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages = mutableMessages.asSharedFlow()

    fun show(context: Context, message: String, durationMillis: Long) {
        val notification = AppToastMessage(
            text = message,
            tone = toastToneFor(message),
            durationMillis = durationMillis.coerceIn(1_000L, 10_000L),
        )
        if (mutableMessages.subscriptionCount.value > 0) {
            mutableMessages.tryEmit(notification)
        } else {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.showToast(message: String, durationMillis: Long = 3_000L) =
    AppToastNotifications.show(this, message, durationMillis)

internal fun toastToneFor(message: String): AppToastTone {
    val normalized = message.trim().lowercase()
    return when {
        normalized.startsWith("no ") ||
            "no se pudo" in normalized ||
            "error" in normalized ||
            "inválid" in normalized -> AppToastTone.ERROR
            "correct" in normalized ||
            "guardad" in normalized ||
            "registrad" in normalized ||
            "actualizad" in normalized ||
            "eliminad" in normalized ||
            "duplicad" in normalized ||
            "restaurad" in normalized ||
            "exportad" in normalized ||
            "cerrad" in normalized ||
            "fijad" in normalized -> AppToastTone.SUCCESS
        else -> AppToastTone.INFO
    }
}
