package com.example.personalfinancetracker.presentation.pending

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.EntryCardSize
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingPeriodFilter
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.isPendingReminderInFuture
import com.example.personalfinancetracker.domain.model.isPendingDateValid
import com.example.personalfinancetracker.domain.model.label
import com.example.personalfinancetracker.domain.model.toTransactionType
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceDetailRow
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingEntriesScreen(
    onSettings: () -> Unit,
    viewModel: PendingEntriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        if (!granted) context.showToast("Activa las notificaciones para usar alertas")
    }
    var selectedType by remember { mutableStateOf(PendingType.PAYMENT) }
    var periodFilter by remember { mutableStateOf(PendingPeriodFilter.FORTNIGHT) }
    var query by remember { mutableStateOf("") }
    val cardSize by viewModel.cardSize.collectAsStateWithLifecycle()
    var editorEntry by remember { mutableStateOf<PendingEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingEntry?>(null) }
    var selectedEntry by remember { mutableStateOf<PendingEntry?>(null) }

    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO")) }

    val periodRange = remember(periodFilter) {
        when (periodFilter) {
            PendingPeriodFilter.FORTNIGHT -> DateRange.currentFortnight()
            PendingPeriodFilter.MONTH -> DateRange.currentMonth()
            PendingPeriodFilter.ALL -> null
        }
    }

    val visibleEntries = remember(state.entries, selectedType, periodRange, query, state.categories) {
        filterPendingEntries(state.entries, selectedType, periodRange, query, state.categories)
    }
    val pendingTotal = remember(visibleEntries) {
        visibleEntries.filterNot(PendingEntry::isDone).sumOf(PendingEntry::amountInCents)
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
                title = { ModuleTitle("Recordatorios") },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Nuevo recordatorio",
                        onClick = {
                            editorEntry = null
                            showEditor = true
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onClick = onSettings)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PendingTypeChip(PendingType.PAYMENT, selectedType, { selectedType = it }, Modifier.weight(1f))
                    PendingTypeChip(PendingType.COLLECTION, selectedType, { selectedType = it }, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PendingPeriodChip("Quincena", periodFilter == PendingPeriodFilter.FORTNIGHT, { periodFilter = PendingPeriodFilter.FORTNIGHT }, Modifier.weight(1f))
                    PendingPeriodChip("Mes", periodFilter == PendingPeriodFilter.MONTH, { periodFilter = PendingPeriodFilter.MONTH }, Modifier.weight(1f))
                    PendingPeriodChip("Todas", periodFilter == PendingPeriodFilter.ALL, { periodFilter = PendingPeriodFilter.ALL }, Modifier.weight(1f))
                }
            }
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
                    EntryCardSizeMenu(cardSize, viewModel::setCardSize)
                }
            }
            item {
                PendingSummaryCard(
                    type = selectedType,
                    filter = periodFilter,
                    range = periodRange,
                    total = pendingTotal,
                    count = visibleEntries.count { !it.isDone },
                    formatter = formatter,
                )
            }
            if (state.entries.isEmpty()) {
                item {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Todavía no hay recordatorios", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Guarda aquí las cosas que piensas pagar o cobrar y ponles la fecha. Aparecerán en la quincena o el mes que elijas.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton("Crear el primero", {
                                editorEntry = null
                                showEditor = true
                            }, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else if (visibleEntries.isEmpty()) {
                item {
                    Text(
                        "No encontramos recordatorios que coincidan con tu búsqueda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(visibleEntries, key = PendingEntry::id) { entry ->
                PendingEntryCard(
                    entry = entry,
                    category = state.categories[entry.categoryId],
                    size = cardSize,
                    formatter = formatter,
                    onToggleDone = { viewModel.toggleDone(entry) },
                    onEdit = {
                        editorEntry = entry
                        showEditor = true
                    },
                    onDelete = { pendingDelete = entry },
                    onSelect = { selectedEntry = entry },
                )
            }
        }
    }

    if (showEditor) {
        PendingEntryDialog(
            entry = editorEntry,
            initialType = editorEntry?.type ?: selectedType,
            categories = state.categories.values.toList(),
            isSaving = isSaving,
            notificationsGranted = notificationsGranted,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showEditor = false },
            onSave = { type, description, amount, categoryId, comment, date, reminderTime ->
                if (reminderTime != null && !notificationsGranted) {
                    context.showToast("Concede permiso de notificaciones o quita la alerta")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    return@PendingEntryDialog
                }
                viewModel.save(editorEntry, type, description, amount, categoryId, comment, date, reminderTime) {
                    selectedType = type
                    showEditor = false
                }
            },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("${entry.description} dejará de aparecer en Recordatorios. Los movimientos ya registrados no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry)
                    pendingDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }

    selectedEntry?.let { entry ->
        PendingEntryDetailsDialog(
            entry = entry,
            category = state.categories[entry.categoryId],
            formatter = formatter,
            onDismiss = { selectedEntry = null },
        )
    }
}

@Composable
private fun PendingEntryDetailsDialog(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onDismiss: () -> Unit,
) {
    val amountColor = pendingAmountColor(entry.type)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = {
            Icon(
                Icons.AutoMirrored.Outlined.PlaylistAdd,
                contentDescription = null,
                tint = amountColor,
            )
        },
        title = { Text("Detalle del recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    MoneyFormatter.format(entry.amountInCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = amountColor,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FinanceDetailRow("Tipo", entry.type.label(), valueColor = amountColor)
                FinanceDetailRow("Categoría", category?.name ?: "Sin categoría")
                FinanceDetailRow(
                    "Fecha",
                    entry.date.format(formatter) + (entry.reminderTime?.takeUnless { entry.isDone }
                        ?.let { " · Alerta ${it.format(reminderTimeFormatter)}" } ?: ""),
                )
                FinanceDetailRow("Estado", if (entry.isDone) "Hecho" else "Pendiente")
                entry.doneAt?.let {
                    FinanceDetailRow(
                        "Registrado",
                        it.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter),
                    )
                }
                entry.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "COMENTARIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { PrimaryButton("Cerrar", onDismiss) },
    )
}

@Composable
private fun PendingTypeChip(
    type: PendingType,
    selectedType: PendingType,
    onSelect: (PendingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selectedType == type,
        onClick = { onSelect(type) },
        label = { Text(if (type == PendingType.PAYMENT) "Pagos" else "Cobros") },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selectedType == type,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun PendingPeriodChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(label) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
        ),
    )
}

