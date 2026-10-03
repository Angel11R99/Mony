package com.angel.mony.presentation.transactions

import android.content.ClipData
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.HistoryPdfMeta
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.time.toJavaLocalDate
import com.angel.mony.core.time.toKotlinLocalDate
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.TransactionRow
import com.angel.mony.presentation.components.TransactionDetailsDialog
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.localDateNullableSaver
import com.angel.mony.presentation.components.LoadingContent
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.SkeletonCard
import com.angel.mony.presentation.components.SkeletonChip
import com.angel.mony.presentation.components.SkeletonLine
import com.angel.mony.presentation.components.SkeletonTextField
import com.angel.mony.presentation.components.SkeletonTransactionRow
import com.angel.mony.presentation.components.SkeletonHost
import com.angel.mony.presentation.components.StaggeredReveal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class HistoryListResult(
    val filtered: List<FinanceTransaction>,
    val sorted: List<FinanceTransaction>,
    val incomeTotal: Long,
    val expenseTotal: Long,
)

private enum class HistoryTypeFilter(val label: String, val type: TransactionType?) {
    ALL("Todos", null),
    EXPENSE("Gastos", TransactionType.EXPENSE),
    INCOME("Ingresos", TransactionType.INCOME),
}

sealed class HistoryCycleFilter(val label: String, val range: DateRange?) {
    data object All : HistoryCycleFilter("Todos los ciclos", null)
    data class Custom(val dateRange: DateRange, val displayLabel: String) :
        HistoryCycleFilter(displayLabel, dateRange)
}

private val historyCycleFilterSaver = listSaver<HistoryCycleFilter, Any>(
    save = {
        when (it) {
            HistoryCycleFilter.All -> listOf(0L)
            is HistoryCycleFilter.Custom -> listOf(
                1L,
                it.dateRange.start.toEpochDays().toLong(),
                it.dateRange.endInclusive.toEpochDays().toLong(),
                it.displayLabel,
            )
        }
    },
    restore = {
        when (it[0]) {
            0L -> HistoryCycleFilter.All
            else -> HistoryCycleFilter.Custom(
                dateRange = DateRange(
                    LocalDate.ofEpochDay(it[1] as Long).toKotlinLocalDate(),
                    LocalDate.ofEpochDay(it[2] as Long).toKotlinLocalDate(),
                ),
                displayLabel = it[3] as String,
            )
        }
    },
)

