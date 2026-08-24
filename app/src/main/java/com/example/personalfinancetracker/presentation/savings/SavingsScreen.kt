package com.example.personalfinancetracker.presentation.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onSettings: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalProgress?>(null) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalProgress?>(null) }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Ahorros") },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Nueva meta",
                        onClick = {
                            editingGoal = null
                            showEditor = true
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onClick = onSettings)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.goals.isEmpty()) {
                item {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Savings, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Todavía no tienes metas de ahorro", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Crea una meta y registra aportes para ver tu avance. Cada aporte se guarda como un gasto en la categoría \"Ahorro\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton("Crear la primera", {
                                editingGoal = null
                                showEditor = true
                            }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            items(state.goals, key = { it.goal.id }) { goal ->
                SavingsGoalCard(
                    progress = goal,
                    onContribute = { contributingGoal = goal },
                    onEdit = {
                        editingGoal = goal
                        showEditor = true
                    },
                    onDelete = { viewModel.requestDelete(goal) },
                )
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            existing = editingGoal,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { name, target ->
                val goal = editingGoal
                if (goal == null) {
                    viewModel.create(name, target) { showEditor = false }
                } else {
                    viewModel.update(goal.goal.id, name, target) { showEditor = false }
                }
            },
        )
    }

    contributingGoal?.let { goal ->
        ContributeDialog(
            goal = goal,
            isSaving = isSaving,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount, description ->
                viewModel.contribute(goal, amount, description) { contributingGoal = null }
            },
        )
    }

    pendingDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("¿Eliminar meta?") },
            text = {
                Text("${goal.goal.name} se eliminará. Los aportes ya registrados permanecerán en el historial como gastos normales.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalCard(
    progress: SavingsGoalProgress,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fraction =
        if (progress.goal.targetAmountInCents <= 0) 0f
        else progress.savedInCents.toFloat() / progress.goal.targetAmountInCents
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        progress.goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (progress.isCompleted) "¡Meta cumplida!"
                        else "${progress.percent}% del objetivo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (progress.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, "Editar ${progress.goal.name}", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Eliminar ${progress.goal.name}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        MoneyFormatter.format(progress.savedInCents),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "de ${MoneyFormatter.format(progress.goal.targetAmountInCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PrimaryButton(
                    text = "Aportar",
                    onClick = onContribute,
                )
            }
            GoalProgressBar(fraction.coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun GoalProgressBar(ratio: Float) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            Modifier.fillMaxWidth(ratio).height(8.dp)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
        )
    }
}

@Composable
private fun GoalEditorDialog(
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
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (existing == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
                FinanceTextField(
                    target,
                    { target = sanitizeAmountInput(it) },
                    "Objetivo (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Guardando…" else "Guardar",
                onClick = { onSave(name, target) },
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
private fun ContributeDialog(
    goal: SavingsGoalProgress,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var amount by remember(goal) { mutableStateOf("") }
    var description by remember(goal) { mutableStateOf("") }
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
                    { amount = sanitizeAmountInput(it) },
                    "Monto (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
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
                onClick = { onConfirm(amount, description) },
                enabled = !isSaving && amount.isNotBlank(),
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}