@Composable
private fun PendingSummaryCard(
    type: PendingType,
    filter: PendingPeriodFilter,
    range: DateRange?,
    total: Long,
    count: Int,
    formatter: DateTimeFormatter,
) {
    val periodTitle = when (filter) {
        PendingPeriodFilter.FORTNIGHT -> "QUINCENA ACTUAL"
        PendingPeriodFilter.MONTH -> "MES ACTUAL"
        PendingPeriodFilter.ALL -> "TODOS LOS RECORDATORIOS"
    }
    val periodSubtitle = range?.let {
        "${it.start.format(formatter)} – ${it.endInclusive.format(formatter)}"
    } ?: "Sin filtro de fecha"
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                periodTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                if (type == PendingType.PAYMENT) "POR PAGAR" else "POR COBRAR",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                MoneyFormatter.format(total),
                style = MaterialTheme.typography.headlineMedium,
                color = if (type == PendingType.PAYMENT) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
            Text(
                "$count recordatorio${if (count == 1) "" else "s"} · $periodSubtitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PendingEntryCard(
    entry: PendingEntry,
    category: Category?,
    size: EntryCardSize,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        when (size) {
            EntryCardSize.COMPACT ->
                PendingCompactCardContent(entry, category, formatter, onToggleDone, onEdit, onDelete)
            EntryCardSize.NORMAL ->
                PendingNormalCardContent(entry, category, formatter, onToggleDone, onEdit, onDelete)
            EntryCardSize.DETAILED ->
                PendingDetailedCardContent(entry, category, formatter, onToggleDone, onEdit, onDelete)
        }
    }
}

