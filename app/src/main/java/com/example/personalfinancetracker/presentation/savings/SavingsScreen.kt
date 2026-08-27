package com.example.personalfinancetracker.presentation.savings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.personalfinancetracker.domain.model.EntryCardSize
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.LoadingContent
import com.example.personalfinancetracker.presentation.components.SkeletonChip
import com.example.personalfinancetracker.presentation.components.SkeletonGoalCard
import com.example.personalfinancetracker.presentation.components.SkeletonHost
import com.example.personalfinancetracker.presentation.components.SkeletonTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val pendingComplete by viewModel.pendingComplete.collectAsStateWithLifecycle()
    val pendingReopen by viewModel.pendingReopen.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.selectedGoal.collectAsStateWithLifecycle()
    val contributions by viewModel.contributions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalProgress?>(null) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalProgress?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val cardSize by viewModel.cardSize.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var draftShowCompleted by remember { mutableStateOf(showCompleted) }

    val displayedGoals = remember(state.activeGoals, state.completedGoals, showCompleted, query) {
        val goals = if (showCompleted) state.completedGoals else state.activeGoals
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) goals
        else goals.filter { it.goal.name.contains(normalizedQuery, ignoreCase = true) }
    }

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
                    if (!showCompleted) {
                        GlobalOutlinedIconButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = "Nueva meta",
                            onClick = {
                                editingGoal = null
                                showEditor = true
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                    }
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
        SkeletonHost(isLoading = !state.isReady) {
            LoadingContent(
                isLoading = !state.isReady,
                modifier = Modifier.padding(padding),
                skeleton = { SavingsSkeleton() },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            item {
                SavingsFilterButton(
                    showCompleted = showCompleted,
                    activeCount = state.activeGoals.size,
                    completedCount = state.completedGoals.size,
                    onClick = {
                        draftShowCompleted = showCompleted
                        showFilters = true
                    },
                )
            }
            if (state.goals.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FinanceTextField(
                            query,
                            { query = it },
                            "Buscar",
                            modifier = Modifier.weight(1f),
                            placeholder = "Buscar...",
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Limpiar búsqueda",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            },
                        )
                        SavingsCardSizeMenu(cardSize, viewModel::setCardSize)
                    }
                }
            }
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
            } else if (displayedGoals.isEmpty()) {
                item {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (showCompleted) "No tienes metas completadas aún."
                                else "No hay metas activas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(displayedGoals, key = { it.goal.id }) { goal ->
                SavingsGoalCard(
                    progress = goal,
                    size = cardSize,
                    onOpen = { viewModel.openContributions(goal) },
                    onContribute = { contributingGoal = goal },
                    onComplete = { viewModel.requestComplete(goal) },
                    onReopen = { viewModel.requestReopen(goal) },
                    onEdit = {
                        editingGoal = goal
                        showEditor = true
                    },
                     onDelete = { viewModel.requestDelete(goal) },
                 )
             }
                }
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

    selectedGoal?.let { goal ->
        ContributionsSheet(
            goal = goal,
            contributions = contributions,
            onDismiss = viewModel::closeContributions,
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

    pendingComplete?.let { goal ->
        AlertDialog(
            onDismissRequest = viewModel::cancelComplete,
            icon = {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("¿Finalizar esta meta?") },
            text = {
                Text("La meta \"${goal.goal.name}\" será movida a Completadas.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmComplete) {
                    Text("Finalizar")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelComplete) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }

    pendingReopen?.let { goal ->
        AlertDialog(
            onDismissRequest = viewModel::cancelReopen,
            icon = {
                Icon(
                    Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            },
            title = { Text("¿Reabrir esta meta?") },
            text = {
                Text("La meta \"${goal.goal.name}\" volverá a Activas.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReopen) {
                    Text("Reabrir")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelReopen) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }

    if (showFilters) {
        SavingsFilterSheet(
            draftShowCompleted = draftShowCompleted,
            onDraftChange = { draftShowCompleted = it },
            activeCount = state.activeGoals.size,
            completedCount = state.completedGoals.size,
            onClear = { draftShowCompleted = false },
            onApply = {
                showCompleted = draftShowCompleted
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SavingsFilterButton(
    showCompleted: Boolean,
    activeCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
) {
    val label = if (showCompleted) "Completadas ($completedCount)" else "Activas ($activeCount)"
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Icon(Icons.Outlined.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.Start) {
            Text("FILTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsFilterSheet(
    draftShowCompleted: Boolean,
    onDraftChange: (Boolean) -> Unit,
    activeCount: Int,
    completedCount: Int,
    onClear: () -> Unit,
    onApply: () -> Unit,
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
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FILTROS", style = MaterialTheme.typography.headlineMedium)
                    Text("Filtra por estado de la meta", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar filtros") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("ESTADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !draftShowCompleted,
                    onClick = { onDraftChange(false) },
                    label = { Text("Activas ($activeCount)") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                )
                FilterChip(
                    selected = draftShowCompleted,
                    onClick = { onDraftChange(true) },
                    label = { Text("Completadas ($completedCount)") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                PrimaryButton("Aplicar", onApply, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SavingsSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(52.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)) }
        item { SkeletonTextField() }
        items(4) { SkeletonGoalCard() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalCard(
    progress: SavingsGoalProgress,
    size: EntryCardSize,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        when (size) {
            EntryCardSize.COMPACT ->
                SavingsCompactCardContent(progress, onOpen, onContribute, onComplete, onReopen, onEdit, onDelete)
            EntryCardSize.NORMAL ->
                SavingsNormalCardContent(progress, onOpen, onContribute, onComplete, onReopen, onEdit, onDelete)
            EntryCardSize.DETAILED ->
                SavingsDetailedCardContent(progress, onOpen, onContribute, onComplete, onReopen, onEdit, onDelete)
        }
    }
}

@Composable
private fun SavingsCompactCardContent(
    progress: SavingsGoalProgress,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fraction =
        if (progress.goal.targetAmountInCents <= 0) 0f
        else progress.savedInCents.toFloat() / progress.goal.targetAmountInCents
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    progress.goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (progress.goal.completedAt != null) "Completada"
                    else if (progress.isCompleted) "¡Meta cumplida!"
                    else "${progress.percent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.goal.completedAt != null || progress.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                MoneyFormatter.format(progress.savedInCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (progress.canComplete) {
                IconButton(onClick = onComplete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Check, contentDescription = "Finalizar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (progress.goal.completedAt != null) {
                IconButton(onClick = onReopen, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Savings, contentDescription = "Reabrir", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }
            if (progress.isActive) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, "Editar", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
        GoalProgressBar(fraction.coerceAtMost(1f))
    }
}

@Composable
private fun SavingsNormalCardContent(
    progress: SavingsGoalProgress,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fraction =
        if (progress.goal.targetAmountInCents <= 0) 0f
        else progress.savedInCents.toFloat() / progress.goal.targetAmountInCents
    val excessFraction =
        if (progress.excessInCents > 0 && progress.goal.targetAmountInCents > 0)
            (progress.excessInCents.toFloat() / progress.goal.targetAmountInCents).coerceAtMost(1f)
        else 0f
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
                    if (progress.goal.completedAt != null) "Completada"
                    else if (progress.isCompleted) "¡Meta cumplida!"
                    else "${progress.percent}% del objetivo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.goal.completedAt != null || progress.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                )
            }
            if (progress.canComplete) {
                IconButton(onClick = onComplete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Check, "Finalizar ${progress.goal.name}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (progress.goal.completedAt != null) {
                IconButton(onClick = onReopen, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Savings, contentDescription = "Reabrir ${progress.goal.name}", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }
            if (progress.isActive) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, "Editar ${progress.goal.name}", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${progress.goal.name}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
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
                if (progress.excessInCents > 0) {
                    Text(
                        "Excedente: ${MoneyFormatter.format(progress.excessInCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            PrimaryButton(text = "Aportar", onClick = onContribute)
        }
        GoalProgressBar(fraction.coerceAtMost(1f), excessFraction)
    }
}

@Composable
private fun SavingsDetailedCardContent(
    progress: SavingsGoalProgress,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fraction =
        if (progress.goal.targetAmountInCents <= 0) 0f
        else progress.savedInCents.toFloat() / progress.goal.targetAmountInCents
    val excessFraction =
        if (progress.excessInCents > 0 && progress.goal.targetAmountInCents > 0)
            (progress.excessInCents.toFloat() / progress.goal.targetAmountInCents).coerceAtMost(1f)
        else 0f
    val timingFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO")) }
    val zone = remember { ZoneId.systemDefault() }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    progress.goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (progress.goal.completedAt != null) "Completada"
                    else if (progress.isCompleted) "¡Meta cumplida!"
                    else "${progress.percent}% del objetivo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.goal.completedAt != null || progress.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                )
            }
            if (progress.canComplete) {
                IconButton(onClick = onComplete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Check, "Finalizar ${progress.goal.name}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (progress.goal.completedAt != null) {
                IconButton(onClick = onReopen, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Savings, contentDescription = "Reabrir ${progress.goal.name}", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
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
                if (progress.excessInCents > 0) {
                    Text(
                        "Excedente: ${MoneyFormatter.format(progress.excessInCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        GoalProgressBar(fraction.coerceAtMost(1f), excessFraction)
        progress.goal.createdAt?.let { createdAt ->
            SavingsDetailRow("Creada", createdAt.atZone(zone).format(timingFormatter))
        }
        progress.goal.completedAt?.let { completedAt ->
            SavingsDetailRow("Completada", completedAt.atZone(zone).format(timingFormatter))
        }
        val remaining = (progress.goal.targetAmountInCents - progress.savedInCents).coerceAtLeast(0)
        SavingsDetailRow("Restante", MoneyFormatter.format(remaining))
        SavingsDetailRow("Objetivo", MoneyFormatter.format(progress.goal.targetAmountInCents))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (progress.isActive) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, "Editar ${progress.goal.name}", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${progress.goal.name}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Aportar", onClick = onContribute)
        }
    }
}

@Composable
private fun GoalProgressBar(ratio: Float, excessRatio: Float = 0f) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            Modifier.fillMaxWidth(ratio).height(8.dp)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
        )
        if (excessRatio > 0f) {
            Box(
                Modifier.fillMaxWidth(excessRatio).height(8.dp)
                    .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}

@Composable
private fun SavingsDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SavingsCardSizeMenu(selected: EntryCardSize, onSelect: (EntryCardSize) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.ViewAgenda, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(selected.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EntryCardSize.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (selected == option) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
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
                    { amount = sanitizeAmountInput(it) },
                    "Monto (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
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
                onClick = { onConfirm(amount, description) },
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
private fun ContributionsSheet(
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
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar aportes")
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
        Icon(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
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

private val contributionDateFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))
