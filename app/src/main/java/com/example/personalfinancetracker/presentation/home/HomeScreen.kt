package com.example.personalfinancetracker.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.canManuallyCloseBudgetCycle
import com.example.personalfinancetracker.domain.model.shouldAutomaticallyCloseBudgetCycle
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.TransactionRow
import com.example.personalfinancetracker.presentation.components.TransactionDetailsDialog
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Composable
fun HomeScreen(
    automaticCycleClose: Boolean,
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

    LaunchedEffect(automaticCycleClose, state.budget?.cycleStart, state.budget?.period) {
        if (automaticCycleClose && shouldAutomaticallyCloseBudgetCycle(state.budget, today)) {
            viewModel.closeCurrentCycle {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp).height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    "Registrar gasto",
                    { onAdd(TransactionType.EXPENSE) },
                    Modifier.weight(1f).fillMaxHeight(),
                )
                SecondaryButton(
                    "Registrar ingreso",
                    { onAdd(TransactionType.INCOME) },
                    Modifier.weight(1f).fillMaxHeight(),
                )
                GlobalSettingsButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel(if (state.budget?.period == BudgetPeriod.MONTHLY) "MES ACTUAL" else "QUINCENA ACTUAL")
                            Text(
                                "${state.period.start.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-DO")))} / ${state.period.endInclusive.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-DO")))}".uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        IconButton(onClick = { editingBudget = true }) { Icon(Icons.Outlined.Edit, "Editar presupuesto") }
                    }
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
                                "El cierre está disponible los días ${state.budget?.closingDays?.sorted()?.joinToString(", ")}.",
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
            items(state.spending.take(5), key = { it.category.id }) { spending ->
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
                "Aún no hay movimientos. Registra el primero para verlo aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.recent, key = { it.id }) { transaction ->
            TransactionRow(
                transaction = transaction,
                category = state.categories[transaction.categoryId],
                onClick = { selectedTransaction = transaction },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (editingBudget) {
        BudgetDialog(
            currentAmount = state.budget?.amountInCents,
            currentPeriod = state.budget?.period ?: BudgetPeriod.FORTNIGHTLY,
            currentClosingDays = state.budget?.closingDays ?: listOf(15),
            onDismiss = { editingBudget = false },
            onSave = { amount, period, closingDays ->
                viewModel.saveBudget(amount, period, closingDays) { editingBudget = false }
            },
        )
    }

    if (confirmingClose) {
        AlertDialog(
            onDismissRequest = { if (!closingCycle) confirmingClose = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Cerrar ciclo actual") },
            text = {
                Text(
                    "Se guardará este periodo en el historial y los ingresos y gastos del presupuesto comenzarán nuevamente en cero. Tus movimientos y tu saldo general no se borrarán."
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = if (closingCycle) "Cerrando…" else "Cerrar ciclo",
                    onClick = {
                        viewModel.closeCurrentCycle { confirmingClose = false }
                    },
                    enabled = !closingCycle,
                )
            },
            dismissButton = {
                SecondaryButton("Cancelar", { confirmingClose = false })
            },
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
    currentClosingDays: List<Int>,
    onDismiss: () -> Unit,
    onSave: (String, BudgetPeriod, List<Int>) -> Unit,
) {
    val context = LocalContext.current
    var amount by remember(currentAmount) {
        mutableStateOf(currentAmount?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var period by remember(currentPeriod) { mutableStateOf(currentPeriod) }
    var closingDays by remember(currentClosingDays) {
        mutableStateOf(
            currentClosingDays.filter { it in 1..31 }.distinct().sorted().ifEmpty { listOf(15) }.map(Int::toString)
        )
    }
    val parsedDays = closingDays.mapNotNull(String::toIntOrNull).filter { it in 1..31 }.distinct()
    val valid = MoneyFormatter.parseToCents(amount)?.let { it > 0 } == true &&
        parsedDays.isNotEmpty() && parsedDays.size == closingDays.size

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
                    "Indica cuánto ganas, el periodo que quieres controlar y en qué días se cierra el ciclo.",
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DÍAS DE CIERRE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "El ciclo se cierra manual o automáticamente (si activas la opción) cuando la fecha coincida con uno de estos días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    closingDays.forEachIndexed { index, day ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = day,
                                onValueChange = { newValue ->
                                    closingDays = closingDays.toMutableList()
                                        .also { it[index] = newValue.filter(Char::isDigit).take(2) }
                                },
                                label = { Text("Día ${index + 1}") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            if (closingDays.size > 1) {
                                IconButton(
                                    onClick = {
                                        closingDays = closingDays.toMutableList().also { it.removeAt(index) }
                                    },
                                ) { Icon(Icons.Outlined.Close, "Quitar día ${index + 1}") }
                            }
                        }
                    }
                    TextButton(
                        onClick = { closingDays = closingDays + "" },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Text("Agregar día")
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Guardar",
                onClick = {
                    if (valid) onSave(amount, period, parsedDays.sorted())
                    else context.showToast("Introduce un monto válido y al menos un día de cierre válido")
                },
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
    )
}
