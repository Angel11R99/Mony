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
    val isModelInstalled: Boolean
    val canRunModel: Boolean
    val modelBytes: Long
    suspend fun downloadModel(onProgress: (Int) -> Unit)
    suspend fun removeModel()
}
