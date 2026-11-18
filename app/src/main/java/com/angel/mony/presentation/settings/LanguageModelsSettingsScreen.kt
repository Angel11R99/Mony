package com.angel.mony.presentation.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.angel.mony.data.nlp.LanguageModelDownloadWorker
import com.angel.mony.domain.model.*
import com.angel.mony.presentation.components.FinanceModuleTopAppBar
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageModelsSettingsViewModel @Inject constructor(
    private val manager: LocalLanguageModelManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val models = manager.models
    val selection = MutableStateFlow(manager.activeModelId)
    val status = MutableStateFlow("Verificando IA...")
    val busy = MutableStateFlow(false)
    val jobs = MutableStateFlow<Map<String, WorkInfo>>(emptyMap())
    val pauses = MutableStateFlow<Set<String>>(emptySet())
    val installedIds = MutableStateFlow(models.filter { manager.isModelInstalled(it.id) }.map { it.id }.toSet())
    val googleId = "gemini-nano-aicore"

    init {
        models.filter { it.id != googleId }.forEach { model ->
            viewModelScope.launch {
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(
                    LanguageModelDownloadWorker.uniqueWorkName(model.id)
                ).collectLatest { infos ->
                    val current = infos.firstOrNull()
                    if (current != null) jobs.value = jobs.value + (model.id to current)
                    if (current?.state == WorkInfo.State.SUCCEEDED) refresh()
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            installedIds.value = models.filter { manager.isModelInstalled(it.id) }.map { it.id }.toSet()
            selection.value = manager.activeModelId
            val preparation = manager.prepareActiveModel()
            status.value = when (preparation) {
                is LocalModelPreparation.Ready -> "Motor disponible: " + models.firstOrNull { it.id == preparation.modelId }?.name.orEmpty()
                LocalModelPreparation.NoInstalledModel -> "Instala un modelo compatible para interpretar la voz."
                is LocalModelPreparation.Failed -> "No se pudo iniciar el modelo seleccionado; puedes elegir otra alternativa."
            }
        }
    }

    fun select(modelId: String) {
        if (modelId != googleId && !manager.isModelInstalled(modelId)) return
        manager.setActiveModel(modelId)
        selection.value = modelId
        refresh()
    }

    fun download(modelId: String) {
        if (busy.value) return
        if (jobs.value[modelId]?.state in listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)) return
        if (modelId == googleId) {
            viewModelScope.launch {
                busy.value = true
                status.value = "Preparando Gemini Nano mediante AICore..."
                try {
                    manager.downloadModel(modelId) {}
                    manager.setActiveModel(modelId)
                } catch (_: Exception) {
                    status.value = "Gemini Nano no está disponible en este dispositivo."
                } finally {
                    busy.value = false
                    refresh()
                }
            }
        } else {
            val request = OneTimeWorkRequestBuilder<LanguageModelDownloadWorker>()
                .setInputData(Data.Builder().putString(LanguageModelDownloadWorker.KEY_MODEL_ID, modelId).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                LanguageModelDownloadWorker.uniqueWorkName(modelId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
            pauses.value = pauses.value - modelId
            status.value = "Descargando modelo…"

        }
    }
    fun partialBytes(modelId: String): Long = manager.partialDownloadBytes(modelId)

    fun pause(modelId: String) {
        viewModelScope.launch {
            WorkManager.getInstance(context).cancelUniqueWork(
                LanguageModelDownloadWorker.uniqueWorkName(modelId)
            ).result.get()
            pauses.value = pauses.value + modelId
            status.value = "Descarga pausada. Puedes reanudarla."
        }
    }

    fun remove(modelId: String) {
        if (modelId == googleId) return
        viewModelScope.launch {
            WorkManager.getInstance(context).cancelUniqueWork(
                LanguageModelDownloadWorker.uniqueWorkName(modelId)
            ).result.get()
            manager.resetDownload(modelId)
            manager.removeModel(modelId)
            jobs.value = jobs.value - modelId
            pauses.value = pauses.value - modelId
            refresh()
        }
    }

}

@Composable
fun LanguageModelsSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageModelsSettingsViewModel = hiltViewModel(),
) {
    val selected by viewModel.selection.collectAsStateWithLifecycle()
    val installed by viewModel.installedIds.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    Scaffold(topBar = { FinanceModuleTopAppBar(title = "Modelos de IA", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Selecciona una IA para registrar movimientos por voz.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
            items(viewModel.models, key = { it.id }) { model ->
                val isGoogle = model.id == viewModel.googleId
                val available = model.id in installed
                val job = jobs[model.id]
                val downloading = job?.state in listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)
                val progress = job?.progress?.getInt(LanguageModelDownloadWorker.KEY_PROGRESS, 0) ?: 0
                val transferred = viewModel.partialBytes(model.id)
                val percent = if (progress > 0) progress else if (model.sizeBytes > 0) ((transferred * 100L) / model.sizeBytes).toInt().coerceIn(0, 100) else 0
                val partial = transferred > 0L
                val downloadError = if (job?.state == WorkInfo.State.FAILED)
                    job.outputData.getString(LanguageModelDownloadWorker.KEY_ERROR) ?: "La descarga falló. Puedes reanudarla."
                    else null
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(model.name, style = MaterialTheme.typography.titleMedium)
                            if (selected == model.id) Text("Seleccionado", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(model.description, style = MaterialTheme.typography.bodySmall)
                        if (downloading || partial && !available) {
                            if (percent > 0) LinearProgressIndicator(
                                progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                (if (downloading) "Descargando" else "Descarga pausada") +
                                    " · $percent% · ${transferred / 1048576L} MB de ${model.sizeBytes / 1048576L} MB",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (downloading) {
                            TextButton(onClick = { viewModel.pause(model.id) },
                                modifier = Modifier.fillMaxWidth()) { Text("Pausar descarga") }
                        }
                        downloadError?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error) }
                        if (available || isGoogle) {
                            PrimaryButton(
                                if (selected == model.id) "Comprobar disponibilidad" else "Elegir modelo",
                                onClick = {
                                    if (selected == model.id) viewModel.refresh() else viewModel.select(model.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                        }
                        if (!available && !downloading) {
                            PrimaryButton(
                                if (isGoogle) "Preparar con AICore" else if (partial) "Reanudar descarga" else "Descargar modelo",
                                onClick = { viewModel.download(model.id) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                        }
                        if (!isGoogle && (available || partial) && !downloading) {
                            TextButton(onClick = { viewModel.remove(model.id) },
                                modifier = Modifier.fillMaxWidth()) { Text("Eliminar archivos del modelo") }
                        }
                    }
                }
            }
            item {
                Text(
                    "Gemini Nano depende de AICore. Los modelos Qwen se descargan para ejecutarse localmente " +
                        "con LiteRT-LM. El registro manual sigue disponible en todos los dispositivos.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
}
