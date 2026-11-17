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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    transcript: String,
    example: String?,
    visible: Boolean,
    listening: Boolean,
    saving: Boolean,
    onListen: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    if (!visible) return
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MonyIcon(
                    MonyIcon.Voice,
                    contentDescription = null,
                    tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    role = MonyIconRole.STATE,
                )
                Text(
                    text = status.ifBlank { "Dictado listo." },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (example != null) {
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (transcript.isNotBlank()) Text("“$transcript”", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(
                    text = "Repetir",
                    onClick = onListen,
                    enabled = !listening && !saving,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Detener",
                    onClick = onStop,
                    enabled = listening,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(
                    text = "Cancelar",
                    onClick = onCancel,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Guardar",
                    onClick = onSave,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


