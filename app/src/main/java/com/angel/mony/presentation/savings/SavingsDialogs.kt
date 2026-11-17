package com.angel.mony.presentation.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput

@OptIn(ExperimentalMaterial3Api::class)

@Composable
internal fun GoalEditorDialog(
    existing: SavingsGoalProgress?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.goal?.name.orEmpty()) }
    var target by remember(existing) {
        mutableStateOf(
            existing?.goal?.targetAmountInCents
                ?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }
                .orEmpty()
        )
    }
    val formState = remember { FormState() }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (existing == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceTextField(
                    name,
                    { name = it; formState.clearError("name") },
                    "Nombre",
                    singleLine = true,
                    isError = formState.hasError("name"),
                    errorMessage = formState["name"],
                )
                FinanceTextField(
                    target,
                    { target = sanitizeAmountInput(it); formState.clearError("target") },
                    "Objetivo (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                    isError = formState.hasError("target"),
                    errorMessage = formState["target"],
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Guardando…" else "Guardar",
                onClick = {
                    formState.clearAll()
                    if (name.isBlank()) formState.setError("name", "Escribe un nombre")
                    val cents = MoneyFormatter.parseToCents(target)
                    if (cents == null || cents <= 0) formState.setError("target", "Ingresa un monto válido")
                    if (!formState.isValid()) return@PrimaryButton
                    onSave(name, target)
                },
                enabled = !isSaving && name.isNotBlank() && (
                    existing == null ||
                        name.trim() != existing.goal.name ||
                        MoneyFormatter.parseToCents(target) != existing.goal.targetAmountInCents
                    ),
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
internal fun ContributeDialog(
    goal: SavingsGoalProgress,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var amount by remember(goal) { mutableStateOf("") }
    var description by remember(goal) { mutableStateOf("") }
    val formState = remember { FormState() }
    val cents = MoneyFormatter.parseToCents(amount)
    val remaining = (goal.goal.targetAmountInCents - goal.savedInCents).coerceAtLeast(0)
    val showBreakdown = cents != null && cents > 0 && remaining > 0 && cents > remaining
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Aportar a ${goal.goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Llevas ${MoneyFormatter.format(goal.savedInCents)} de ${MoneyFormatter.format(goal.goal.targetAmountInCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FinanceTextField(
                    amount,
                    { amount = sanitizeAmountInput(it); formState.clearError("amount") },
                    "Monto (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                    isError = formState.hasError("amount"),
                    errorMessage = formState["amount"],
                )
                if (showBreakdown) {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Desglose del aporte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Para la meta", style = MaterialTheme.typography.bodySmall)
                                Text(MoneyFormatter.format(remaining), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Excedente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                Text(MoneyFormatter.format(cents!! - remaining), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
                FinanceTextField(
                    description,
                    { description = it },
                    "Descripción (opcional)",
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Registrando…" else "Aportar",
                onClick = {
                    formState.clearAll()
                    val c = MoneyFormatter.parseToCents(amount)
                    if (c == null || c <= 0) formState.setError("amount", "Ingresa un monto válido")
                    if (!formState.isValid()) return@PrimaryButton
                    onConfirm(amount, description)
                },
                enabled = !isSaving && amount.isNotBlank(),
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContributionsSheet(
    goal: SavingsGoalProgress,
    contributions: List<FinanceTransaction>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(goal.goal.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${MoneyFormatter.format(goal.savedForGoalInCents)} en la meta" +
                            if (goal.excessInCents > 0) " · ${MoneyFormatter.format(goal.excessInCents)} excedente"
                            else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${contributions.size} " +
                            if (contributions.size == 1) "aporte" else "aportes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Close, "Cerrar aportes")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (contributions.isEmpty()) {
                Text(
                    "Todavía no has aportado a esta meta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(contributions, key = { it.id }) { contribution ->
                        ContributionRow(contribution)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(contribution: FinanceTransaction) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.angel.mony.ui.iconography.MonyIcon(
            com.angel.mony.ui.iconography.MonyIcon.Expense,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
        )
        Column(Modifier.weight(1f)) {
            Text(
                contribution.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                contribution.date.format(contributionDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "−${MoneyFormatter.format(contribution.amountInCents)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}


