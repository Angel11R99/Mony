package com.example.personalfinancetracker.presentation.list

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.EntryCardSize
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListOverview
import com.example.personalfinancetracker.domain.model.ShoppingListStatus
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: ShoppingListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val pendingReopen by viewModel.pendingReopen.collectAsStateWithLifecycle()
    val pendingDuplicate by viewModel.pendingDuplicate.collectAsStateWithLifecycle()
    val cardSize by viewModel.cardSize.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var showCardSizeMenu by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(ListStatusFilter.ALL) }
    var showFilters by remember { mutableStateOf(false) }
    var draftStatusFilter by remember { mutableStateOf(statusFilter) }

    val activeLists = remember(state.lists) { state.lists.filter { it.list.status != ShoppingListStatus.COMPLETED } }
    val completedLists = remember(state.lists) { state.lists.filter { it.list.status == ShoppingListStatus.COMPLETED } }
    val displayed = remember(activeLists, completedLists, statusFilter, query) {
        val source = when (statusFilter) {
            ListStatusFilter.ALL -> state.lists
            ListStatusFilter.ACTIVE -> activeLists
            ListStatusFilter.COMPLETED -> completedLists
        }
        val normalized = query.trim()
        if (normalized.isBlank()) source
        else source.filter { it.list.name.contains(normalized, ignoreCase = true) }
    }

    LaunchedEffect(message) {
        message?.let { context.showToast(it); viewModel.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Lista") },
                actions = {
                    GlobalOutlinedIconButton(Icons.Outlined.Add, "Nueva lista", { showCreate = true })
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onSettings)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(); Text("Cargando listas…", Modifier.padding(top = 12.dp)) }
            state.hasError -> EmptyListsCard(
                "No se pudieron cargar las listas.",
                Modifier.padding(padding).padding(18.dp),
            )
            state.lists.isEmpty() && query.isBlank() -> EmptyListsCard(
                "Todavía no tienes listas de compra.",
                Modifier.padding(padding).padding(18.dp),
                onCreate = { showCreate = true },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ListFilterButton(
                            statusFilter = statusFilter,
                            activeCount = activeLists.size,
                            completedCount = completedLists.size,
                            onClick = {
                                draftStatusFilter = statusFilter
                                showFilters = true
                            },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SearchBar(
                                query = query,
                                onQueryChange = { query = it },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            CardSizeMenu(cardSize, viewModel::setCardSize, showCardSizeMenu, { showCardSizeMenu = it })
                        }
                    }
                }
                if (displayed.isEmpty()) {
                    item {
                        EmptyListsCard(
                            if (query.isNotBlank()) "No se encontraron listas." else "No hay listas para este filtro.",
                            Modifier.fillMaxWidth(),
                            if (query.isBlank() && statusFilter != ListStatusFilter.COMPLETED) ({ showCreate = true }) else null,
                        )
                    }
                } else {
                    items(displayed, key = { it.list.id }) { overview ->
                        val list = overview.list
                        ShoppingListCard(
                            overview = overview,
                            size = cardSize,
                            onOpen = { onOpen(list.id) },
                            onDuplicate = { viewModel.requestDuplicate(list) },
                            onDelete = { viewModel.requestDelete(list) },
                            onReopen = { viewModel.requestReopen(list) },
                            enabled = !isSaving,
                        )
                    }
                }
            }
        }
    }

    if (showCreate) CreateListDialog(
        isSaving = isSaving,
        onDismiss = { if (!isSaving) showCreate = false },
        onCreate = { name, budget -> viewModel.create(name, budget) { showCreate = false; onOpen(it) } },
    )
    pendingDelete?.let { list ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Eliminar lista") },
            text = { Text("¿Eliminar \"${list.name}\" y todos sus productos y ajustes?") },
            confirmButton = { TextButton(viewModel::confirmDelete, enabled = !isSaving) { Text("Eliminar") } },
            dismissButton = { TextButton(viewModel::cancelDelete) { Text("Cancelar") } },
        )
    }
    pendingReopen?.let { list ->
        AlertDialog(
            onDismissRequest = viewModel::cancelReopen,
            title = { Text("¿Reabrir esta lista?") },
            text = { Text("La lista \"${list.name}\" volverá a En compra. El gasto asociado se eliminará del historial.") },
            confirmButton = { TextButton(viewModel::confirmReopen, enabled = !isSaving) { Text("Reabrir") } },
            dismissButton = { TextButton(viewModel::cancelReopen) { Text("Cancelar") } },
        )
    }
    pendingDuplicate?.let { list ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDuplicate,
            title = { Text("¿Duplicar lista?") },
            text = { Text("Se creará una copia de \"${list.name}\" con el mismo nombre y productos.") },
            confirmButton = { TextButton(viewModel::confirmDuplicate, enabled = !isSaving) { Text("Duplicar") } },
            dismissButton = { TextButton(viewModel::cancelDuplicate) { Text("Cancelar") } },
        )
    }

    if (showFilters) {
        ListFilterSheet(
            draftStatusFilter = draftStatusFilter,
            onDraftChange = { draftStatusFilter = it },
            activeCount = activeLists.size,
            completedCount = completedLists.size,
            onClear = { draftStatusFilter = ListStatusFilter.ALL },
            onApply = {
                statusFilter = draftStatusFilter
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Buscar listas…") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton({ onQueryChange("") }) { Icon(Icons.Outlined.Close, "Limpiar") }
            }
        },
        singleLine = true,
    )
}

