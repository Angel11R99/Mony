package com.angel.mony.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.mony.presentation.transactions.LocalAiPhase
import com.angel.mony.presentation.transactions.LocalAiReadiness
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.iconography.MonyIconRole

@Composable
internal fun VoiceTypeBadge(state: VoiceTypeBadgeState) {
    val visual = when (state) {
        VoiceTypeBadgeState.UNIDENTIFIED -> VoiceTypeBadgeVisual(
            label = "Tipo: por identificar",
            icon = MonyIcon.Info,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        VoiceTypeBadgeState.CONFIRMATION_REQUIRED -> VoiceTypeBadgeVisual(
            label = "Tipo: por confirmar",
            icon = MonyIcon.Warning,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        VoiceTypeBadgeState.EXPENSE -> VoiceTypeBadgeVisual(
            label = "Gasto",
            icon = MonyIcon.Expense,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        VoiceTypeBadgeState.INCOME -> VoiceTypeBadgeVisual(
            label = "Ingreso",
            icon = MonyIcon.Income,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = visual.containerColor,
        contentColor = visual.contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonyIcon(
                icon = visual.icon,
                contentDescription = null,
                tint = visual.contentColor,
                role = MonyIconRole.STATE,
                modifier = Modifier.size(20.dp),
            )
            Text(visual.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private data class VoiceTypeBadgeVisual(
    val label: String,
    val icon: MonyIcon,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)

@Composable
internal fun VoiceDraftPanel(
    status: String,
    example: String?,
    listening: Boolean,
    saving: Boolean,
    readiness: LocalAiReadiness,
    interpreting: Boolean,
    onManageModels: () -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onInterpretText: () -> Unit,
    onListen: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Describe tu movimiento", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = textInput,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Di qué ocurrió con tu dinero") },
                minLines = 2,
                maxLines = 4,
            )
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (readiness.phase == LocalAiPhase.PREPARING) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Preparando IA…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (readiness.phase) {
                LocalAiPhase.READY -> androidx.compose.material3.Button(
                    onClick = if (listening) onStop else onListen,
                    enabled = !saving && !interpreting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (interpreting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        MonyIcon(
                            MonyIcon.Voice,
                            contentDescription = null,
                            role = MonyIconRole.STATE,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(when {
                        interpreting -> "Interpretando..."
                        listening -> "Terminar dictado"
                        else -> "Hablar con Mony"
                    })
                }
                LocalAiPhase.PREPARING -> Unit
                LocalAiPhase.NOT_INSTALLED -> SecondaryButton(
                    "Configurar IA",
                    onClick = onManageModels,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                LocalAiPhase.ERROR -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val message = when (readiness.failure) {
                        com.angel.mony.domain.model.LocalModelFailure.INVALID_FILE -> "El archivo del modelo está dañado."
                        com.angel.mony.domain.model.LocalModelFailure.INSUFFICIENT_MEMORY -> "Este teléfono no tiene memoria suficiente para abrir el modelo."
                        com.angel.mony.domain.model.LocalModelFailure.INCOMPATIBLE -> "Este modelo no es compatible con el motor de Mony."
                        else -> "No se pudo preparar la IA."
                    }
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    SecondaryButton("Configurar IA", onClick = onManageModels, enabled = !saving)
                }
            }
        }
    }
}