@Composable
private fun PendingCompactCardContent(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    category?.name ?: "Sin categoría",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!entry.comment.isNullOrBlank()) {
                Icon(
                    Icons.Outlined.Notes,
                    contentDescription = "Tiene comentario",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                MoneyFormatter.format(entry.amountInCents),
                style = MaterialTheme.typography.titleMedium,
                color = pendingAmountColor(entry.type),
            )
            PendingStatusBadge(entry)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                entry.date.format(formatter),
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleDone, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (entry.isDone) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.CheckCircle,
                    contentDescription = if (entry.isDone) "Reabrir" else "Marcar hecho",
                    tint = if (entry.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Edit, "Editar", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PendingNormalCardContent(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PendingCardHeader(entry, category)
        Text(
            MoneyFormatter.format(entry.amountInCents),
            style = MaterialTheme.typography.headlineMedium,
            color = pendingAmountColor(entry.type),
        )
        PendingCardDateRow(entry, formatter)
        entry.comment?.takeIf { it.isNotBlank() }?.let {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "COMENTARIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        PendingCardActionsRow(onToggleDone = onToggleDone, isDone = entry.isDone, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun PendingDetailedCardContent(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(entry.id, entry.updatedAt) { mutableStateOf(false) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PendingCardHeader(entry, category)
        Text(
            MoneyFormatter.format(entry.amountInCents),
            style = MaterialTheme.typography.headlineMedium,
            color = pendingAmountColor(entry.type),
        )
        PendingCardDateRow(entry, formatter)
        TextButton(onClick = { expanded = !expanded }) {
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(if (expanded) "Ver menos" else "Ver más", modifier = Modifier.padding(start = 6.dp))
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceDetailRow("Tipo", entry.type.label())
                FinanceDetailRow(
                    "Alerta",
                    entry.reminderTime?.takeUnless { entry.isDone }?.format(reminderTimeFormatter) ?: "Sin alerta",
                )
                entry.comment?.let { FinanceDetailRow("Comentario", it) }
                entry.doneAt?.let {
                    FinanceDetailRow(
                        "Registrado",
                        it.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter),
                    )
                }
                FinanceDetailRow("Creado", entry.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter))
            }
        }
        PendingCardActionsRow(onToggleDone = onToggleDone, isDone = entry.isDone, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun PendingCardHeader(entry: PendingEntry, category: Category?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.description, style = MaterialTheme.typography.titleLarge)
            Text(
                category?.name ?: "Sin categoría",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        PendingStatusBadge(entry)
    }
}

@Composable
private fun PendingCardDateRow(entry: PendingEntry, formatter: DateTimeFormatter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            entry.date.format(formatter) +
                (entry.reminderTime?.takeUnless { entry.isDone }?.let {
                    " · Alerta ${it.format(reminderTimeFormatter)}"
                } ?: "") +
                (entry.doneAt?.let { " · HECHO ${it.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.isDone) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PendingCardActionsRow(
    isDone: Boolean,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PrimaryButton(
            text = if (isDone) "Reabrir" else "Marcar hecho",
            onClick = onToggleDone,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun pendingAmountColor(type: PendingType): Color =
    if (type == PendingType.PAYMENT) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary

@Composable
private fun EntryCardSizeMenu(selected: EntryCardSize, onSelect: (EntryCardSize) -> Unit) {
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
private fun PendingStatusBadge(entry: PendingEntry) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (entry.isDone) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (entry.isDone) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.Undo,
                contentDescription = null,
                tint = if (entry.isDone) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (entry.isDone) "HECHO" else "PENDIENTE",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.isDone) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingEntryDialog(
    entry: PendingEntry?,
    initialType: PendingType,
    categories: List<Category>,
    isSaving: Boolean,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (PendingType, String, String, Long?, String, LocalDate, LocalTime?) -> Unit,
) {
    var type by remember(entry) { mutableStateOf(entry?.type ?: initialType) }
    var description by remember(entry) { mutableStateOf(entry?.description.orEmpty()) }
    var amount by remember(entry) {
        mutableStateOf(entry?.amountInCents?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var categoryId by remember(entry) { mutableStateOf(entry?.categoryId) }
    var comment by remember(entry) { mutableStateOf(entry?.comment.orEmpty()) }
    var date by remember(entry) { mutableStateOf(entry?.date ?: LocalDate.now()) }
    var reminderTime by remember(entry) { mutableStateOf(entry?.reminderTime) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(entry, categories) {
        mutableStateOf(categories.firstOrNull { it.id == entry?.categoryId }?.name.orEmpty())
    }
    val availableCategories = remember(categories, type) {
        categories.filter { it.type == type.toTransactionType() && it.isActive }.sortedBy(Category::name)
    }
    val matchingCategories = remember(availableCategories, categorySearch, categoryId) {
        if (categoryId != null) availableCategories
        else availableCategories.filter { it.name.contains(categorySearch.trim(), ignoreCase = true) }
    }

    LaunchedEffect(type) {
        if (availableCategories.none { it.id == categoryId }) {
            categoryId = null
            categorySearch = ""
        }
        categoryExpanded = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Nuevo recordatorio" else "Editar recordatorio") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PendingTypeChip(PendingType.PAYMENT, type, { type = it }, Modifier.weight(1f))
                        PendingTypeChip(PendingType.COLLECTION, type, { type = it }, Modifier.weight(1f))
                    }
                }
                item { FinanceTextField(description, { description = it }, "Descripción", singleLine = true) }
                item {
                    FinanceTextField(
                        amount,
                        { amount = sanitizeAmountInput(it) },
                        "Monto (RD$)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = AmountVisualTransformation,
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = categorySearch,
                            onValueChange = { value ->
                                categorySearch = value
                                categoryId = null
                                categoryExpanded = true
                            },
                            label = { Text("Buscar categoría") },
                            placeholder = { Text("Escribe o selecciona") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (categorySearch.isNotBlank() || categoryId != null) {
                                        IconButton(
                                            onClick = {
                                                categorySearch = ""
                                                categoryId = null
                                                categoryExpanded = true
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Limpiar categoría",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                                }
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            singleLine = true,
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            matchingCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categorySearch = category.name
                                        categoryId = category.id
                                        categoryExpanded = false
                                    },
                                )
                            }
                            if (matchingCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No se encontraron categorías") },
                                    onClick = {},
                                    enabled = false,
                                )
                            }
                        }
                    }
                }
                item {
                    PendingDateField(
                        label = "Fecha del pago o cobro",
                        value = date,
                        minimumDate = LocalDate.now(),
                        onValueChange = { date = it },
                    )
                }
                item {
                    PendingReminderField(
                        date = date,
                        value = reminderTime,
                        notificationsGranted = notificationsGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onValueChange = { reminderTime = it },
                    )
                }
                item { FinanceTextField(comment, { comment = it }, "Comentario", placeholder = "Opcional") }
            }
        },
        confirmButton = {
            PrimaryButton(
                if (isSaving) "Guardando…" else "Guardar",
                { onSave(type, description, amount, categoryId, comment, date, reminderTime) },
                enabled = !isSaving,
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingReminderField(
    date: LocalDate,
    value: LocalTime?,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onValueChange: (LocalTime?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var errorMessage by remember(date, value) { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) {
                if (!isPendingDateValid(date)) {
                    errorMessage = "Selecciona hoy o una fecha futura para activar la alerta"
                    return@clickable
                }
                if (!notificationsGranted) onRequestNotificationPermission()
                showPicker = true
            },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Alerta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(value?.format(reminderTimeFormatter) ?: "Sin alerta")
                if (value != null && !notificationsGranted) {
                    Text(
                        "Se necesita permiso de notificaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (value != null) {
                IconButton(onClick = { onValueChange(null) }) {
                    Icon(Icons.Outlined.NotificationsOff, "Quitar alerta")
                }
            } else {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = value?.hour ?: 8,
            initialMinute = value?.minute ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Hora de la alerta") },
            text = { TimePicker(pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime = LocalTime.of(pickerState.hour, pickerState.minute)
                    if (isPendingReminderInFuture(date, selectedTime)) {
                        errorMessage = null
                        onValueChange(selectedTime)
                        showPicker = false
                    } else {
                        errorMessage = "Elige una hora que todavía no haya pasado"
                        showPicker = false
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingDateField(
    label: String,
    value: LocalDate,
    minimumDate: LocalDate? = null,
    onValueChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(value.format(formatter))
            }
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (showPicker) {
        val selectableDates = remember(minimumDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minimumDate == null ||
                        LocalDate.ofEpochDay(utcTimeMillis / MILLIS_PER_DAY) >= minimumDate
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private val reminderTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))

private val searchDateLongFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))

private val searchDateNumericFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun filterPendingEntries(
    entries: List<PendingEntry>,
    type: PendingType,
    range: DateRange?,
    query: String,
    categories: Map<Long, Category>,
): List<PendingEntry> {
    val normalizedQuery = query.trim()
    val digitQuery = normalizedQuery.filter(Char::isDigit)
    return entries
        .filter { it.type == type }
        .filter { range == null || it.date in range.start..range.endInclusive }
        .filter {
            normalizedQuery.isEmpty() || it.matchesPendingQuery(normalizedQuery, digitQuery, categories)
        }
        .sortedWith(compareBy<PendingEntry> { it.isDone }.thenBy(PendingEntry::date))
}

private fun PendingEntry.matchesPendingQuery(
    query: String,
    digitQuery: String,
    categories: Map<Long, Category>,
): Boolean {
    val haystack = listOfNotNull(
        description,
        comment,
        categories[categoryId]?.name,
        type.label(),
        MoneyFormatter.format(amountInCents),
        date.format(searchDateLongFormatter),
        date.format(searchDateNumericFormatter),
        reminderTime?.format(reminderTimeFormatter),
    ).joinToString(" ")
    return haystack.contains(query, ignoreCase = true) ||
        (digitQuery.isNotEmpty() && amountInCents.toString().contains(digitQuery))
}
