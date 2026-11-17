package com.angel.mony.presentation.pending

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.EntryCardSize
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.PendingPeriodFilter
import com.angel.mony.domain.model.PendingType
import com.angel.mony.domain.model.isPendingReminderInFuture
import com.angel.mony.domain.model.isPendingDateValid
import com.angel.mony.domain.model.label
import com.angel.mony.domain.model.toTransactionType
import com.angel.mony.presentation.components.EntryCardSizeMenu
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.LoadingContent
import com.angel.mony.presentation.components.SkeletonBox
import com.angel.mony.presentation.components.SkeletonCard
import com.angel.mony.presentation.components.SkeletonChip
import com.angel.mony.presentation.components.SkeletonEntryCard
import com.angel.mony.presentation.components.SkeletonHost
import com.angel.mony.presentation.components.SkeletonLine
import com.angel.mony.presentation.components.SkeletonTextField
import com.angel.mony.presentation.components.StaggeredReveal
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingEntriesScreen(
    onSettings: () -> Unit,
    onOpenList: (String) -> Unit,
    viewModel: PendingEntriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val lastDraft by viewModel.lastDraft.collectAsStateWithLifecycle()
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
    var selectedType by rememberSaveable { mutableStateOf(PendingType.PAYMENT) }
    var periodFilter by rememberSaveable { mutableStateOf(PendingPeriodFilter.FORTNIGHT) }
    var statusFilter by rememberSaveable { mutableStateOf(PendingStatusFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var draftType by remember { mutableStateOf(selectedType) }
    var draftPeriod by remember { mutableStateOf(periodFilter) }
    var draftStatus by remember { mutableStateOf(statusFilter) }
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

    val visibleEntries = remember(state.entries, selectedType, periodRange, statusFilter, query, state.categories) {
        filterPendingEntries(state.entries, selectedType, periodRange, query, state.categories, statusFilter)
    }
    val pendingTotal = remember(visibleEntries) {
        visibleEntries.filterNot(PendingEntry::isDone).sumOf(PendingEntry::amountInCents)
    }
    val statusCounts = remember(state.entries, selectedType, periodRange, query, state.categories) {
        val baseFiltered = filterPendingEntries(
            state.entries, selectedType, periodRange, query, state.categories, PendingStatusFilter.ALL
        )
        Triple(
            baseFiltered.size,
            baseFiltered.count { !it.isDone },
            baseFiltered.count { it.isDone },
        )
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
                title = { ModuleTitle("Recordatorios") },
                actions = {
                    GlobalOutlinedIconButton(
                        semanticIcon = com.angel.mony.ui.iconography.MonyIcon.Add,
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
        SkeletonHost(isLoading = !state.isReady) {
            LoadingContent(
                isLoading = !state.isReady,
                modifier = Modifier.padding(padding),
                skeleton = { PendingEntriesSkeleton() },
            ) {
                StaggeredReveal(
                    screenKey = "pending",
                    modifier = Modifier.fillMaxSize(),
                    staggerMillis = 40,
                    durationMillis = 180,
                    slideOffsetDp = 8.dp,
                ) {
                    add {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
            item {
                PendingFilterButton(
                    type = selectedType,
                    period = periodFilter,
                    status = statusFilter,
                    statusCounts = statusCounts,
                    onClick = {
                        draftType = selectedType
                        draftPeriod = periodFilter
                        draftStatus = statusFilter
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
                        leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Search, null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    com.angel.mony.ui.iconography.MonyIcon(
                                        com.angel.mony.ui.iconography.MonyIcon.Close,
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
                            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Add, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
                            Text("Todavía no hay recordatorios", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Guarda aquí las cosas que piensas pagar o cobrar y ponles la fecha. Aparecerán en el ciclo o el mes que elijas.",
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
                    onOpenList = onOpenList,
                     onDelete = { pendingDelete = entry },
                     onSelect = { selectedEntry = entry },
                  )
             }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        PendingEntryDialog(
            entry = editorEntry,
            initialType = editorEntry?.type ?: lastDraft.type,
            initialCategoryId = if (editorEntry == null) lastDraft.categoryId else null,
            initialDate = if (editorEntry == null) lastDraft.date else null,
            initialReminderTime = if (editorEntry == null) lastDraft.reminderTime else null,
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

    if (showFilters) {
        PendingFilterSheet(
            draftType = draftType,
            onTypeChange = { draftType = it },
            draftPeriod = draftPeriod,
            onPeriodChange = { draftPeriod = it },
            draftStatus = draftStatus,
            onStatusChange = { draftStatus = it },
            statusCounts = statusCounts,
            onClear = {
                draftType = PendingType.PAYMENT
                draftPeriod = PendingPeriodFilter.ALL
                draftStatus = PendingStatusFilter.ALL
            },
            onApply = {
                selectedType = draftType
                periodFilter = draftPeriod
                statusFilter = draftStatus
                showFilters = false
            },
            onDismiss = { showFilters = false },
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
private fun PendingEntriesSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 52.dp), MaterialTheme.shapes.small)
        }
        item { SkeletonTextField() }
        item {
            SkeletonCard(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(Modifier.width(120.dp), height = 11.dp)
                SkeletonLine(Modifier.width(90.dp), height = 15.dp)
                SkeletonLine(Modifier.width(130.dp), height = 26.dp)
                SkeletonLine(Modifier.fillMaxWidth(0.65f), height = 11.dp)
            }
        }
        items(3) { SkeletonEntryCard(showProgress = false) }
    }
}

@Composable
internal fun PendingTypeChip(
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
private fun PendingStatusChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(label, maxLines = 1) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

enum class PendingStatusFilter(val label: String) {
    ALL("Todos"),
    PENDING("Pendientes"),
    DONE("Hechos"),
}

@Composable
private fun PendingFilterButton(
    type: PendingType,
    period: PendingPeriodFilter,
    status: PendingStatusFilter,
    statusCounts: Triple<Int, Int, Int>,
    onClick: () -> Unit,
) {
    val typeLabel = if (type == PendingType.PAYMENT) "Pagos" else "Cobros"
    val periodLabel = when (period) {
        PendingPeriodFilter.FORTNIGHT -> "Ciclo"
        PendingPeriodFilter.MONTH -> "Mes"
        PendingPeriodFilter.ALL -> "Todas"
    }
    val statusLabel = when (status) {
        PendingStatusFilter.ALL -> "Todos (${statusCounts.first})"
        PendingStatusFilter.PENDING -> "Pendientes (${statusCounts.second})"
        PendingStatusFilter.DONE -> "Hechos (${statusCounts.third})"
    }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        com.angel.mony.ui.iconography.MonyIcon(
            com.angel.mony.ui.iconography.MonyIcon.Filter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.Start) {
            Text("FILTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("$typeLabel · $periodLabel · $statusLabel", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        com.angel.mony.ui.iconography.MonyIcon(
            com.angel.mony.ui.iconography.MonyIcon.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingFilterSheet(
    draftType: PendingType,
    onTypeChange: (PendingType) -> Unit,
    draftPeriod: PendingPeriodFilter,
    onPeriodChange: (PendingPeriodFilter) -> Unit,
    draftStatus: PendingStatusFilter,
    onStatusChange: (PendingStatusFilter) -> Unit,
    statusCounts: Triple<Int, Int, Int>,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
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
                    Text(
                        "Filtra por tipo, periodo y estado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Close, "Cerrar filtros")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("TIPO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PendingTypeChip(PendingType.PAYMENT, draftType, onTypeChange, Modifier.weight(1f))
                PendingTypeChip(PendingType.COLLECTION, draftType, onTypeChange, Modifier.weight(1f))
            }
            Text("PERIODO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PendingPeriodChip("Ciclo", draftPeriod == PendingPeriodFilter.FORTNIGHT, { onPeriodChange(PendingPeriodFilter.FORTNIGHT) }, Modifier.weight(1f))
                PendingPeriodChip("Mes", draftPeriod == PendingPeriodFilter.MONTH, { onPeriodChange(PendingPeriodFilter.MONTH) }, Modifier.weight(1f))
                PendingPeriodChip("Todas", draftPeriod == PendingPeriodFilter.ALL, { onPeriodChange(PendingPeriodFilter.ALL) }, Modifier.weight(1f))
            }
            Text("ESTADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PendingStatusChip(
                    label = "Todos (${statusCounts.first})",
                    selected = draftStatus == PendingStatusFilter.ALL,
                    onSelect = { onStatusChange(PendingStatusFilter.ALL) },
                    modifier = Modifier.weight(1f),
                )
                PendingStatusChip(
                    label = "Pendientes (${statusCounts.second})",
                    selected = draftStatus == PendingStatusFilter.PENDING,
                    onSelect = { onStatusChange(PendingStatusFilter.PENDING) },
                    modifier = Modifier.weight(1f),
                )
                PendingStatusChip(
                    label = "Hechos (${statusCounts.third})",
                    selected = draftStatus == PendingStatusFilter.DONE,
                    onSelect = { onStatusChange(PendingStatusFilter.DONE) },
                    modifier = Modifier.weight(1f),
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
private fun PendingSummaryCard(
    type: PendingType,
    filter: PendingPeriodFilter,
    range: DateRange?,
    total: Long,
    count: Int,
    formatter: DateTimeFormatter,
) {
    val periodTitle = when (filter) {
        PendingPeriodFilter.FORTNIGHT -> "CICLO ACTUAL"
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
    onOpenList: (String) -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        when (size) {
            EntryCardSize.COMPACT ->
                PendingCompactCardContent(entry, category, formatter, onToggleDone, onEdit, onOpenList, onDelete)
            EntryCardSize.NORMAL ->
                PendingNormalCardContent(entry, category, formatter, onToggleDone, onEdit, onOpenList, onDelete)
            EntryCardSize.DETAILED ->
                PendingDetailedCardContent(entry, category, formatter, onToggleDone, onEdit, onOpenList, onDelete)
        }
    }
}

@Composable
private fun PendingEditAction(
    sourceShoppingListId: Long?,
    listName: String,
    onEdit: () -> Unit,
    onOpenList: (String) -> Unit,
) {
    if (sourceShoppingListId != null) {
        IconButton(onClick = { onOpenList(listName) }) {
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Shopping, "Ir a Lista", modifier = Modifier.size(20.dp))
        }
    } else {
        IconButton(onClick = onEdit) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Edit, "Editar", modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun PendingCompactCardContent(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onOpenList: (String) -> Unit,
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
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Notes,
                    contentDescription = "Tiene comentario",
                    tint = MaterialTheme.colorScheme.secondary,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
            com.angel.mony.ui.iconography.MonyIcon(
                com.angel.mony.ui.iconography.MonyIcon.Calendar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
                com.angel.mony.ui.iconography.MonyIcon(
                    if (entry.isDone) com.angel.mony.ui.iconography.MonyIcon.Undo else com.angel.mony.ui.iconography.MonyIcon.Completed,
                    contentDescription = if (entry.isDone) "Reabrir" else "Marcar hecho",
                    tint = if (entry.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
                    modifier = Modifier.size(20.dp),
                )
            }
            PendingEditAction(entry.sourceShoppingListId, entry.description, onEdit, onOpenList)
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, role = com.angel.mony.ui.iconography.MonyIconRole.STATE, modifier = Modifier.size(20.dp))
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
    onOpenList: (String) -> Unit,
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
        PendingCardActionsRow(
            sourceShoppingListId = entry.sourceShoppingListId,
            listName = entry.description,
            isDone = entry.isDone,
            onToggleDone = onToggleDone,
            onEdit = onEdit,
            onOpenList = onOpenList,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun PendingDetailedCardContent(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onOpenList: (String) -> Unit,
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
            com.angel.mony.ui.iconography.MonyIcon(
                if (expanded) com.angel.mony.ui.iconography.MonyIcon.ExpandLess else com.angel.mony.ui.iconography.MonyIcon.ExpandMore,
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
        PendingCardActionsRow(
            sourceShoppingListId = entry.sourceShoppingListId,
            listName = entry.description,
            isDone = entry.isDone,
            onToggleDone = onToggleDone,
            onEdit = onEdit,
            onOpenList = onOpenList,
            onDelete = onDelete,
        )
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
        com.angel.mony.ui.iconography.MonyIcon(
            com.angel.mony.ui.iconography.MonyIcon.Calendar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
    sourceShoppingListId: Long?,
    listName: String,
    isDone: Boolean,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onOpenList: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PrimaryButton(
            text = if (isDone) "Reabrir" else "Marcar hecho",
            onClick = onToggleDone,
            modifier = Modifier.weight(1f),
        )
        PendingEditAction(sourceShoppingListId, listName, onEdit, onOpenList)
        IconButton(onClick = onDelete) {
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        }
    }
}

@Composable
internal fun pendingAmountColor(type: PendingType): Color =
    if (type == PendingType.PAYMENT) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary



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
            com.angel.mony.ui.iconography.MonyIcon(
                if (entry.isDone) com.angel.mony.ui.iconography.MonyIcon.Completed else com.angel.mony.ui.iconography.MonyIcon.Pending,
                contentDescription = null,
                tint = if (entry.isDone) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
internal fun PendingEntry.matchesPendingQuery(
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
