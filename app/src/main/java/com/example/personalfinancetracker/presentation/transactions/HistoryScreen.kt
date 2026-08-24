package com.example.personalfinancetracker.presentation.transactions

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.TransactionRow
import com.example.personalfinancetracker.presentation.components.TransactionDetailsDialog
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class HistoryTypeFilter(val label: String, val type: TransactionType?) {
    ALL("Todos", null),
    EXPENSE("Gastos", TransactionType.EXPENSE),
    INCOME("Ingresos", TransactionType.INCOME),
}

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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var typeFilter by remember { mutableStateOf(HistoryTypeFilter.ALL) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var sort by remember { mutableStateOf(HistorySort.NEWEST) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    var selectedTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportTo) }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
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
    val filtered = remember(state.transactions, typeFilter, categoryId, startDate, endDate, query, state.categories) {
        searchFinanceTransactions(
            filterTransactions(state.transactions, typeFilter.type, categoryId, startDate, endDate),
            state.categories,
            query,
        )
    }
    val sorted = remember(filtered, state.categories, sort) {
        sortTransactions(filtered, state.categories, sort)
    }
    val incomeTotal = filtered.filter { it.type == TransactionType.INCOME }.sumOf(FinanceTransaction::amountInCents)
    val expenseTotal = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf(FinanceTransaction::amountInCents)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Historial") },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.Outlined.FileDownload,
                        contentDescription = "Exportar historial a CSV",
                        onClick = {
                            exportLauncher.launch("movimientos-${LocalDate.now()}.csv")
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onClick = onSettings)
                    Spacer(Modifier.width(14.dp))
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HistoryFilters(
                    query = query,
                    onQueryChange = { query = it },
                    typeFilter = typeFilter,
                    onTypeChange = { typeFilter = it },
                    categories = availableCategories,
                    categoryId = categoryId,
                    onCategoryChange = { categoryId = it },
                    startDate = startDate,
                    onStartDateChange = { selected ->
                        startDate = selected
                        if (endDate != null && selected != null && endDate!!.isBefore(selected)) endDate = selected
                    },
                    endDate = endDate,
                    onEndDateChange = { selected ->
                        endDate = selected
                        if (startDate != null && selected != null && startDate!!.isAfter(selected)) startDate = selected
                    },
                    sort = sort,
                    onSortChange = { sort = it },
                    onClear = {
                        typeFilter = HistoryTypeFilter.ALL
                        categoryId = null
                        startDate = null
                        endDate = null
                    },
                )
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
                    IconButton(onClick = { onEdit(transaction.id, transaction.type) }) {
                        Icon(Icons.Outlined.Edit, "Editar")
                    }
                    IconButton(onClick = { viewModel.duplicate(transaction.id) }) {
                        Icon(Icons.Outlined.ContentCopy, "Duplicar")
                    }
                    IconButton(onClick = { pendingDelete = transaction }) {
                        Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    pendingDelete?.let { transaction ->
        val categoryName = state.categories[transaction.categoryId]?.name ?: "Sin categoría"
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("¿Eliminar movimiento?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$categoryName · ${MoneyFormatter.format(transaction.amountInCents)}")
                    Text(
                        "Esta acción no se puede deshacer.",
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

    selectedTransaction?.let { transaction ->
        TransactionDetailsDialog(
            transaction = transaction,
            category = state.categories[transaction.categoryId],
            onDismiss = { selectedTransaction = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    typeFilter: HistoryTypeFilter,
    onTypeChange: (HistoryTypeFilter) -> Unit,
    categories: List<Category>,
    categoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    sort: HistorySort,
    onSortChange: (HistorySort) -> Unit,
    onClear: () -> Unit,
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryTypeFilter.entries.forEach { option ->
                FilterChip(
                    selected = typeFilter == option,
                    onClick = { onTypeChange(option) },
                    label = { Text(option.label, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            FinanceTextField(
                query,
                onQueryChange,
                "Buscar",
                modifier = Modifier.weight(1f),
                placeholder = "Buscar movimientos...",
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = { onQueryChange("") },
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
            GlobalOutlinedIconButton(
                icon = Icons.Outlined.Menu,
                contentDescription = "Filtros avanzados",
                onClick = { showAdvancedFilters = true },
                size = 56.dp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistorySortMenu(sort = sort, onSortChange = onSortChange)
            TextButton(onClick = {
                onQueryChange("")
                onClear()
            }) {
                Text("Limpiar filtros")
            }
        }
    }
    if (showAdvancedFilters) {
        HistoryAdvancedFiltersSheet(
            categories = categories,
            categoryId = categoryId,
            onCategoryChange = onCategoryChange,
            startDate = startDate,
            onStartDateChange = onStartDateChange,
            endDate = endDate,
            onEndDateChange = onEndDateChange,
            onDismiss = { showAdvancedFilters = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryAdvancedFiltersSheet(
    categories: List<Category>,
    categoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember { mutableStateOf("") }
    val matchingCategories = remember(categories, categorySearch) {
        searchCategories(categories, categorySearch)
    }
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
                    Text(
                        "Refina los movimientos mostrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar filtros")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
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
        }
    }
}

@Composable
private fun HistorySortMenu(sort: HistorySort, onSortChange: (HistorySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(sort.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HistorySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (sort == option) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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
): List<FinanceTransaction> = transactions.filter { transaction ->
    (type == null || transaction.type == type) &&
        (categoryId == null || transaction.categoryId == categoryId) &&
        (startDate == null || !transaction.date.isBefore(startDate)) &&
        (endDate == null || !transaction.date.isAfter(endDate))
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

private const val MILLIS_PER_DAY = 86_400_000L
