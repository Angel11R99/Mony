package com.angel.mony.data.nlp

import android.content.Context
import com.angel.mony.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two genuinely independent local inference backends:
 * Android AICore Gemini Nano, or downloadable LiteRT-LM Qwen.
 * Never silently switches a user-selected model during an inference.
 */
@Singleton
class VoiceModelCatalog @Inject constructor(
    private val google: GoogleVoiceTransactionEngine,
    private val qwen: OnDeviceNaturalLanguageTransactionEngine,
    @param:ApplicationContext context: Context,
) : NaturalLanguageTransactionEngine, LocalLanguageModelManager {
    private val preferences = context.getSharedPreferences("mony_voice_ai", Context.MODE_PRIVATE)
    private val googleId = google.activeModelId
    override val models: List<LocalLanguageModel>
        get() = google.models + qwen.models
    override val activeModelId: String
        get() = preferences.getString("selected_engine", null)
            ?.takeIf { id -> models.any { it.id == id } }
            ?: if (qwen.models.any { qwen.isModelInstalled(it.id) }) {
                qwen.models.first { qwen.isModelInstalled(it.id) }.id
            } else googleId

    override fun isModelInstalled(modelId: String): Boolean =
        if (modelId == googleId) false // AICore availability is asynchronous; check prepareActiveModel().
        else qwen.isModelInstalled(modelId)

    override fun setActiveModel(modelId: String) {
        require(models.any { it.id == modelId }) { "Modelo no reconocido" }
        if (modelId != googleId) qwen.setActiveModel(modelId)
        preferences.edit().putString("selected_engine", modelId).apply()
    }

    override suspend fun prepareActiveModel(): LocalModelPreparation {
        val selected = activeModelId
        if (selected == googleId) {
            val googleResult = google.prepareActiveModel()
            if (googleResult is LocalModelPreparation.Ready) return googleResult
            // If explicitly selected, preserve the preference. Offer an installed Qwen as
            // a temporary alternative rather than permanently overwriting the selection.
            val installed = qwen.models.firstOrNull { qwen.isModelInstalled(it.id) }
            if (installed != null) {
                val prepared = qwen.prepareActiveModel()
                android.util.Log.i("MonyAI", "prepare fallback=${prepared.javaClass.simpleName} from=aicore to=litert")
                return prepared
            }
            return googleResult
        }
        val prepared = qwen.prepareActiveModel()
        android.util.Log.i("MonyAI", "prepare selected=qwen result=${prepared.javaClass.simpleName}")
        return prepared
    }

    override suspend fun interpret(
        transcript: String,
        categories: List<Category>,
        current: VoiceTransactionDraft,
    ): NaturalLanguageInterpretation {
        val ready = prepareActiveModel()
        if (ready !is LocalModelPreparation.Ready) {
            return NaturalLanguageInterpretation(
                VoiceTransactionCommand.Invalid("No hay un modelo compatible preparado. Elige uno en Ajustes."),
                InterpretationEngine.ON_DEVICE_LLM,
                LocalModelFailure.NOT_AVAILABLE
            )
        }
        android.util.Log.i("MonyAI", "inference selected=${activeModelId} prepared=${ready.modelId}")
        return if (ready.modelId == googleId) google.interpret(transcript, categories, current)
        else qwen.interpret(transcript, categories, current)
    }

    override suspend fun unloadActiveModel() {
        qwen.unloadActiveModel()
        google.unloadActiveModel()
    }
    override suspend fun generateText(prompt: String): String {
        val ready = prepareActiveModel()
        check(ready is LocalModelPreparation.Ready) { "No hay un modelo local disponible. Revisa Modelos de IA en Ajustes." }
        return if (ready.modelId == googleId) google.generateText(prompt)
        else qwen.generateText(prompt)
    }
    override suspend fun refreshModelCatalog(): Boolean = qwen.refreshModelCatalog()
    override fun hasPartialDownload(modelId: String) =
        modelId != googleId && qwen.hasPartialDownload(modelId)
    override fun partialDownloadBytes(modelId: String) =
        if (modelId == googleId) 0 else qwen.partialDownloadBytes(modelId)
    override suspend fun downloadModel(modelId: String, onProgress: (Int) -> Unit) {
        if (modelId == googleId) google.downloadModel(modelId, onProgress)
        else qwen.downloadModel(modelId, onProgress)
    }
    override suspend fun checkDownloadAvailability(modelId: String): String =
        if (modelId == googleId) google.checkDownloadAvailability(modelId)
        else qwen.checkDownloadAvailability(modelId)
    override suspend fun resetDownload(modelId: String) {
        if (modelId != googleId) qwen.resetDownload(modelId)
    }
    override suspend fun removeModel(modelId: String) {
        if (modelId != googleId) qwen.removeModel(modelId)
    }
}
