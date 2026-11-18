package com.angel.mony.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.angel.mony.domain.model.LocalLanguageModelManager
import com.angel.mony.domain.model.LocalModelPreparation
import com.angel.mony.presentation.components.FinanceModuleTopAppBar
import com.angel.mony.presentation.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageModelsSettingsViewModel @Inject constructor(
    private val manager: LocalLanguageModelManager
) : ViewModel() {
    private val _state = MutableStateFlow<String>("Comprobando disponibilidad...")
    val state: StateFlow<String> = _state
    private val _downloadable = MutableStateFlow(false)
    val downloadable: StateFlow<Boolean> = _downloadable
    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val readiness = manager.prepareActiveModel()) {
                is LocalModelPreparation.Ready -> {
                    _state.value = "Gemini Nano está disponible."
                    _downloadable.value = false
                }
                LocalModelPreparation.NoInstalledModel -> {
                    _state.value = "Gemini Nano necesita descargarse mediante Android AICore."
                    _downloadable.value = true
                }
                is LocalModelPreparation.Failed -> {
                    _state.value = "Gemini Nano no está disponible en este dispositivo o no pudo iniciarse."
                    _downloadable.value = false
                }
            }
        }
    }

    fun download() {
        viewModelScope.launch {
            _downloadable.value = false
            _state.value = "Preparando Gemini Nano..."
            try {
                manager.downloadModel(manager.activeModelId) {}
            } catch (_: Exception) {
                _state.value = "No se pudo preparar Gemini Nano. Comprueba AICore y tu conexión."
            }
            refresh()
        }
    }
}

/** Gemini Nano is managed by Android AICore, not by the old Qwen file manager. */
@Composable
fun LanguageModelsSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageModelsSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadable by viewModel.downloadable.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { FinanceModuleTopAppBar(title = "IA por voz", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Gemini Nano", style = MaterialTheme.typography.titleLarge)
            Text(state, style = MaterialTheme.typography.bodyMedium)
            Text(
                "La interpretación financiera utiliza la IA local de Google cuando Android AICore está disponible. " +
                    "No se envían movimientos a un servicio de IA remoto. El registro manual siempre funciona.",
                style = MaterialTheme.typography.bodySmall
            )
            if (downloadable) {
                PrimaryButton("Preparar IA", onClick = viewModel::download, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
