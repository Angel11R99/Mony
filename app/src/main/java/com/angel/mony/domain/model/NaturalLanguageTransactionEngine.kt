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
    val modelFailure: LocalModelFailure? = null,
)

enum class InterpretationEngine { ON_DEVICE_LLM, LOCAL_FALLBACK }

interface LocalLanguageModelManager {
    val models: List<LocalLanguageModel>
    val activeModelId: String
    fun isModelInstalled(modelId: String): Boolean
    fun setActiveModel(modelId: String)
    suspend fun prepareActiveModel(): LocalModelPreparation
    suspend fun unloadActiveModel()
    fun hasPartialDownload(modelId: String): Boolean
    fun partialDownloadBytes(modelId: String): Long
    suspend fun downloadModel(modelId: String, onProgress: (Int) -> Unit)
    suspend fun checkDownloadAvailability(modelId: String): String
    suspend fun resetDownload(modelId: String)
    suspend fun removeModel(modelId: String)
    suspend fun generateText(prompt: String): String
    suspend fun refreshModelCatalog(): Boolean = false
}

sealed interface LocalModelPreparation {
    data object NoInstalledModel : LocalModelPreparation
    data class Ready(val modelId: String) : LocalModelPreparation
    data class Failed(val modelId: String, val reason: LocalModelFailure) : LocalModelPreparation
}

enum class LocalModelFailure { NOT_AVAILABLE, INVALID_FILE, INCOMPATIBLE, INSUFFICIENT_MEMORY, INITIALIZATION, INFERENCE }

data class LocalLanguageModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val minimumDeviceMemoryInGb: Int? = null,
    val deviceMemoryInGb: Int? = null,
    val catalogSource: String? = null,
)

/** Keeps a user's valid selection; otherwise chooses the first installed id in documented priority order. */
fun resolveInstalledLanguageModel(
    preferredId: String?,
    installedIds: Set<String>,
    defaultPriority: List<String>,
): String? = preferredId?.takeIf(installedIds::contains)
    ?: defaultPriority.firstOrNull(installedIds::contains)
