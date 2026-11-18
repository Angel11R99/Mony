package com.angel.mony.presentation.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.iconography.MonyIconRole
import com.angel.mony.ui.iconography.MonyIcon as MonyIconView
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

private enum class ModelListFilter(val label: String) {
    ALL("Todos"), INSTALLED("Instalados"), DOWNLOADABLE("Descargables")
}

@HiltViewModel
class LanguageModelsSettingsViewModel @Inject constructor(
    private val manager: LocalLanguageModelManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val models = MutableStateFlow(manager.models)
    val selection = MutableStateFlow(manager.activeModelId)
    val status = MutableStateFlow("Verificando IA...")
    val busy = MutableStateFlow(false)
    val diagnostic = MutableStateFlow<Map<String, String>>(emptyMap())
    val errorAlert = MutableStateFlow<Pair<String, String>?>(null)
    val catalogRefreshing = MutableStateFlow(false)
    val jobs = MutableStateFlow<Map<String, WorkInfo>>(emptyMap())
    val requestedDownloads = MutableStateFlow<Set<String>>(emptySet())
    val pauses = MutableStateFlow<Set<String>>(emptySet())
    val installedIds = MutableStateFlow(models.value.filter { manager.isModelInstalled(it.id) }.map { it.id }.toSet())
    val googleId = "gemini-nano-aicore"
    private val observedDownloadIds = mutableSetOf<String>()

    init {
        observeDownloads(models.value)
        refresh()
        refreshCatalog()
    }

    private fun observeDownloads(modelList: List<LocalLanguageModel>) {
        modelList.filter { it.id != googleId && observedDownloadIds.add(it.id) }.forEach { model ->
            viewModelScope.launch {
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(
                    LanguageModelDownloadWorker.uniqueWorkName(model.id)
                ).collectLatest { infos ->
                    val current = infos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
                        ?: infos.lastOrNull()
                    if (current != null) jobs.value = jobs.value + (model.id to current)
                    if (current?.state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)) {
                        requestedDownloads.value = requestedDownloads.value - model.id
                    }
                    if (current?.state == WorkInfo.State.FAILED) {
                        val reason = current.outputData.getString(LanguageModelDownloadWorker.KEY_ERROR)
                            ?: "La descarga terminó inesperadamente. Comprueba tu conexión y el espacio disponible."
                        errorAlert.value = model.name to reason
                    }
                    if (current?.state == WorkInfo.State.SUCCEEDED) refresh()
                }
            }
        }
    }

    fun refreshCatalog() {
        if (catalogRefreshing.value) return
        viewModelScope.launch {
            catalogRefreshing.value = true
            status.value = "Actualizando catálogo de AI Edge Gallery…"
            try {
                val updated = withContext(Dispatchers.IO) { manager.refreshModelCatalog() }
                models.value = manager.models
                observeDownloads(models.value)
                refresh()
                status.value = if (updated) {
                    "Catálogo actualizado · ${models.value.count { it.catalogSource == "AI Edge Gallery" }} modelos compatibles con texto."
                } else {
                    "No se pudo actualizar el catálogo. Se muestran los modelos guardados en el teléfono."
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                models.value = manager.models
                observeDownloads(models.value)
                refresh()
                status.value = "No se pudo actualizar el catálogo. Se muestran los modelos guardados en el teléfono."
            } finally {
                catalogRefreshing.value = false
            }
        }
    }

    fun refresh() {
        models.value = manager.models
        installedIds.value = models.value.filter { manager.isModelInstalled(it.id) }.map { it.id }.toSet()
        selection.value = manager.activeModelId
        // Browsing settings should not load a gigabyte-sized LLM.
        status.value = if (models.value.any { it.id != googleId && it.id in installedIds.value }) {
            "Modelos locales instalados. Selecciona uno para registrar por voz."
        } else {
            "Elige un modelo descargable. Gemini Nano requiere AICore."
        }
    }

    fun select(modelId: String) {
        if (modelId != googleId && !manager.isModelInstalled(modelId)) return
        manager.setActiveModel(modelId)
        selection.value = modelId
        refresh()
    }

    fun checkAvailability(modelId: String) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            diagnostic.value = diagnostic.value + (modelId to "Comprobando disponibilidad…")
            try {
                val feedback = if (modelId == googleId || !manager.isModelInstalled(modelId)) {
                    withContext(Dispatchers.IO) { manager.checkDownloadAvailability(modelId) }
                } else {
                    val result = withContext(Dispatchers.IO) { manager.prepareActiveModel() }
                    when {
                        result is LocalModelPreparation.Ready && result.modelId == modelId ->
                            "Modelo disponible y listo para usar sin conexión."
                        result is LocalModelPreparation.Ready ->
                            "Este modelo no está activo ahora. Se preparó ${manager.models.firstOrNull { it.id == result.modelId }?.name ?: "otro modelo local"}."
                        result is LocalModelPreparation.NoInstalledModel ->
                            "El modelo todavía no está descargado."
                        result is LocalModelPreparation.Failed && result.reason == LocalModelFailure.INCOMPATIBLE ->
                            "El modelo está instalado, pero este dispositivo no pudo iniciarlo."
                        result is LocalModelPreparation.Failed && result.reason == LocalModelFailure.INSUFFICIENT_MEMORY ->
                            "El modelo está instalado, pero no hay memoria suficiente para iniciarlo ahora."
                        result is LocalModelPreparation.Failed ->
                            "El modelo está instalado, pero no se pudo iniciar en este dispositivo."
                        else -> "No se pudo comprobar la disponibilidad del modelo."
                    }
                }
                diagnostic.value = diagnostic.value + (modelId to feedback)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val reason = failure.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudo comprobar el modelo. Comprueba la conexión e inténtalo de nuevo."
                diagnostic.value = diagnostic.value + (modelId to reason)
                errorAlert.value = manager.models.firstOrNull { it.id == modelId }?.name.orEmpty() to reason
            } finally {
                busy.value = false
                refresh()
            }
        }
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
                } catch (failure: Exception) {
                    val reason = failure.message?.takeIf { it.isNotBlank() }
                        ?: "Este dispositivo no admite Gemini Nano mediante AICore."
                    diagnostic.value = diagnostic.value + (modelId to reason)
                    errorAlert.value = (models.value.firstOrNull { it.id == modelId }?.name ?: "Gemini Nano") to reason
                } finally {
                    busy.value = false
                    refresh()
                }
            }
        } else {
            requestedDownloads.value = requestedDownloads.value + modelId
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                WorkManager.getInstance(context).cancelUniqueWork(
                    LanguageModelDownloadWorker.uniqueWorkName(modelId)
                ).result.get()
            }
            pauses.value = pauses.value + modelId
            requestedDownloads.value = requestedDownloads.value - modelId
            status.value = "Descarga pausada. Puedes reanudarla."
        }
    }

    fun remove(modelId: String) {
        if (modelId == googleId) return
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                WorkManager.getInstance(context).cancelUniqueWork(
                    LanguageModelDownloadWorker.uniqueWorkName(modelId)
                ).result.get()
                manager.resetDownload(modelId)
                manager.removeModel(modelId)
            }
            jobs.value = jobs.value - modelId
            pauses.value = pauses.value - modelId
            refresh()
        }
    }

    fun dismissError() {
        errorAlert.value = null
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
    val requestedDownloads by viewModel.requestedDownloads.collectAsStateWithLifecycle()
    val diagnostic by viewModel.diagnostic.collectAsStateWithLifecycle()
    val errorAlert by viewModel.errorAlert.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val catalogRefreshing by viewModel.catalogRefreshing.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var listFilter by rememberSaveable { mutableStateOf(ModelListFilter.ALL) }
    val visibleModels = remember(models, installed, searchQuery, listFilter) {
        val query = searchQuery.trim()
        models.filter { model ->
            val matchesQuery = query.isBlank() || model.name.contains(query, ignoreCase = true) ||
                model.description.contains(query, ignoreCase = true) ||
                (model.catalogSource?.contains(query, ignoreCase = true) == true)
            val matchesFilter = when (listFilter) {
                ModelListFilter.ALL -> true
                ModelListFilter.INSTALLED -> model.id in installed
                ModelListFilter.DOWNLOADABLE -> model.id !in installed && model.id != viewModel.googleId
            }
            matchesQuery && matchesFilter
        }
    }
    Scaffold(topBar = {
        FinanceModuleTopAppBar(title = "Modelos de IA", onBack = onBack, actions = {
            GlobalOutlinedIconButton(
                semanticIcon = MonyIcon.Reopen,
                contentDescription = "Actualizar catálogo de modelos",
                onClick = viewModel::refreshCatalog,
                enabled = !catalogRefreshing,
                size = 44.dp,
            )
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Elige un modelo local para interpretar movimientos por voz y generar recomendaciones.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.startsWith("Catálogo actualizado")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (catalogRefreshing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("Buscar modelos") },
                    placeholder = { Text("Nombre o descripción") },
                    leadingIcon = { MonyIconView(MonyIcon.Search, contentDescription = null) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModelListFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = listFilter == filter,
                            onClick = { listFilter = filter },
                            label = { Text(filter.label) },
                        )
                    }
                }
            }
            if (visibleModels.isEmpty()) {
                item {
                    Text(
                        if (listFilter == ModelListFilter.INSTALLED) "No hay modelos instalados que coincidan."
                        else "No se encontraron modelos. Prueba con otro nombre o filtro.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visibleModels, key = { it.id }) { model ->
                val isGoogle = model.id == viewModel.googleId
                val available = model.id in installed
                val memoryRequirement = model.minimumDeviceMemoryInGb
                val job = jobs[model.id]
                val downloading = job?.state in listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)
                val downloadPending = model.id in requestedDownloads
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
                            if (selected == model.id) {
                                MonyIconView(
                                    icon = MonyIcon.Check,
                                    contentDescription = "Modelo seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    role = MonyIconRole.STATE,
                                    size = 24.dp,
                                )
                            }
                        }
                        Text(model.description, style = MaterialTheme.typography.bodySmall)
                        model.catalogSource?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (memoryRequirement != null) {
                            Text(
                                "Advertencia: se recomiendan ${memoryRequirement} GB de RAM. Puedes descargarlo; en este dispositivo podría no iniciar o funcionar lentamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (downloading || downloadPending || partial && !available) {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                (if (downloading || downloadPending) "Descargando" else "Descarga pausada") +
                                    " · $percent% · ${transferred / 1048576L} MB de ${model.sizeBytes / 1048576L} MB",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (downloading) {
                            TextButton(onClick = { viewModel.pause(model.id) },
                                modifier = Modifier.fillMaxWidth()) { Text("Pausar descarga") }
                        }
                        downloadError?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error) }
                        diagnostic[model.id]?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = if (it.startsWith("No se pudo")) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            if (!available && !downloading) {
                                GlobalOutlinedIconButton(
                                    semanticIcon = MonyIcon.Search,
                                    contentDescription = "Comprobar disponibilidad de ${model.name} antes de descargar",
                                    onClick = { viewModel.checkAvailability(model.id) },
                                    enabled = !busy,
                                    size = 44.dp,
                                )
                            }
                            if ((available || isGoogle) && selected != model.id) {
                                GlobalOutlinedIconButton(
                                    semanticIcon = MonyIcon.Check,
                                    contentDescription = "Seleccionar ${model.name}",
                                    onClick = { viewModel.select(model.id) }, enabled = !busy, size = 44.dp,
                                )
                            }
                            if (selected == model.id && !downloading) {
                                GlobalOutlinedIconButton(
                                    semanticIcon = MonyIcon.Reopen,
                                    contentDescription = "Comprobar disponibilidad de ${model.name}",
                                    onClick = { viewModel.checkAvailability(model.id) }, enabled = !busy, size = 44.dp,
                                )
                            }
                            if (!available && !downloading) {
                                GlobalOutlinedIconButton(
                                    semanticIcon = MonyIcon.Download,
                                    contentDescription = if (isGoogle) "Preparar ${model.name} con AICore" else if (partial) "Reanudar descarga de ${model.name}" else "Descargar ${model.name}",
                                    onClick = { viewModel.download(model.id) }, enabled = !busy, size = 44.dp,
                                )
                            }
                            if (!isGoogle && (available || partial) && !downloading) {
                                GlobalOutlinedIconButton(
                                    semanticIcon = MonyIcon.Delete,
                                    contentDescription = "Eliminar ${model.name}",
                                    onClick = { viewModel.remove(model.id) }, enabled = !busy, size = 44.dp,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "El catálogo de AI Edge Gallery incluye modelos de texto preparados para LiteRT-LM. " +
                        "Actualizar la lista y descargar requieren Internet; los modelos descargados se ejecutan " +
                        "localmente. Gemini Nano depende de AICore y del dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
    errorAlert?.let { (modelName, reason) ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("No se pudo usar $modelName") },
            text = { Text(reason) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("Entendido") }
            },
        )
    }
}