private enum class ListStatusFilter { ALL, ACTIVE, COMPLETED }

@Composable
private fun ListFilterButton(
    statusFilter: ListStatusFilter,
    activeCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
) {
    val label = when (statusFilter) {
        ListStatusFilter.ALL -> "Todos (${activeCount + completedCount})"
        ListStatusFilter.ACTIVE -> "Activas ($activeCount)"
        ListStatusFilter.COMPLETED -> "Finalizadas ($completedCount)"
    }
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
private fun ListFilterSheet(
    draftStatusFilter: ListStatusFilter,
    onDraftChange: (ListStatusFilter) -> Unit,
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
                    Text("Filtra por estado de la lista", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar filtros") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("ESTADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draftStatusFilter == ListStatusFilter.ALL,
                    onClick = { onDraftChange(ListStatusFilter.ALL) },
                    label = { Text("Todos (${activeCount + completedCount})") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                )
                FilterChip(
                    selected = draftStatusFilter == ListStatusFilter.ACTIVE,
                    onClick = { onDraftChange(ListStatusFilter.ACTIVE) },
                    label = { Text("Activas ($activeCount)") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                )
                FilterChip(
                    selected = draftStatusFilter == ListStatusFilter.COMPLETED,
                    onClick = { onDraftChange(ListStatusFilter.COMPLETED) },
                    label = { Text("Finalizadas ($completedCount)") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.example.personalfinancetracker.presentation.components.SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                com.example.personalfinancetracker.presentation.components.PrimaryButton("Aplicar", onApply, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CardSizeMenu(selected: EntryCardSize, onSelect: (EntryCardSize) -> Unit, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    Box {
        TextButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Outlined.ViewAgenda, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(selected.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded, { onExpandedChange(false) }) {
            EntryCardSize.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = { onSelect(option); onExpandedChange(false) },
                )
            }
        }
    }
}

@Composable
private fun EmptyListsCard(text: String, modifier: Modifier, onCreate: (() -> Unit)? = null) {
    FinanceCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (onCreate != null) {
                Text("Organiza productos, presupuesto y el gasto final desde un solo lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton("Crear la primera lista", onCreate, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShoppingListCard(
    overview: ShoppingListOverview,
    size: EntryCardSize,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onReopen: () -> Unit,
    enabled: Boolean,
) {
    when (size) {
        EntryCardSize.COMPACT -> CompactListCard(overview, onOpen, onDuplicate, onDelete, onReopen, enabled)
        EntryCardSize.NORMAL -> NormalListCard(overview, onOpen, onDuplicate, onDelete, onReopen, enabled)
        EntryCardSize.DETAILED -> DetailedListCard(overview, onOpen, onDuplicate, onDelete, onReopen, enabled)
    }
}

@Composable
private fun CompactListCard(
    overview: ShoppingListOverview, onOpen: () -> Unit, onDuplicate: () -> Unit,
    onDelete: () -> Unit, onReopen: () -> Unit, enabled: Boolean,
) {
    val list = overview.list
    FinanceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(list.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(list.status.spanishLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    IconButton(onClick = onDuplicate, enabled = enabled, modifier = Modifier.heightIn(min = 36.dp)) { Icon(Icons.Outlined.ContentCopy, "Duplicar lista", modifier = Modifier.padding(0.dp)) }
                    if (list.status != ShoppingListStatus.COMPLETED) {
                        IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.heightIn(min = 36.dp)) { Icon(Icons.Outlined.Delete, "Eliminar lista", modifier = Modifier.padding(0.dp)) }
                    } else {
                        IconButton(onClick = onReopen, enabled = enabled, modifier = Modifier.heightIn(min = 36.dp)) { Icon(Icons.Outlined.Refresh, "Reabrir lista", modifier = Modifier.padding(0.dp)) }
                    }
                }
            }
            Text(
                "${overview.itemCount} producto${if (overview.itemCount == 1) "" else "s"} · ${MoneyFormatter.format(overview.totalInCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NormalListCard(
    overview: ShoppingListOverview, onOpen: () -> Unit, onDuplicate: () -> Unit,
    onDelete: () -> Unit, onReopen: () -> Unit, enabled: Boolean,
) {
    val list = overview.list
    FinanceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(list.status.spanishLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onDuplicate, enabled = enabled) { Icon(Icons.Outlined.ContentCopy, "Duplicar lista") }
                if (list.status != ShoppingListStatus.COMPLETED) {
                    IconButton(onClick = onDelete, enabled = enabled) { Icon(Icons.Outlined.Delete, "Eliminar lista") }
                } else {
                    IconButton(onClick = onReopen, enabled = enabled) { Icon(Icons.Outlined.Refresh, "Reabrir lista") }
                }
            }
            val date = list.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO")))
            Text("Actualizada $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${overview.itemCount} producto${if (overview.itemCount == 1) "" else "s"} · ${MoneyFormatter.format(overview.totalInCents)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            list.budgetInCents?.let { Text("Presupuesto: ${MoneyFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun DetailedListCard(
    overview: ShoppingListOverview, onOpen: () -> Unit, onDuplicate: () -> Unit,
    onDelete: () -> Unit, onReopen: () -> Unit, enabled: Boolean,
) {
    val list = overview.list
    FinanceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(list.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(list.status.spanishLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDuplicate, enabled = enabled) { Icon(Icons.Outlined.ContentCopy, "Duplicar lista") }
                if (list.status != ShoppingListStatus.COMPLETED) {
                    IconButton(onClick = onDelete, enabled = enabled) { Icon(Icons.Outlined.Delete, "Eliminar lista") }
                } else {
                    IconButton(onClick = onReopen, enabled = enabled) { Icon(Icons.Outlined.Refresh, "Reabrir lista") }
                }
            }
            val zone = ZoneId.systemDefault()
            val timingFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-DO"))
            Text("Creada ${list.createdAt.atZone(zone).toLocalDate().format(timingFormatter)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val updated = list.updatedAt.atZone(zone).toLocalDate().format(timingFormatter)
            Text("Actualizada $updated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${overview.itemCount} producto${if (overview.itemCount == 1) "" else "s"} · ${MoneyFormatter.format(overview.totalInCents)}", style = MaterialTheme.typography.bodyMedium)
            list.budgetInCents?.let { Text("Presupuesto: ${MoneyFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun CreateListDialog(isSaving: Boolean, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
                FinanceTextField(
                    budget, { budget = sanitizeAmountInput(it) }, "Presupuesto (opcional)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
            }
        },
        confirmButton = { TextButton({ onCreate(name, budget) }, enabled = !isSaving) { Text(if (isSaving) "Guardando…" else "Crear") } },
        dismissButton = { TextButton(onDismiss, enabled = !isSaving) { Text("Cancelar") } },
    )
}

internal fun ShoppingListStatus.spanishLabel(): String = when (this) {
    ShoppingListStatus.PENDING -> "Pendiente"
    ShoppingListStatus.SHOPPING -> "En compra"
    ShoppingListStatus.COMPLETED -> "Finalizada"
}