internal enum class HistorySort(val label: String) {
    NEWEST("Más recientes"),
    OLDEST("Más antiguos"),
    AMOUNT_DESC("Mayor monto"),
    AMOUNT_ASC("Menor monto"),
    CATEGORY_ASC("Categoría A–Z"),
    CATEGORY_DESC("Categoría Z–A"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEdit: (Long, TransactionType) -> Unit,
    onSettings: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    onViewShoppingList: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var typeFilter by rememberSaveable { mutableStateOf(HistoryTypeFilter.ALL) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var startDate by rememberSaveable(stateSaver = localDateNullableSaver) { mutableStateOf<LocalDate?>(null) }
    var endDate by rememberSaveable(stateSaver = localDateNullableSaver) { mutableStateOf<LocalDate?>(null) }
    var sort by rememberSaveable { mutableStateOf(HistorySort.NEWEST) }
    var query by rememberSaveable { mutableStateOf("") }
    var cycleFilter by rememberSaveable(stateSaver = historyCycleFilterSaver) { mutableStateOf<HistoryCycleFilter>(HistoryCycleFilter.All) }
    var cycleFilterInitialized by rememberSaveable { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var draftType by remember { mutableStateOf(typeFilter) }
    var draftCategoryId by remember { mutableStateOf(categoryId) }
    var draftStart by remember { mutableStateOf(startDate) }
    var draftEnd by remember { mutableStateOf(endDate) }
    var draftCycle by remember { mutableStateOf(cycleFilter) }
    var pendingDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    var pendingDuplicate by remember { mutableStateOf<FinanceTransaction?>(null) }
    var selectedTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportAllHistory by remember { mutableStateOf(false) }
    var exportFormat by rememberSaveable { mutableStateOf(HistoryExportFormat.PDF) }
    var pendingExportRequest by remember { mutableStateOf<HistoryExportRequest?>(null) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::prepareImportFrom) }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val request = pendingExportRequest
        if (uri != null && request != null) viewModel.exportHistoryTo(uri, request, HistoryExportFormat.PDF)
        pendingExportRequest = null
    }
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val request = pendingExportRequest
        if (uri != null && request != null) viewModel.exportHistoryTo(uri, request, HistoryExportFormat.EXCEL)
        pendingExportRequest = null
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val request = pendingExportRequest
        if (uri != null && request != null) viewModel.exportHistoryTo(uri, request, HistoryExportFormat.CSV)
        pendingExportRequest = null
    }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    val cycleOptions = remember(state.budget, state.cycleHistory) {
        buildHistoryCycleOptions(state.budget, state.cycleHistory)
    }
    LaunchedEffect(state.isReady, cycleOptions) {
        if (!state.isReady) return@LaunchedEffect
        if (!cycleFilterInitialized) {
            cycleFilter = currentHistoryCycleFilter(state.budget)
            draftCycle = cycleFilter
            cycleFilterInitialized = true
        } else if (cycleFilter != HistoryCycleFilter.All && cycleOptions.none { it.label == cycleFilter.label && it.range == cycleFilter.range }) {
            cycleFilter = HistoryCycleFilter.All
        }
    }
    val availableCategories = remember(state.categories, typeFilter) {
        state.categories.values
            .filter { typeFilter.type == null || it.type == typeFilter.type }
            .sortedBy(Category::name)
    }
    LaunchedEffect(typeFilter) {
        if (categoryId != null && availableCategories.none { it.id == categoryId }) categoryId = null
    }
    val debouncedQuery by produceState(initialValue = query, query) {
        if (value != query && query.isNotBlank()) delay(160)
        value = query
    }
    val listResult by produceState<HistoryListResult?>(
        initialValue = null,
        state.transactions,
        state.categories,
        typeFilter,
        categoryId,
        startDate,
        endDate,
        cycleFilter,
        debouncedQuery,
        sort,
    ) {
        if (!state.isReady) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val filteredItems = searchFinanceTransactions(
                filterTransactions(
                    state.transactions,
                    typeFilter.type,
                    categoryId,
                    startDate,
                    endDate,
                    cycleFilter.range,
                ),
                state.categories,
                debouncedQuery,
            )
            var income = 0L
            var expense = 0L
            filteredItems.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amountInCents
                    TransactionType.EXPENSE -> expense += transaction.amountInCents
                }
            }
            HistoryListResult(
                filtered = filteredItems,
                sorted = sortTransactions(filteredItems, state.categories, sort),
                incomeTotal = income,
                expenseTotal = expense,
            )
        }
    }
    val filtered = listResult?.filtered.orEmpty()
    val sorted = listResult?.sorted.orEmpty()
    val incomeTotal = listResult?.incomeTotal ?: 0L
    val expenseTotal = listResult?.expenseTotal ?: 0L

    val exportDateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    fun buildExportRequest(allHistory: Boolean): HistoryExportRequest {
        val cycleLabel = cycleFilter.takeIf { it != HistoryCycleFilter.All }?.label
        val periodLabel = when {
            allHistory -> "Todo el historial"
            cycleLabel != null -> "Ciclo: $cycleLabel"
            startDate != null && endDate != null ->
                "Período: ${startDate!!.format(exportDateFormatter)} – ${endDate!!.format(exportDateFormatter)}"
            startDate != null -> "Desde: ${startDate!!.format(exportDateFormatter)}"
            endDate != null -> "Hasta: ${endDate!!.format(exportDateFormatter)}"
            else -> "Todo el historial"
        }
        val filterLines = if (allHistory) {
            listOf("Sin filtros aplicados")
        } else {
            buildList {
                if (typeFilter.type != null) add("Tipo: ${typeFilter.label}")
                categoryId?.let {
                    add("Categoría: ${state.categories[it]?.name ?: "Sin categoría"}")
                }
                if (query.isNotBlank()) add("Búsqueda: \"${query.trim()}\"")
                if (isEmpty()) add("Sin filtros adicionales")
            }
        }
        val items = if (allHistory) {
            sortTransactions(state.transactions, state.categories, sort)
        } else {
            sorted
        }
        return HistoryExportRequest(items, HistoryPdfMeta(periodLabel, filterLines, sort.label))
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Historial") },
                actions = {
                    GlobalOutlinedIconButton(
                        semanticIcon = com.angel.mony.ui.iconography.MonyIcon.Download,
                        contentDescription = "Importar respaldo",
                        onClick = { importLauncher.launch(arrayOf("*/*", "application/json", "text/csv")) },
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    GlobalOutlinedIconButton(
                        semanticIcon = com.angel.mony.ui.iconography.MonyIcon.Upload,
                        contentDescription = "Exportar respaldo completo",
                        onClick = {
                            exportLauncher.launch("mony-respaldo-${LocalDate.now()}.json")
                        },
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    GlobalOutlinedIconButton(
                        semanticIcon = com.angel.mony.ui.iconography.MonyIcon.Share,
                        contentDescription = "Compartir historial",
                        onClick = {
                            exportAllHistory = false
                            showExportDialog = true
                        },
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalOutlinedIconButton(
                        semanticIcon = com.angel.mony.ui.iconography.MonyIcon.Settings,
                        contentDescription = "Ajustes de la app",
                        onClick = onSettings,
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        val isLoadingHistory = !state.isReady || listResult == null
        SkeletonHost(isLoading = isLoadingHistory) {
            LoadingContent(
                isLoading = isLoadingHistory,
                modifier = Modifier.padding(padding),
                minVisibleMillis = 0,
                skeleton = { HistorySkeleton() },
            ) {
                StaggeredReveal(
                    screenKey = "history",
                    modifier = Modifier.fillMaxSize(),
                    staggerMillis = 40,
                    durationMillis = 180,
                    slideOffsetDp = 8.dp,
                ) {
                    add {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
            item {
                HistoryFilterButton(
                    typeFilter = typeFilter,
                    cycleFilter = cycleFilter,
                    categoryId = categoryId,
                    categories = state.categories,
                    startDate = startDate,
                    endDate = endDate,
                    onClick = {
                        draftType = typeFilter
                        draftCycle = cycleFilter
                        draftCategoryId = categoryId
                        draftStart = startDate
                        draftEnd = endDate
                        showFilters = true
                    },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FinanceTextField(
                        query,
                        { query = it },
                        "Buscar",
                        modifier = Modifier.weight(1f),
                        placeholder = "Buscar movimientos...",
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
                    HistorySortMenu(sort = sort, onSortChange = { sort = it })
                }
            }
            item {
                FilterSummary(
                    count = filtered.size,
                    income = incomeTotal,
                    expense = expenseTotal,
                )
            }
            if (sorted.isEmpty()) {
                item {
                    Text(
                        "No hay movimientos para este filtro.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(sorted, key = FinanceTransaction::id) { transaction ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransactionRow(
                        transaction,
                        state.categories[transaction.categoryId],
                        Modifier.weight(1f),
                        onClick = { selectedTransaction = transaction },
                    )
                    val linkedShoppingList = state.shoppingListIdsByExpenseTransactionId[transaction.id]
                    IconButton(
                        onClick = { onEdit(transaction.id, transaction.type) },
                        enabled = linkedShoppingList == null,
                    ) {
                        com.angel.mony.ui.iconography.MonyIcon(
                            com.angel.mony.ui.iconography.MonyIcon.Edit,
                            if (linkedShoppingList == null) "Editar" else "El gasto de una lista finalizada no se puede editar",
                        )
                    }
                    IconButton(onClick = { pendingDuplicate = transaction }) {
                        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Copy, "Duplicar")
                    }
                    IconButton(
                        onClick = { pendingDelete = transaction },
                        enabled = linkedShoppingList == null,
                    ) {
                        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Delete, if (linkedShoppingList == null) "Eliminar" else "El movimiento se administra desde la compra", tint = MaterialTheme.colorScheme.error, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
                    }
                }
            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        HistoryFilterSheet(
            availableCategories = availableCategories,
            categoryId = draftCategoryId,
            onCategoryChange = { draftCategoryId = it },
            startDate = draftStart,
            onStartDateChange = { selected ->
                draftStart = selected
                if (draftEnd != null && selected != null && draftEnd!!.isBefore(selected)) draftEnd = selected
            },
            endDate = draftEnd,
            onEndDateChange = { selected ->
                draftEnd = selected
                if (draftStart != null && selected != null && draftStart!!.isAfter(selected)) draftStart = selected
            },
            typeFilter = draftType,
            onTypeChange = { draftType = it },
            cycleFilter = draftCycle,
            onCycleChange = { draftCycle = it },
            cycleOptions = cycleOptions,
            onClear = {
                draftType = HistoryTypeFilter.ALL
                draftCategoryId = null
                draftStart = null
                draftEnd = null
                draftCycle = HistoryCycleFilter.All
            },
            onApply = {
                typeFilter = draftType
                categoryId = draftCategoryId
                startDate = draftStart
                endDate = draftEnd
                cycleFilter = draftCycle
                if (categoryId != null && availableCategories.none { it.id == categoryId }) categoryId = null
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }

    pendingDelete?.let { transaction ->
        val categoryName = state.categories[transaction.categoryId]?.name ?: "Sin categoría"
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
                )
            },
            title = { Text("¿Eliminar movimiento?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$categoryName · ${MoneyFormatter.format(transaction.amountInCents)}")
                    Text(
                        if (state.shoppingListIdsByExpenseTransactionId.containsKey(transaction.id)) {
                            "La lista y su detalle se conservarán, pero dejarán de estar vinculados a este gasto."
                        } else {
                            "Esta acción no se puede deshacer."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(transaction.id)
                    pendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    pendingDuplicate?.let { transaction ->
        val categoryName = state.categories[transaction.categoryId]?.name ?: "Sin categoría"
        AlertDialog(
            onDismissRequest = { pendingDuplicate = null },
            icon = {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Copy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("¿Duplicar movimiento?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$categoryName · ${MoneyFormatter.format(transaction.amountInCents)}")
                    transaction.description?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Se creará una copia con la fecha de hoy.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.duplicate(transaction.id)
                    pendingDuplicate = null
                }) {
                    Text("Duplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicate = null }) { Text("Cancelar") }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    restorePreview?.let { preview ->
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val bp = preview.backupPreview
        AlertDialog(
            onDismissRequest = { if (!isRestoring) viewModel.cancelRestore() },
            icon = {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("¿Restaurar respaldo?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bp != null && !bp.isLegacyCsv) {
                        Text("Respaldo completo encontrado:")
                        if (bp.transactionsCount > 0) Text("• ${bp.transactionsCount} movimientos")
                        if (bp.fixedEntriesCount > 0) Text("• ${bp.fixedEntriesCount} fijos")
                        if (bp.pendingEntriesCount > 0) Text("• ${bp.pendingEntriesCount} pendientes / recordatorios")
                        if (bp.shoppingListsCount > 0) Text("• ${bp.shoppingListsCount} listas de compras")
                        if (bp.savingsGoalsCount > 0) Text("• ${bp.savingsGoalsCount} metas de ahorro")
                        if (bp.budgetCyclesCount > 0) Text("• ${bp.budgetCyclesCount} ciclos históricos")
                        if (bp.fortnightPlansCount > 0) Text("• ${bp.fortnightPlansCount} planes de ciclo")
                        if (bp.fortnightPaymentsCount > 0) Text("• ${bp.fortnightPaymentsCount} abonos de ciclo")
                        if (bp.categoriesCount > 0) Text("• ${bp.categoriesCount} categorías")
                        if (bp.transactionsCount == 0 && bp.fixedEntriesCount == 0 && bp.pendingEntriesCount == 0) {
                            Text("El archivo contiene datos de configuración y categorías.")
                        }
                    } else {
                        Text("${preview.movementsCount} movimientos encontrados en el archivo.")
                    }
                    val rangeText = when {
                        preview.firstDate != null && preview.lastDate != null ->
                            "${preview.firstDate.format(dateFormatter)} – ${preview.lastDate.format(dateFormatter)}"
                        else -> null
                    }
                    rangeText?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(
                        "Las categorías faltantes se crearán y los datos que ya existen se omitirán. Los datos actuales no se borrarán.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (bp != null && !bp.isLegacyCsv) {
                        Text(
                            "Incluye fijos, pendientes, listas, metas y ciclos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore, enabled = !isRestoring) {
                    Text(if (isRestoring) "Restaurando…" else "Restaurar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelRestore,
                    enabled = !isRestoring,
                ) { Text("Cancelar") }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    if (showExportDialog) {
        val selectedCount = if (exportAllHistory) state.transactions.size else filtered.size
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("Exportar historial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Elige el formato y los movimientos que deseas incluir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Formato", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryExportFormat.entries.forEach { format ->
                            FilterChip(
                                selected = exportFormat == format,
                                onClick = { exportFormat = format },
                                label = { Text(format.label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                    Text(
                        when (exportFormat) {
                            HistoryExportFormat.PDF -> "Documento listo para leer, imprimir o enviar."
                            HistoryExportFormat.EXCEL -> "Libro editable con resumen, filtros y montos numéricos."
                            HistoryExportFormat.CSV -> "Archivo simple compatible con hojas de cálculo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Contenido", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !exportAllHistory,
                            onClick = { exportAllHistory = false },
                            label = { Text("Filtros actuales (${filtered.size})", maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        FilterChip(
                            selected = exportAllHistory,
                            onClick = { exportAllHistory = true },
                            label = { Text("Todo (${state.transactions.size})", maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                    PrimaryButton(
                        text = "Compartir",
                        onClick = {
                            val selectedFormat = exportFormat
                            showExportDialog = false
                            viewModel.shareHistory(buildExportRequest(exportAllHistory), selectedFormat) { uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = selectedFormat.mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Historial financiero - Mony")
                                    putExtra(Intent.EXTRA_TEXT, "Historial financiero exportado desde Mony.")
                                    clipData = ClipData.newRawUri("Historial financiero", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Compartir historial como ${selectedFormat.label}")
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedCount > 0,
                    )
                    SecondaryButton(
                        text = "Guardar como archivo",
                        onClick = {
                            showExportDialog = false
                            pendingExportRequest = buildExportRequest(exportAllHistory)
                            val fileName = "historial-${LocalDate.now()}.${exportFormat.extension}"
                            when (exportFormat) {
                                HistoryExportFormat.PDF -> pdfLauncher.launch(fileName)
                                HistoryExportFormat.EXCEL -> excelLauncher.launch(fileName)
                                HistoryExportFormat.CSV -> csvLauncher.launch(fileName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedCount > 0,
                    )
                    if (selectedCount == 0) {
                        Text(
                            "No hay movimientos en la selección actual.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancelar") }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    selectedTransaction?.let { transaction ->
        val shoppingListId = if (transaction.type == TransactionType.EXPENSE) {
            state.shoppingListIdsByExpenseTransactionId[transaction.id]
        } else {
            null
        }
        TransactionDetailsDialog(
            transaction = transaction,
            category = state.categories[transaction.categoryId],
            onDismiss = { selectedTransaction = null },
            onViewShoppingList = shoppingListId?.let { listId ->
                {
                    selectedTransaction = null
                    onViewShoppingList(listId)
                }
            },
        )
    }
}

@Composable
private fun HistoryFilterButton(
    typeFilter: HistoryTypeFilter,
    cycleFilter: HistoryCycleFilter,
    categoryId: Long?,
    categories: Map<Long, Category>,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onClick: () -> Unit,
) {
    val categoryLabel = categoryId?.let { categories[it]?.name } ?: "Todas categorías"
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val dateLabel = when {
        startDate != null && endDate != null -> "${startDate.format(formatter)} – ${endDate.format(formatter)}"
        startDate != null -> "Desde ${startDate.format(formatter)}"
        endDate != null -> "Hasta ${endDate.format(formatter)}"
        else -> null
    }
    val subtitleParts = buildList {
        add(typeFilter.label)
        add(cycleFilter.label)
        add(categoryLabel)
        dateLabel?.let { add(it) }
    }
    val subtitle = subtitleParts.joinToString(" · ")
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Filter, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.Start) {
            Text("FILTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterSheet(
    availableCategories: List<Category>,
    categoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    typeFilter: HistoryTypeFilter,
    onTypeChange: (HistoryTypeFilter) -> Unit,
    cycleFilter: HistoryCycleFilter,
    onCycleChange: (HistoryCycleFilter) -> Unit,
    cycleOptions: List<HistoryCycleFilter>,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(categoryId, availableCategories) {
        mutableStateOf(availableCategories.firstOrNull { it.id == categoryId }?.name.orEmpty())
    }
    val matchingCategories = remember(availableCategories, categorySearch, categoryId) {
        if (categoryId != null) availableCategories
        else searchCategories(availableCategories, categorySearch)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        "Refina los movimientos mostrados",
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
                HistoryTypeFilter.entries.forEach { option ->
                    FilterChip(
                        selected = typeFilter == option,
                        onClick = { onTypeChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
            Text("CICLO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            HistoryCycleMenu(
                selected = cycleFilter,
                options = cycleOptions,
                onSelect = onCycleChange,
            )
            Text("CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = categorySearch,
                    onValueChange = { value ->
                        categorySearch = value
                        onCategoryChange(null)
                        categoryExpanded = true
                    },
                    label = { Text("Buscar categoría") },
                    placeholder = { Text("Todas las categorías") },
                    leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (categorySearch.isNotBlank() || categoryId != null) {
                                IconButton(
                                    onClick = {
                                        categorySearch = ""
                                        onCategoryChange(null)
                                        categoryExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    com.angel.mony.ui.iconography.MonyIcon(
                                        com.angel.mony.ui.iconography.MonyIcon.Close,
                                        contentDescription = "Limpiar categoría",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                        }
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    if (categorySearch.isBlank()) {
                        DropdownMenuItem(
                            text = { Text("Todas las categorías") },
                            onClick = {
                                categorySearch = ""
                                onCategoryChange(null)
                                categoryExpanded = false
                            },
                        )
                    }
                    matchingCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categorySearch = category.name
                                onCategoryChange(category.id)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryDateField("Desde", startDate, onStartDateChange, Modifier.weight(1f))
                HistoryDateField("Hasta", endDate, onEndDateChange, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                PrimaryButton("Aplicar", onApply, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistorySortMenu(sort: HistorySort, onSortChange: (HistorySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Sort, null, modifier = Modifier.size(18.dp))
            Text(sort.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HistorySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (sort == option) {
                        { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Check, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE) }
                    } else null,
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value?.format(formatter) ?: "Cualquier día", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Calendar, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
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
                    onValueChange(pickerState.selectedDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) })
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                Row {
                    if (value != null) TextButton(onClick = {
                        onValueChange(null)
                        showPicker = false
                    }) { Text("Quitar") }
                    TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
                }
            },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun FilterSummary(count: Int, income: Long, expense: Long) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RESULTADO · $count MOVIMIENTOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            SummaryAmount("INGRESOS", income)
            SummaryAmount("GASTOS", expense, expense = true)
            SummaryAmount("BALANCE", income - expense)
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: Long, expense: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

internal fun filterTransactions(
    transactions: List<FinanceTransaction>,
    type: TransactionType?,
    categoryId: Long?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    cycleRange: com.angel.mony.domain.model.DateRange? = null,
): List<FinanceTransaction> = transactions.filter { transaction ->
    (type == null || transaction.type == type) &&
        (categoryId == null || transaction.categoryId == categoryId) &&
        (startDate == null || !transaction.date.isBefore(startDate)) &&
        (endDate == null || !transaction.date.isAfter(endDate)) &&
        (cycleRange == null || transaction.date.toKotlinLocalDate() in cycleRange.start..cycleRange.endInclusive)
}

internal fun searchCategories(categories: List<Category>, query: String): List<Category> =
    categories.filter { category ->
        query.isBlank() || category.name.contains(query.trim(), ignoreCase = true)
    }

private val searchDateLongFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("es-DO"))

private val searchDateNumericFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun searchFinanceTransactions(
    transactions: List<FinanceTransaction>,
    categories: Map<Long, Category>,
    query: String,
): List<FinanceTransaction> {
    val normalizedQuery = query.trim()
    val digitQuery = normalizedQuery.filter(Char::isDigit)
    if (normalizedQuery.isEmpty()) return transactions
    return transactions.filter { it.matchesTransactionQuery(normalizedQuery, digitQuery, categories) }
}

private fun FinanceTransaction.matchesTransactionQuery(
    query: String,
    digitQuery: String,
    categories: Map<Long, Category>,
): Boolean {
    val haystack = listOfNotNull(
        description,
        categories[categoryId]?.name,
        if (type == TransactionType.EXPENSE) "Gastos" else "Ingresos",
        MoneyFormatter.format(amountInCents),
        date.format(searchDateLongFormatter),
        date.format(searchDateNumericFormatter),
    ).joinToString(" ")
    return haystack.contains(query, ignoreCase = true) ||
        (digitQuery.isNotEmpty() && amountInCents.toString().contains(digitQuery))
}

internal fun sortTransactions(
    transactions: List<FinanceTransaction>,
    categories: Map<Long, Category>,
    sort: HistorySort,
): List<FinanceTransaction> {
    val newestFirst = compareByDescending<FinanceTransaction> { it.date }
        .thenByDescending { it.createdAt }
    val oldestFirst = compareBy<FinanceTransaction> { it.date }
        .thenBy { it.createdAt }
    val categoryName: (FinanceTransaction) -> String = {
        categories[it.categoryId]?.name.orEmpty().lowercase()
    }
    return when (sort) {
        HistorySort.NEWEST -> transactions.sortedWith(newestFirst)
        HistorySort.OLDEST -> transactions.sortedWith(oldestFirst)
        HistorySort.AMOUNT_DESC -> transactions.sortedWith(
            compareByDescending<FinanceTransaction> { it.amountInCents }.then(newestFirst)
        )
        HistorySort.AMOUNT_ASC -> transactions.sortedWith(
            compareBy<FinanceTransaction> { it.amountInCents }.then(newestFirst)
        )
        HistorySort.CATEGORY_ASC -> transactions.sortedWith(
            compareBy(categoryName).then(newestFirst)
        )
        HistorySort.CATEGORY_DESC -> transactions.sortedWith(
            compareByDescending(categoryName).then(newestFirst)
        )
    }
}

internal fun currentHistoryCycleFilter(
    budget: com.angel.mony.domain.model.BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): HistoryCycleFilter.Custom {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("es-DO"))
    val current = com.angel.mony.domain.model.activeBudgetPeriod(budget, today)
    return HistoryCycleFilter.Custom(
        current,
        "Actual: ${current.start.toJavaLocalDate().format(formatter)} – ${current.endInclusive.toJavaLocalDate().format(formatter)}",
    )
}

private fun buildHistoryCycleOptions(
    budget: com.angel.mony.domain.model.BudgetConfig?,
    history: List<com.angel.mony.domain.model.BudgetCycle>,
): List<HistoryCycleFilter> {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("es-DO"))
    val options = mutableListOf<HistoryCycleFilter>(HistoryCycleFilter.All)
    options.add(currentHistoryCycleFilter(budget))
    val prev = com.angel.mony.domain.model.previousBudgetPeriod(budget, LocalDate.now())
    options.add(
        HistoryCycleFilter.Custom(
            prev,
            "Anterior: ${prev.start.toJavaLocalDate().format(formatter)} – ${prev.endInclusive.toJavaLocalDate().format(formatter)}",
        ),
    )
    history.forEach { cycle ->
        val range = com.angel.mony.domain.model.DateRange(
            cycle.startDate,
            cycle.endDate,
        )
        val label = "Hist: ${range.start.toJavaLocalDate().format(formatter)} – ${range.endInclusive.toJavaLocalDate().format(formatter)}"
        if (options.none { it.range == range }) {
            options.add(HistoryCycleFilter.Custom(range, label))
        }
    }
    return options.distinctBy { it.range to it.label }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCycleMenu(
    selected: HistoryCycleFilter,
    options: List<HistoryCycleFilter>,
    onSelect: (HistoryCycleFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ciclo") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            leadingIcon = {
                com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Calendar, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (selected.label == option.label && selected.range == option.range) {
                        { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Check, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE) }
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

private const val MILLIS_PER_DAY = 86_400_000L

@Composable
private fun HistorySkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SkeletonTextField() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonChip(Modifier.weight(1f))
                SkeletonChip(Modifier.weight(1f))
                SkeletonChip(Modifier.weight(1f))
            }
        }
        item {
            SkeletonCard(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonLine(Modifier.width(96.dp), height = 12.dp)
                    SkeletonLine(Modifier.width(64.dp), height = 12.dp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonLine(Modifier.width(72.dp), height = 12.dp)
                    SkeletonLine(Modifier.width(72.dp), height = 12.dp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonLine(Modifier.width(56.dp), height = 12.dp)
                    SkeletonLine(Modifier.width(88.dp), height = 12.dp)
                }
            }
        }
        items(6) { SkeletonTransactionRow() }
    }
}
