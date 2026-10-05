package com.angel.mony.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.iconography.MonyIconRole
import com.angel.mony.ui.theme.LocalAppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFundingDialog(
    overflowAmountInCents: Long,
    availableBeforeExpenseInCents: Long,
    expenseAmountInCents: Long,
    onConfirm: (sourceDescription: String) -> Unit,
    onDismiss: () -> Unit,
    sourceDescription: String? = null,
    onSourceDescriptionChange: ((String) -> Unit)? = null,
    onVoice: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var localSourceDescription by remember { mutableStateOf("") }
    val currentSourceDescription = sourceDescription ?: localSourceDescription
    val isDescriptionValid = currentSourceDescription.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = LocalAppShapes.current.dialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = {
            MonyIcon(
                icon = MonyIcon.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                role = MonyIconRole.STATE,
                modifier = Modifier.size(48.dp),
            )
        },
        title = { Text("Este gasto supera tu saldo disponible") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Te faltan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        MoneyFormatter.format(overflowAmountInCents),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FundingInfoRow(
                            label = "Disponible antes del gasto",
                            value = MoneyFormatter.format(availableBeforeExpenseInCents),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                        )
                        FundingInfoRow(
                            label = "Gasto",
                            value = MoneyFormatter.format(expenseAmountInCents),
                            valueColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Text(
                    "¿De dónde salió este dinero?",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = currentSourceDescription,
                    onValueChange = { value ->
                        if (onSourceDescriptionChange != null) onSourceDescriptionChange(value) else localSourceDescription = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ej.: préstamo, efectivo guardado, otra cuenta, tarjeta de crédito...") },
                    placeholder = { Text("Origen del dinero adicional") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isDescriptionValid) onConfirm(currentSourceDescription.trim()) },
                    ),
                    visualTransformation = VisualTransformation.None,
                    singleLine = false,
                    maxLines = 4,
                )
                if (onVoice != null) {
                    TextButton(onClick = onVoice) {
                        MonyIcon(MonyIcon.Voice, contentDescription = null)
                        Text(" Dictar fuente")
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Registrar gasto",
                onClick = { if (isDescriptionValid) onConfirm(currentSourceDescription.trim()) },
                enabled = isDescriptionValid,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun FundingInfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
    }
}
