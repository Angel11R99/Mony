package com.angel.mony.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.angel.mony.data.nlp.LanguageModelDownloadWorker
import com.angel.mony.domain.model.LocalLanguageModel
import com.angel.mony.domain.model.LocalLanguageModelManager
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceModuleTopAppBar
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ModelDownloadUiState(
    val installed: Boolean = false,
    val partialBytes: Long = 0,
    val progress: Int = 0,
    val workState: WorkInfo.State? = null,
    val error: String? = null,
)

@HiltViewModel
class LanguageModelsSettingsViewModel @Inject constructor(
    private val manager: LocalLanguageModelManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val models: List<LocalLanguageModel> = manager.models
    private val workManager = WorkManager.getInstance(context)
    private val _activeModelId = MutableStateFlow(manager.activeModelId)
    val activeModelId: StateFlow<String> = _activeModelId.asStateFlow()
    private val _modelStates = MutableStateFlow(models.associate { model -> model.id to initialState(model.id) })
    val modelStates: StateFlow<Map<String, ModelDownloadUiState>> = _modelStates.asStateFlow()

    init {
        models.forEach { model ->
            viewModelScope.launch {
                observeWork(LanguageModelDownloadWorker.uniqueWorkName(model.id))
                    .collectLatest { infos ->
                        val work = infos.firstOrNull()
                        if (work?.state == WorkInfo.State.SUCCEEDED) _activeModelId.value = manager.activeModelId
                        updateModelState(model.id) { previous ->
                            previous.copy(
                                installed = manager.isModelInstalled(model.id),
                                partialBytes = manager.partialDownloadBytes(model.id),
                                progress = work?.progress?.getInt(LanguageModelDownloadWorker.KEY_PROGRESS, previous.progress) ?: previous.progress,
                                workState = work?.state,
                                error = if (work?.state == WorkInfo.State.FAILED) "La descarga no terminó. Puedes reanudarla o reiniciarla." else null,
                            )
                        }
                    }
            }
        }
    }

    fun select(modelId: String) {
        if (!manager.isModelInstalled(modelId)) return
        manager.setActiveModel(modelId)
        _activeModelId.value = modelId
    }

    fun downloadOrResume(modelId: String) {
        if (_modelStates.value[modelId]?.installed == true) return
        val request = OneTimeWorkRequestBuilder<LanguageModelDownloadWorker>()
            .setInputData(Data.Builder().putString(LanguageModelDownloadWorker.KEY_MODEL_ID, modelId).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(LanguageModelDownloadWorker.uniqueWorkName(modelId))
            .build()
        workManager.enqueueUniqueWork(
            LanguageModelDownloadWorker.uniqueWorkName(modelId),
            ExistingWorkPolicy.KEEP,
            request,
        )
        updateModelState(modelId) { it.copy(error = null) }
    }

    fun pause(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelAndWait(modelId)
            updateModelState(modelId) { it.copy(partialBytes = manager.partialDownloadBytes(modelId), workState = WorkInfo.State.CANCELLED) }
        }
    }

    fun resetDownload(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelAndWait(modelId)
            manager.resetDownload(modelId)
            updateModelState(modelId) { ModelDownloadUiState(installed = manager.isModelInstalled(modelId)) }
        }
    }

    fun removeModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            manager.removeModel(modelId)
            updateModelState(modelId) { ModelDownloadUiState() }
            _activeModelId.value = manager.activeModelId
        }
    }

    private suspend fun cancelAndWait(modelId: String) {
        val name = LanguageModelDownloadWorker.uniqueWorkName(modelId)
        workManager.cancelUniqueWork(name).result.get()
        while (true) {
            val infos = workManager.getWorkInfosForUniqueWork(name).get()
            if (infos.isEmpty() || infos.all { it.state.isFinished }) break
            delay(100)
        }
    }

    private fun observeWork(name: String) = callbackFlow {
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData(name)
        val observer = Observer<List<WorkInfo>> { infos -> trySend(infos) }
        liveData.observeForever(observer)
        awaitClose { liveData.removeObserver(observer) }
    }

    private fun initialState(modelId: String) = ModelDownloadUiState(
        installed = manager.isModelInstalled(modelId),
        partialBytes = manager.partialDownloadBytes(modelId),
    )

    private fun updateModelState(modelId: String, update: (ModelDownloadUiState) -> ModelDownloadUiState) {
        _modelStates.value = _modelStates.value.toMutableMap().apply {
            this[modelId] = update(this[modelId] ?: ModelDownloadUiState())
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LanguageModelsSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageModelsSettingsViewModel = hiltViewModel(),
) {
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val states by viewModel.modelStates.collectAsStateWithLifecycle()
    var modelToRemove by remember { mutableStateOf<LocalLanguageModel?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FinanceModuleTopAppBar(title = "Modelos de IA", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                    Text("Elige el modelo que interpretará tus descripciones", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Descarga modelos por separado y cambia el activo cuando quieras. Las descargas continúan en segundo plano y se pueden pausar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(viewModel.models, key = LocalLanguageModel::id) { model ->
                val state = states[model.id] ?: ModelDownloadUiState()
                ModelOptionCard(
                    model = model,
                    state = state,
                    active = activeModelId == model.id,
                    onSelect = { viewModel.select(model.id) },
                    onDownload = { viewModel.downloadOrResume(model.id) },
                    onPause = { viewModel.pause(model.id) },
                    onReset = { viewModel.resetDownload(model.id) },
                    onRemove = { modelToRemove = model },
                )
            }
            item {
                Text(
                    "Ambos modelos se ejecutan en el teléfono y no envían tus datos financieros a un servicio externo. Qwen3 1.7B ocupa más espacio y puede usar bastante memoria; el rendimiento depende del dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    modelToRemove?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToRemove = null },
            title = { Text("Eliminar modelo") },
            text = { Text("Se eliminará ${model.name} de este dispositivo. Podrás descargarlo de nuevo cuando quieras.") },
            confirmButton = {
                TextButton(onClick = { viewModel.removeModel(model.id); modelToRemove = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { modelToRemove = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ModelOptionCard(
    model: LocalLanguageModel,
    state: ModelDownloadUiState,
    active: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onRemove: () -> Unit,
) {
    val running = state.workState == WorkInfo.State.RUNNING || state.workState == WorkInfo.State.ENQUEUED
    val paused = !running && !state.installed && state.partialBytes > 0L
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium)
                    Text("LiteRT-LM · ${formatModelSize(model.sizeBytes)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                if (state.installed) RadioButton(selected = active, onClick = onSelect)
            }
            Text(model.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when {
                    active && state.installed -> "Activo · se usará para interpretar texto y dictado"
                    state.installed -> "Instalado · puedes activarlo"
                    running -> "Descargando en segundo plano · ${state.progress}%"
                    paused -> "Descarga pausada · ${formatModelSize(state.partialBytes)} de ${formatModelSize(model.sizeBytes)} guardados"
                    state.workState == WorkInfo.State.FAILED -> state.error ?: "La descarga falló."
                    else -> "No instalado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.workState == WorkInfo.State.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (running || state.progress > 0) {
                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when {
                state.installed -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!active) PrimaryButton("Usar este modelo", onClick = onSelect, modifier = Modifier.weight(1f))
                    SecondaryButton("Eliminar", onClick = onRemove, modifier = Modifier.weight(1f))
                }
                running -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Pausar", onClick = onPause, modifier = Modifier.weight(1f))
                    SecondaryButton("Reiniciar descarga", onClick = onReset, modifier = Modifier.weight(1f))
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        if (paused) "Reanudar descarga" else "Descargar modelo",
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.partialBytes > 0L || state.workState == WorkInfo.State.FAILED) {
                        SecondaryButton("Reiniciar", onClick = onReset, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun formatModelSize(bytes: Long): String = "%.0f MB".format(bytes / (1024.0 * 1024.0))
