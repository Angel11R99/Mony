package com.angel.mony.domain.model

/** Semantic layer contract. Implementations never persist a transaction. */
interface NaturalLanguageTransactionEngine {
    suspend fun interpret(
        transcript: String,
        categories: List<Category>,
        current: VoiceTransactionDraft,
    ): NaturalLanguageInterpretation
}

data class NaturalLanguageInterpretation(
    val command: VoiceTransactionCommand,
    val engine: InterpretationEngine,
)

enum class InterpretationEngine { ON_DEVICE_LLM, LOCAL_FALLBACK }

interface LocalLanguageModelManager {
    val models: List<LocalLanguageModel>
    val activeModelId: String
    fun isModelInstalled(modelId: String): Boolean
    fun setActiveModel(modelId: String)
    fun hasPartialDownload(modelId: String): Boolean
    fun partialDownloadBytes(modelId: String): Long
    suspend fun downloadModel(modelId: String, onProgress: (Int) -> Unit)
    suspend fun resetDownload(modelId: String)
    suspend fun removeModel(modelId: String)
}

data class LocalLanguageModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
)
