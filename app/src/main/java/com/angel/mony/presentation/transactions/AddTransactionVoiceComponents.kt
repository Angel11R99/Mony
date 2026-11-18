package com.angel.mony.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.angel.mony.presentation.transactions.LocalAiPhase
import com.angel.mony.presentation.transactions.LocalAiReadiness
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
    voiceLevel: Float,
    saving: Boolean,
    readiness: LocalAiReadiness,
    interpreting: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onInterpretText: () -> Unit,
    onVoiceHelp: () -> Unit,
    onDismiss: () -> Unit,
    onListen: () -> Unit,
    onStop: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Describe tu movimiento",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            when {
                                interpreting -> "Organizando lo que dijiste"
                                listening -> "Te escucho"
                                else -> "Habla o escribe con naturalidad"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).size(40.dp)) {
                        MonyIcon(MonyIcon.Close, contentDescription = "Cerrar", modifier = Modifier.size(20.dp))
                    }
                }
                VoiceActivityOrb(
                    active = listening || interpreting,
                    level = if (listening) voiceLevel else 0f,
                    size = 124.dp,
                    contentDescription = when {
                        interpreting -> "Procesando el movimiento"
                        listening -> "Mony está escuchando"
                        else -> "Micrófono de Mony"
                    },
                )
                if (status.isNotBlank() && status != "Escuchando…") {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.contains("no ", ignoreCase = true) || status.contains("error", ignoreCase = true))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (status.contains("español", ignoreCase = true) ||
                    status.contains("reconocimiento de voz no está disponible", ignoreCase = true)
                ) {
                    TextButton(onClick = onVoiceHelp, enabled = !saving && !interpreting) {
                        Text("Ver cómo habilitar el español")
                    }
                }
                if (readiness.phase == LocalAiPhase.PREPARING) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Preparando el asistente…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (readiness.phase == LocalAiPhase.NOT_INSTALLED || readiness.phase == LocalAiPhase.ERROR) {
                    Text(
                        "El asistente no está listo. Puedes escribir la descripción y volver a intentarlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp, max = 132.dp),
                    placeholder = { Text(example ?: "Ej.: Gasté 850 pesos en una cena anoche") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                Button(
                    onClick = if (listening) onStop else onListen,
                    enabled = !saving && !interpreting && readiness.phase == LocalAiPhase.READY,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = CircleShape,
                ) {
                    if (interpreting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else MonyIcon(MonyIcon.Voice, contentDescription = null, role = MonyIconRole.STATE, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(when {
                        interpreting -> "Interpretando"
                        listening -> "Detener escucha"
                        status.contains("disponible", ignoreCase = true) || status.contains("intenta", ignoreCase = true) -> "Intentar de nuevo"
                        else -> "Hablar de nuevo"
                    })
                }
                if (textInput.isNotBlank()) {
                    TextButton(
                        onClick = onInterpretText,
                        enabled = !saving && !interpreting,
                    ) { Text("Interpretar texto") }
                }
            }
        }
    }
}

@Composable
internal fun VoiceMicButton(onClick: () -> Unit, enabled: Boolean) {
    val size = 92.dp
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = "Hablar con Mony",
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            VoiceActivityOrb(
                active = false,
                level = 0f,
                size = size,
                contentDescription = "Hablar con Mony",
            )
        }
    }
}

@Composable
private fun VoiceActivityOrb(
    active: Boolean,
    level: Float,
    size: Dp,
    contentDescription: String,
) {
    val animatedLevel by animateFloatAsState(
        targetValue = if (active) level.coerceIn(0f, 1f) else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "voice-level",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val density = LocalDensity.current
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size).clip(CircleShape)) {
            val radius = this.size.minDimension * 0.31f
            val amplitude = animatedLevel
            if (active) {
                drawCircle(
                    primary.copy(alpha = 0.12f + amplitude * 0.24f),
                    radius = radius * (1.24f + amplitude * 0.48f),
                    style = Stroke(with(density) { (1.5f + amplitude * 3f).dp.toPx() }),
                )
                drawCircle(
                    secondary.copy(alpha = 0.10f + amplitude * 0.20f),
                    radius = radius * (1.08f + amplitude * 0.34f),
                    style = Stroke(with(density) { (1.dp + amplitude.dp * 2f).toPx() }),
                )
            } else {
                drawCircle(primary.copy(alpha = 0.10f), radius = radius * 1.48f)
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(secondary, primary, primary.copy(alpha = 0.86f))),
                radius = radius * (1f + amplitude * 0.08f),
            )
        }
        MonyIcon(
            MonyIcon.Voice,
            contentDescription = contentDescription,
            tint = onPrimary,
            role = MonyIconRole.STATE,
            modifier = Modifier.size(34.dp),
        )
    }
}


