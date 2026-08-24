package com.example.personalfinancetracker.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.BudgetPeriodView
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.canManuallyCloseBudgetCycle
import com.example.personalfinancetracker.domain.model.shouldAutomaticallyCloseBudgetCycle
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.TransactionRow
import com.example.personalfinancetracker.presentation.components.TransactionDetailsDialog
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onAdd: (TransactionType) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closingCycle by viewModel.closingCycle.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val manualCloseAvailable = canManuallyCloseBudgetCycle(state.budget, today)
    var editingBudget by remember { mutableStateOf(false) }
    var confirmingClose by remember { mutableStateOf(false) }
    var showingHistory by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<com.example.personalfinancetracker.domain.model.FinanceTransaction?>(null) }

    LaunchedEffect(
        automaticCycleClose,
        automaticCloseTime,
        state.budget?.cycleStart,
        state.budget?.period,
        state.budget?.cycleSchedules,
    ) {
        if (automaticCycleClose && shouldAutomaticallyCloseBudgetCycle(state.budget, LocalDateTime.now(), automaticCloseTime)) {
            viewModel.closeCurrentCycle {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Inicio") },
                actions = {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RegisterActionChip("Registrar gasto", { onAdd(TransactionType.EXPENSE) }, Modifier.weight(1f))
                    RegisterActionChip("Registrar ingreso", { onAdd(TransactionType.INCOME) }, Modifier.weight(1f))
                }
            }
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel(
                                when {
                                    state.budget?.period == BudgetPeriod.MONTHLY && state.selectedPeriodView == BudgetPeriodView.NEXT -> "PRÓXIMO MES"
                                    state.budget?.period == BudgetPeriod.MONTHLY -> "MES ACTUAL"
                                    state.selectedPeriodView == BudgetPeriodView.NEXT -> "PRÓXIMA QUINCENA"
                                    else -> "QUINCENA ACTUAL"
                                },
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { editingBudget = true }) { Icon(Icons.Outlined.Edit, "Editar presupuesto") }
                        }
                        PeriodViewSelector(
                            currentPeriod = state.currentPeriod,
                            nextPeriod = state.nextPeriod,
                            selected = state.selectedPeriodView,
                            pinned = state.pinnedPeriodView,
                            onSelect = viewModel::selectPeriodView,
                            onPin = viewModel::pinPeriodView,
                        )
                        if (state.budget == null) {
                            Text(
                                "Define cuánto quieres administrar durante este periodo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton("Agregar presupuesto", { editingBudget = true }, Modifier.fillMaxWidth())
                        } else {
                            Text("PRESUPUESTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.format(state.budget!!.amountInCents), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Metric("INGRESOS", state.periodIncomeInCents, Modifier.weight(1f))
                                Metric("GASTOS", state.periodExpenseInCents, Modifier.weight(1f), true)
                                Metric("RESTANTE", state.budget!!.amountInCents - state.periodExpenseInCents, Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryButton(
                                    text = "Historial",
                                    onClick = { showingHistory = true },
                                    modifier = Modifier.weight(1f),
                                )
                                PrimaryButton(
                                    text = "Cerrar ciclo",
                                    onClick = { confirmingClose = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !closingCycle && !automaticCycleClose && manualCloseAvailable,
                                )
                            }
                            if (automaticCycleClose) {
                                Text(
                                    "El cierre automático está activo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (!manualCloseAvailable) {
                                Text(
                                    "El cierre estará disponible el ${state.currentPeriod.endInclusive.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-DO")))}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (state.spending.isNotEmpty()) {
                item { EditorialHeading("GASTOS POR CATEGORÍA") }
                items(state.spending.take(5), key = { "spending-category-${it.category.id}" }) { spending ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(spending.category.name.uppercase(), style = MaterialTheme.typography.labelLarge)
                        Text(MoneyFormatter.format(spending.amountInCents), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EditorialHeading("Últimos movimientos", Modifier.weight(1f))
                    TextButton(onClick = onHistory) {
                        Text("Ver todos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            if (state.recent.isEmpty()) item {
                Text(
                    "No hay movimientos en el período seleccionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.recent, key = { "recent-transaction-${it.id}" }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    category = state.categories[transaction.categoryId],
                    onClick = { selectedTransaction = transaction },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (editingBudget) {
        BudgetDialog(
            currentAmount = state.budget?.amountInCents,
            currentPeriod = state.budget?.period ?: BudgetPeriod.FORTNIGHTLY,
            onDismiss = { editingBudget = false },
            onSave = { amount, period ->
                viewModel.saveBudget(amount, period) { editingBudget = false }
            },
        )
    }

    if (confirmingClose) {
        CloseCycleDialog(
            currentPeriod = state.currentPeriod,
            nextPeriod = state.nextPeriod,
            closingCycle = closingCycle,
            onDismiss = { if (!closingCycle) confirmingClose = false },
            onConfirm = { viewModel.closeCurrentCycle { confirmingClose = false } },
        )
    }

    if (showingHistory) {
        BudgetHistoryDialog(
            cycles = state.cycleHistory,
            onDismiss = { showingHistory = false },
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
@Composable
private fun PeriodViewSelector(
    currentPeriod: DateRange,
    nextPeriod: DateRange,
    selected: BudgetPeriodView,
    pinned: BudgetPeriodView,
    onSelect: (BudgetPeriodView) -> Unit,
    onPin: (BudgetPeriodView) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodViewOption(
            title = "Actual",
            period = currentPeriod,
            selected = selected == BudgetPeriodView.CURRENT,
            pinned = pinned == BudgetPeriodView.CURRENT,
            onSelect = { onSelect(BudgetPeriodView.CURRENT) },
            onPin = { onPin(BudgetPeriodView.CURRENT) },
            modifier = Modifier.weight(1f),
        )
        PeriodViewOption(
            title = "Próxima",
            period = nextPeriod,
            selected = selected == BudgetPeriodView.NEXT,
            pinned = pinned == BudgetPeriodView.NEXT,
            onSelect = { onSelect(BudgetPeriodView.NEXT) },
            onPin = { onPin(BudgetPeriodView.NEXT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PeriodViewOption(
    title: String,
    period: DateRange,
    selected: Boolean,
    pinned: Boolean,
    onSelect: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-DO"))
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${period.start.format(formatter)} / ${period.endInclusive.format(formatter)}".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onPin,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "Vista fijada" else "Fijar vista $title",
                    tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloseCycleDialog(
    currentPeriod: DateRange,
    nextPeriod: DateRange,
    closingCycle: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-DO"))
    }

    AlertDialog(
        onDismissRequest = { if (!closingCycle) onDismiss() },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Cerrar ciclo actual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Se guardará el período del ${currentPeriod.start.format(dateFormatter)} al ${currentPeriod.endInclusive.format(dateFormatter)} en el historial. Tus movimientos y tu saldo general no se borrarán.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "El próximo ciclo irá del ${nextPeriod.start.format(dateFormatter)} al ${nextPeriod.endInclusive.format(dateFormatter)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (closingCycle) "Cerrando…" else "Cerrar ciclo",
                onClick = onConfirm,
                enabled = !closingCycle,
            )
        },
        dismissButton = {
            SecondaryButton("Cancelar", onDismiss)
        },
    )
}

@Composable
private fun RegisterActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text("◆  $text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun EditorialHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun Metric(label: String, amount: Long, modifier: Modifier = Modifier, expense: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            MoneyFormatter.format(amount),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = if (expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun BudgetHistoryDialog(cycles: List<BudgetCycle>, onDismiss: () -> Unit) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("es-DO"))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Historial de ciclos") },
        text = {
            if (cycles.isEmpty()) {
                Text(
                    "Todavía no has cerrado ningún ciclo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(cycles, key = BudgetCycle::id) { cycle ->
                        FinanceCard(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    if (cycle.period == BudgetPeriod.MONTHLY) "CICLO MENSUAL" else "CICLO QUINCENAL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    "${cycle.startDate.format(dateFormatter)} — ${cycle.endDate.format(dateFormatter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                HistoryAmount("Presupuesto", cycle.budgetAmountInCents)
                                HistoryAmount("Ingresos", cycle.incomeInCents)
                                HistoryAmount("Gastos", cycle.expenseInCents, isExpense = true)
                                HistoryAmount("Restante", cycle.remainingInCents)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { PrimaryButton("Listo", onDismiss) },
    )
}

@Composable
private fun HistoryAmount(label: String, amount: Long, isExpense: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BudgetDialog(
    currentAmount: Long?,
    currentPeriod: BudgetPeriod,
    onDismiss: () -> Unit,
    onSave: (String, BudgetPeriod) -> Unit,
) {
    val context = LocalContext.current
    var amount by remember(currentAmount) {
        mutableStateOf(currentAmount?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var period by remember(currentPeriod) { mutableStateOf(currentPeriod) }
    val valid = MoneyFormatter.parseToCents(amount)?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Tu presupuesto", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Indica cuánto ganas y el periodo que quieres controlar. Los días y la hora de cierre se configuran en Ajustes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FinanceTextField(
                    value = amount,
                    onValueChange = { amount = sanitizeAmountInput(it) },
                    label = "Monto (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
                BudgetPeriod.entries.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = period == option,
                            onClick = { period = option },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Text(if (option == BudgetPeriod.MONTHLY) "Mensual" else "Quincenal", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Guardar",
                onClick = {
                    if (valid) onSave(amount, period)
                    else context.showToast("Introduce un monto válido")
                },
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
    )
}
