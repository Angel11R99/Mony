package com.angel.mony.presentation.fixed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.EntryCardSize
import com.angel.mony.domain.model.FixedDateMode
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.FixedScheduleMode
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.previousFortnightEnd
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.LoadingContent
import com.angel.mony.presentation.components.SkeletonBox
import com.angel.mony.presentation.components.SkeletonChip
import com.angel.mony.presentation.components.SkeletonEntryCard
import com.angel.mony.presentation.components.SkeletonHost
import com.angel.mony.presentation.components.SkeletonLine
import com.angel.mony.presentation.components.SkeletonTone
import com.angel.mony.presentation.components.SkeletonTextField
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.ZoneId
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedEntriesScreen(
    onSettings: () -> Unit,
    viewModel: FixedEntriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var query by rememberSaveable { mutableStateOf("") }
    val cardSize by viewModel.cardSize.collectAsStateWithLifecycle()
    var editorEntry by remember { mutableStateOf<FixedEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FixedEntry?>(null) }
    var configEntry by remember { mutableStateOf<FixedEntry?>(null) }
    var manualEntry by remember { mutableStateOf<FixedEntry?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var draftType by remember { mutableStateOf(selectedType) }
    val visibleEntries = remember(state.entries, selectedType, query, state.categories) {
        filterFixedEntries(state.entries, selectedType, query, state.categories)
    }
    val typeCounts = remember(state.entries, query, state.categories) {
        val base = filterFixedEntries(state.entries, TransactionType.EXPENSE, query, state.categories).size to
            filterFixedEntries(state.entries, TransactionType.INCOME, query, state.categories).size
        base
    }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Fijos") },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Nueva plantilla",
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
        SkeletonHost(isLoading = !state.isReady) {
            LoadingContent(
                isLoading = !state.isReady,
                modifier = Modifier.padding(padding),
                skeleton = { FixedEntriesSkeleton() },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            item {
                FixedFilterButton(
                    selectedType = selectedType,
                    counts = typeCounts,
                    onClick = {
                        draftType = selectedType
                        showFilters = true
                    },
                )
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
                Text(
                    if (selectedType == TransactionType.EXPENSE) "GASTOS RECURRENTES" else "INGRESOS RECURRENTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (state.entries.isEmpty()) {
                item {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Todavía no tienes plantillas", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Guarda aquí los movimientos que repites y agrégalos con la fecha que elijas.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton("Crear la primera", {
                                editorEntry = null
                                showEditor = true
                            }, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else if (visibleEntries.isEmpty()) {
                item {
                    Text(
                        "No encontramos plantillas que coincidan con tu búsqueda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(visibleEntries, key = FixedEntry::id) { entry ->
                FixedEntryCard(
                    entry = entry,
                    category = state.categories[entry.categoryId],
                    size = cardSize,
                    onToggle = { viewModel.toggle(entry) },
                    onAddNow = { manualEntry = entry },
                    onConfigure = { configEntry = entry },
                    onEdit = {
                        editorEntry = entry
                        showEditor = true
                    },
                    onDelete = { pendingDelete = entry },
                )
            }
                }
            }
        }
    }

    if (showEditor) {
        FixedEntryDialog(
            entry = editorEntry,
            initialType = editorEntry?.type ?: selectedType,
            categories = state.categories.values.toList(),
            onDismiss = { showEditor = false },
            onSave = { type, description, amount, categoryId, comment, active ->
                viewModel.save(editorEntry, type, description, amount, categoryId, comment, active) {
                    selectedType = type
                    showEditor = false
                }
            },
        )
    }

    configEntry?.let { entry ->
        FixedEntryConfigurationDialog(
            entry = entry,
            onDismiss = { configEntry = null },
            onSave = { scheduleMode, hour, scheduleDate ->
                viewModel.configure(
                    entry = entry,
                    scheduleMode = scheduleMode,
                    scheduleHour = hour,
                    scheduleSpecificDate = scheduleDate,
                ) { configEntry = null }
            },
        )
    }

    manualEntry?.let { entry ->
        FixedEntryManualActionsDialog(
            entry = entry,
            onDismiss = { manualEntry = null },
            onAdd = { date ->
                viewModel.addNow(entry, date)
                manualEntry = null
            },
        )
    }

    if (showFilters) {
        FixedFilterSheet(
            draftType = draftType,
            onTypeChange = { draftType = it },
            counts = typeCounts,
            onClear = { draftType = TransactionType.EXPENSE },
            onApply = {
                selectedType = draftType
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar plantilla?") },
            text = { Text("${entry.description} dejará de aparecer en Fijos. Los movimientos ya agregados no se borrarán.") },
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
}

@Composable
private fun TypeChip(
    type: TransactionType,
    selectedType: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selectedType == type,
        onClick = { onSelect(type) },
        label = { Text(if (type == TransactionType.EXPENSE) "Gastos" else "Ingresos") },
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
private fun FixedEntryCard(
    entry: FixedEntry,
    category: Category?,
    size: EntryCardSize,
    onToggle: () -> Unit,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        when (size) {
            EntryCardSize.COMPACT ->
                FixedCompactCardContent(entry, category, onToggle, onAddNow, onConfigure, onEdit, onDelete)
            EntryCardSize.NORMAL ->
                FixedNormalCardContent(entry, category, onToggle, onAddNow, onConfigure, onEdit, onDelete)
            EntryCardSize.DETAILED ->
                FixedDetailedCardContent(entry, category, onToggle, onAddNow, onConfigure, onEdit, onDelete)
        }
    }
}

@Composable
private fun fixedAmountColor(type: TransactionType): Color =
    if (type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary

@Composable
private fun FixedCompactCardContent(
    entry: FixedEntry,
    category: Category?,
    onToggle: () -> Unit,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
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
            Text(
                MoneyFormatter.format(entry.amountInCents),
                style = MaterialTheme.typography.titleMedium,
                color = fixedAmountColor(entry.type),
            )
            Switch(checked = entry.isActive, onCheckedChange = { onToggle() })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                compactTimingText(entry),
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddNow, enabled = entry.isActive, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Agregar ahora",
                    tint = if (entry.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onConfigure, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Settings, "Configurar fechas", modifier = Modifier.size(20.dp))
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
private fun FixedNormalCardContent(
    entry: FixedEntry,
    category: Category?,
    onToggle: () -> Unit,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.description, style = MaterialTheme.typography.titleLarge)
                Text(
                    category?.name ?: "Sin categoría",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Switch(checked = entry.isActive, onCheckedChange = { onToggle() })
        }
        Text(
            MoneyFormatter.format(entry.amountInCents),
            style = MaterialTheme.typography.headlineMedium,
            color = fixedAmountColor(entry.type),
        )
        entry.comment?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FixedEntryTimingStatus(entry)
        FixedEntryActionsRow(entry, onAddNow, onConfigure, onEdit, onDelete)
    }
}

@Composable
private fun FixedDetailedCardContent(
    entry: FixedEntry,
    category: Category?,
    onToggle: () -> Unit,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(entry.id, entry.lastAddedAt, entry.nextRunAt) { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.description, style = MaterialTheme.typography.titleLarge)
                Text(
                    category?.name ?: "Sin categoría",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Switch(checked = entry.isActive, onCheckedChange = { onToggle() })
        }
        Text(
            MoneyFormatter.format(entry.amountInCents),
            style = MaterialTheme.typography.headlineMedium,
            color = fixedAmountColor(entry.type),
        )
        FixedEntryTimingStatus(entry)
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
                FinanceDetailRow("Tipo", if (entry.type == TransactionType.EXPENSE) "Gasto" else "Ingreso")
                FinanceDetailRow("Programación", scheduleModeLabel(entry.scheduleMode))
                if (entry.scheduleMode != FixedScheduleMode.MANUAL) {
                    FinanceDetailRow("Hora programada", formatScheduleHour(entry.scheduleHour))
                }
                entry.scheduleSpecificDate?.let {
                    FinanceDetailRow("Fecha específica", it.format(timingDateFormatter))
                }
                FinanceDetailRow("Fecha al agregar", manualDateModeLabel(entry.manualDateMode))
                entry.lastAddedDate?.let {
                    FinanceDetailRow("Última fecha agregada", it.format(timingDateFormatter))
                }
                entry.comment?.let { FinanceDetailRow("Comentario", it) }
            }
        }
        FixedEntryActionsRow(entry, onAddNow, onConfigure, onEdit, onDelete)
    }
}

@Composable
private fun FixedEntryActionsRow(
    entry: FixedEntry,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PrimaryButton(
            text = when {
                entry.isActive -> "Agregar"
                entry.lastAddedAt != null -> "Agregado"
                else -> "Inactivo"
            },
            onClick = onAddNow,
            modifier = Modifier.weight(1f),
            enabled = entry.isActive,
        )
        IconButton(onClick = onConfigure) { Icon(Icons.Outlined.Settings, "Configurar fechas") }
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun compactTimingText(entry: FixedEntry): String {
    val zone = ZoneId.systemDefault()
    return when {
        !entry.isActive -> "INACTIVA"
        entry.nextRunAt != null -> "PRÓXIMO · ${entry.nextRunAt.atZone(zone).format(timingDateTimeFormatter)}"
        entry.lastAddedAt != null -> "ÚLTIMO AGREGADO · ${entry.lastAddedAt.atZone(zone).format(timingDateTimeFormatter)}"
        else -> "SIN PROGRAMACIÓN"
    }
}

private fun formatScheduleHour(hour: Int): String =
    LocalDate.now().atTime(hour.coerceIn(0, 23), 0).format(scheduleHourFormatter)

private fun manualDateModeLabel(mode: FixedDateMode): String = when (mode) {
    FixedDateMode.TODAY -> "Hoy"
    FixedDateMode.PREVIOUS_FORTNIGHT -> "Quincena anterior"
    FixedDateMode.PREVIOUS_MONTH -> "Mes anterior"
    FixedDateMode.SPECIFIC_DATE -> "Fecha específica"
}

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
private fun FixedEntryTimingStatus(entry: FixedEntry) {
    val zone = remember { ZoneId.systemDefault() }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        entry.nextRunAt?.let {
            Text(
                "PRÓXIMO · ${it.atZone(zone).format(timingDateTimeFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        entry.lastAddedAt?.let {
            Text(
                "ÚLTIMO AGREGADO · ${it.atZone(zone).format(timingDateTimeFormatter)}" +
                    (entry.lastAddedDate?.let { date -> " · FECHA ${date.format(timingDateFormatter)}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedEntryManualActionsDialog(
    entry: FixedEntry,
    onDismiss: () -> Unit,
    onAdd: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar ${entry.description}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Elige la fecha. Al tocar una opción, el movimiento se agrega de inmediato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = "Hoy",
                    onClick = { onAdd(today) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Quincena anterior",
                    onClick = { onAdd(previousFortnightEnd(today)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Mes anterior",
                    onClick = { onAdd(YearMonth.from(today).minusMonths(1).atEndOfMonth()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Fecha específica",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = today.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onAdd(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    showDatePicker = false
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FixedEntryConfigurationDialog(
    entry: FixedEntry,
    onDismiss: () -> Unit,
    onSave: (FixedScheduleMode, Int, LocalDate?) -> Unit,
) {
    var scheduleMode by remember(entry) { mutableStateOf(entry.scheduleMode) }
    var scheduleDate by remember(entry) { mutableStateOf(entry.scheduleSpecificDate) }
    var scheduleHour by remember(entry) { mutableStateOf(entry.scheduleHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programación automática") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "PROGRAMACIÓN AUTOMÁTICA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FixedScheduleMode.entries.forEach { option ->
                            ConfigChip(
                                selected = scheduleMode == option,
                                text = scheduleModeLabel(option),
                                onClick = { scheduleMode = option },
                            )
                        }
                    }
                }
                if (scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME) {
                    item {
                        FixedDateField(
                            label = "Fecha programada",
                            value = scheduleDate,
                            onValueChange = { scheduleDate = it },
                        )
                    }
                }
                if (scheduleMode != FixedScheduleMode.MANUAL) {
                    item {
                        FixedHourField(
                            hour = scheduleHour,
                            onHourChange = { scheduleHour = it },
                        )
                    }
                    item {
                        Text(
                            if (scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME) {
                                "Al registrarse se desactivará automáticamente."
                            } else {
                                "Se repetirá y mostrará la próxima ejecución en la tarjeta."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton("Guardar", {
                onSave(scheduleMode, scheduleHour, scheduleDate)
            })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun ConfigChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedDateField(label: String, value: LocalDate?, onValueChange: (LocalDate) -> Unit) {
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
                Text(value?.format(formatter) ?: "Seleccionar fecha")
            }
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).toEpochDay() * MILLIS_PER_DAY,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedHourField(hour: Int, onHourChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val display = remember(hour) {
        LocalDate.now().atTime(hour.coerceIn(0, 23), 0)
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO")))
    }
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
                Text("Hora aproximada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(display)
            }
            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (showPicker) {
        val timeState = rememberTimePickerState(initialHour = hour.coerceIn(0, 23), initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Elegir hora") },
            text = { TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onHourChange(timeState.hour)
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

private fun scheduleModeLabel(mode: FixedScheduleMode): String = when (mode) {
    FixedScheduleMode.MANUAL -> "Sin programación"
    FixedScheduleMode.AFTER_FORTNIGHT -> "Después de cada quincena"
    FixedScheduleMode.AFTER_MONTH -> "Después de cada mes"
    FixedScheduleMode.SPECIFIC_DATE_TIME -> "Fecha específica"
}

private const val MILLIS_PER_DAY = 86_400_000L

private val timingDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.forLanguageTag("es-DO"))

private val timingDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))

private val scheduleHourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))

private val searchDateNumericFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun filterFixedEntries(
    entries: List<FixedEntry>,
    type: TransactionType,
    query: String,
    categories: Map<Long, Category>,
): List<FixedEntry> {
    val normalizedQuery = query.trim()
    val digitQuery = normalizedQuery.filter(Char::isDigit)
    return entries
        .filter { it.type == type }
        .filter {
            normalizedQuery.isEmpty() || it.matchesFixedQuery(normalizedQuery, digitQuery, categories)
        }
}

private fun FixedEntry.matchesFixedQuery(
    query: String,
    digitQuery: String,
    categories: Map<Long, Category>,
): Boolean {
    val haystack = listOfNotNull(
        description,
        comment,
        categories[categoryId]?.name,
        if (type == TransactionType.EXPENSE) "Gastos" else "Ingresos",
        MoneyFormatter.format(amountInCents),
        scheduleModeLabel(scheduleMode),
        scheduleSpecificDate?.format(searchDateNumericFormatter),
        lastAddedDate?.format(searchDateNumericFormatter),
    ).joinToString(" ")
    return haystack.contains(query, ignoreCase = true) ||
        (digitQuery.isNotEmpty() && amountInCents.toString().contains(digitQuery))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedEntryDialog(
    entry: FixedEntry?,
    initialType: TransactionType,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String, String, Long?, String, Boolean) -> Unit,
) {
    var type by remember(entry) { mutableStateOf(entry?.type ?: initialType) }
    var description by remember(entry) { mutableStateOf(entry?.description.orEmpty()) }
    var amount by remember(entry) {
        mutableStateOf(entry?.amountInCents?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var categoryId by remember(entry) { mutableStateOf(entry?.categoryId) }
    var comment by remember(entry) { mutableStateOf(entry?.comment.orEmpty()) }
    var active by remember(entry) { mutableStateOf(entry?.isActive ?: true) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(entry, categories) {
        mutableStateOf(categories.firstOrNull { it.id == entry?.categoryId }?.name.orEmpty())
    }
    val availableCategories = categories.filter { it.type == type && it.isActive }.sortedBy(Category::name)
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
        title = { Text(if (entry == null) "Nueva plantilla" else "Editar plantilla") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeChip(TransactionType.EXPENSE, type, { type = it }, Modifier.weight(1f))
                        TypeChip(TransactionType.INCOME, type, { type = it }, Modifier.weight(1f))
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
                item { FinanceTextField(comment, { comment = it }, "Comentario", placeholder = "Opcional") }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Plantilla activa", fontWeight = FontWeight.SemiBold)
                            Text("Permite agregarla al historial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton("Guardar", { onSave(type, description, amount, categoryId, comment, active) })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun FixedFilterButton(
    selectedType: TransactionType,
    counts: Pair<Int, Int>,
    onClick: () -> Unit,
) {
    val label = if (selectedType == TransactionType.EXPENSE) "Gastos (${counts.first})" else "Ingresos (${counts.second})"
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
private fun FixedFilterSheet(
    draftType: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    counts: Pair<Int, Int>,
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
                    Text("Filtra por tipo de plantilla", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar filtros") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("TIPO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(TransactionType.EXPENSE, draftType, onTypeChange, Modifier.weight(1f))
                TypeChip(TransactionType.INCOME, draftType, onTypeChange, Modifier.weight(1f))
            }
            Text("Gastos: ${counts.first} · Ingresos: ${counts.second}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                PrimaryButton("Aplicar", onApply, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FixedEntriesSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 52.dp), MaterialTheme.shapes.small) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonTextField(Modifier.weight(1f))
                SkeletonBox(
                    Modifier.size(width = 56.dp, height = 56.dp),
                    MaterialTheme.shapes.small,
                )
            }
        }
        item {
            SkeletonLine(Modifier.width(150.dp), height = 11.dp, tone = SkeletonTone.Accent)
        }
        items(4) { SkeletonEntryCard(showProgress = false) }
    }
}
