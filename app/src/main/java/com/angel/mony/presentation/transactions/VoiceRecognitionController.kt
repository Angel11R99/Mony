package com.angel.mony.presentation.transactions

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

enum class VoiceRecognitionMode { ON_DEVICE, CONVENTIONAL, UNAVAILABLE }

sealed interface VoiceRecognitionEvent {
    data object Listening : VoiceRecognitionEvent
    data object Processing : VoiceRecognitionEvent
    data class Result(val transcript: String) : VoiceRecognitionEvent
    data class Error(val message: String) : VoiceRecognitionEvent
    data object ConventionalRecognitionRequired : VoiceRecognitionEvent
    data object Stopped : VoiceRecognitionEvent
}

/** Owns one SpeechRecognizer at a time. All methods are called from Compose on Android's main thread. */
class VoiceRecognitionController(
    private val context: Context,
    private val onEvent: (VoiceRecognitionEvent) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private val sessionGate = VoiceSessionGate()

    fun availableMode(): VoiceRecognitionMode = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) ->
            VoiceRecognitionMode.ON_DEVICE
        SpeechRecognizer.isRecognitionAvailable(context) -> VoiceRecognitionMode.CONVENTIONAL
        else -> VoiceRecognitionMode.UNAVAILABLE
    }

    fun start(mode: VoiceRecognitionMode = availableMode()) {
        invalidateRecognizer()
        if (mode == VoiceRecognitionMode.UNAVAILABLE) {
            onEvent(VoiceRecognitionEvent.Error("El reconocimiento de voz no está disponible en este dispositivo."))
            return
        }
        val sessionId = sessionGate.open()
        val speechRecognizer = if (mode == VoiceRecognitionMode.ON_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = speechRecognizer
        speechRecognizer.setRecognitionListener(createListener(sessionId, mode, speechRecognizer))
        val supportIntent = recognitionIntent(mode, GENERIC_SPANISH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            speechRecognizer.checkRecognitionSupport(supportIntent, context.mainExecutor, object : RecognitionSupportCallback {
                override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                    if (!isCurrent(sessionId, speechRecognizer)) return
                    val availableLanguages = if (mode == VoiceRecognitionMode.ON_DEVICE) {
                        recognitionSupport.installedOnDeviceLanguages
                    } else {
                        recognitionSupport.onlineLanguages + recognitionSupport.installedOnDeviceLanguages
                    }
                    val spanishLanguage = preferredSpanishLanguage(availableLanguages)
                    if (availableLanguages.isNotEmpty() && spanishLanguage == null) {
                        finishSession(sessionId, speechRecognizer)
                        if (mode == VoiceRecognitionMode.ON_DEVICE && SpeechRecognizer.isRecognitionAvailable(context)) {
                            onEvent(VoiceRecognitionEvent.ConventionalRecognitionRequired)
                        } else {
                            onEvent(VoiceRecognitionEvent.Error("El servicio de voz instalado no ofrece reconocimiento en español."))
                        }
                    } else {
                        speechRecognizer.startListening(recognitionIntent(mode, spanishLanguage ?: GENERIC_SPANISH))
                    }
                }

                override fun onError(error: Int) {
                    if (!isCurrent(sessionId, speechRecognizer)) return
                    // Some recognition services do not publish a language catalogue. In that case
                    // start normally and let the listener report LANGUAGE_UNAVAILABLE precisely.
                    speechRecognizer.startListening(supportIntent)
                }
            })
        } else {
            speechRecognizer.startListening(supportIntent)
        }
    }

    fun stop() {
        val current = recognizer
        if (current == null) onEvent(VoiceRecognitionEvent.Stopped) else current.stopListening()
    }

    fun cancel() {
        val current = recognizer
        sessionGate.invalidate()
        recognizer = null
        current?.cancel()
        current?.destroy()
        onEvent(VoiceRecognitionEvent.Stopped)
    }

    fun release() {
        val current = recognizer
        sessionGate.invalidate()
        recognizer = null
        current?.cancel()
        current?.destroy()
    }

    private fun recognitionIntent(mode: VoiceRecognitionMode, language: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        if (mode == VoiceRecognitionMode.ON_DEVICE) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    private fun invalidateRecognizer() {
        val current = recognizer
        sessionGate.invalidate()
        recognizer = null
        current?.cancel()
        current?.destroy()
    }

    private fun isCurrent(sessionId: Long, speechRecognizer: SpeechRecognizer): Boolean =
        sessionGate.isCurrent(sessionId) && recognizer === speechRecognizer

    private fun finishSession(sessionId: Long, speechRecognizer: SpeechRecognizer) {
        if (!isCurrent(sessionId, speechRecognizer)) return
        sessionGate.invalidate()
        recognizer = null
        speechRecognizer.destroy()
    }

    private fun isSpanish(languageTag: String): Boolean =
        languageTag.equals("es", ignoreCase = true) || languageTag.startsWith("es-", ignoreCase = true)

    private fun preferredSpanishLanguage(languages: List<String>): String? =
        languages.firstOrNull { it.equals(DOMINICAN_SPANISH, ignoreCase = true) }
            ?: languages.firstOrNull(::isSpanish)

    private fun createListener(
        sessionId: Long,
        mode: VoiceRecognitionMode,
        speechRecognizer: SpeechRecognizer,
    ) = object : RecognitionListener {
        private var deliveredFinalResult = false

        override fun onReadyForSpeech(params: Bundle?) {
            if (isCurrent(sessionId, speechRecognizer)) onEvent(VoiceRecognitionEvent.Listening)
        }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            if (isCurrent(sessionId, speechRecognizer)) onEvent(VoiceRecognitionEvent.Processing)
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onResults(results: Bundle?) {
            if (!isCurrent(sessionId, speechRecognizer) || deliveredFinalResult) return
            deliveredFinalResult = true
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (text.isNullOrBlank()) {
                onEvent(VoiceRecognitionEvent.Error("No se reconoció ninguna frase. Inténtalo nuevamente."))
            } else {
                onEvent(VoiceRecognitionEvent.Result(text))
            }
            finishSession(sessionId, speechRecognizer)
        }

        override fun onError(error: Int) {
            if (!isCurrent(sessionId, speechRecognizer)) return
            if (deliveredFinalResult && error == SpeechRecognizer.ERROR_CLIENT) return
            if (
                mode == VoiceRecognitionMode.ON_DEVICE &&
                (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) &&
                SpeechRecognizer.isRecognitionAvailable(context)
            ) {
                finishSession(sessionId, speechRecognizer)
                onEvent(VoiceRecognitionEvent.ConventionalRecognitionRequired)
                return
            }
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "No se pudo acceder al audio."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mony necesita permiso de micrófono."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "El reconocimiento en español no está disponible."
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "El servicio de voz convencional no pudo conectarse a la red."
                SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió la frase. Inténtalo nuevamente."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Inténtalo nuevamente."
                SpeechRecognizer.ERROR_SERVER -> "El servicio de voz no respondió."
                else -> "No se pudo completar el reconocimiento de voz."
            }
            onEvent(VoiceRecognitionEvent.Error(message))
            finishSession(sessionId, speechRecognizer)
        }
    }

    private companion object {
        const val GENERIC_SPANISH = "es"
        const val DOMINICAN_SPANISH = "es-DO"
    }
}
