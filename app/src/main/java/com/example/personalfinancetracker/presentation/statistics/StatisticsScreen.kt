package com.example.personalfinancetracker.presentation.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.activeBudgetPeriod
import com.example.personalfinancetracker.domain.model.belongsToActiveBudgetCycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    onSettings: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var range by remember { mutableStateOf(StatisticsRange.CURRENT_BUDGET) }
    var cycleIndex by remember { mutableStateOf<Int?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var draftRange by remember { mutableStateOf(range) }
    var draftCycleIndex by remember { mutableStateOf(cycleIndex) }
    var draftCategoryId by remember { mutableStateOf(categoryId) }
    var draftCustomStart by remember { mutableStateOf(customStart) }
    var draftCustomEnd by remember { mutableStateOf(customEnd) }
    val cycleSchedules = state.budget?.cycleSchedules.orEmpty()
    val selectedCycle = cycleIndex?.let(cycleSchedules::getOrNull)
    val periodLabel = when {
        selectedCycle != null -> selectedCycle.displayLabel(cycleIndex ?: 0)
        range == StatisticsRange.CUSTOM -> customRangeLabel(customStart, customEnd)
        else -> range.displayLabel(state.budget)
    }
    LaunchedEffect(cycleSchedules, cycleIndex) {
        if (cycleIndex != null && selectedCycle == null) cycleIndex = null
    }
    val expenseCategories = remember(state.categories) {
        state.categories.values
            .filter { it.type == TransactionType.EXPENSE }
            .sortedBy(Category::name)
    }
    val period = remember(range, selectedCycle, state.budget, customStart, customEnd) {
        selectedCycle?.let { statisticsPeriod(it) }
            ?: statisticsPeriod(
                range = range,
                budget = state.budget,
                customStart = customStart,
                customEnd = customEnd,
            )
    }
    val isBudgetCycleFilter = selectedCycle != null || range == StatisticsRange.CURRENT_BUDGET
    val periodTransactions = remember(state.transactions, state.budget, isBudgetCycleFilter, period) {
        val activePeriod = activeBudgetPeriod(state.budget)
        if (!isBudgetCycleFilter ||
            period.startDate != activePeriod.start ||
            period.endDate != activePeriod.endInclusive
        ) state.transactions
        else {
            state.transactions.filter { it.belongsToActiveBudgetCycle(state.budget, activePeriod) }
        }
    }
    val report = remember(periodTransactions, state.categories, period) {
        calculateStatistics(
            transactions = periodTransactions,
            categories = state.categories,
            startDate = period.startDate,
            endDate = period.endDate,
        )
    }
    val previousPeriod = remember(range, selectedCycle, state.budget, period) {
        previousStatisticsPeriod(
            range = range,
            selectedCycle = selectedCycle,
            budget = state.budget,
            current = period,
        )
    }
    val previousReport = previousPeriod?.let { previous ->
        calculateStatistics(
            transactions = periodTransactions,
            categories = state.categories,
            startDate = previous.startDate,
            endDate = previous.endDate,
        )
    }
    val selectedCategory = categoryId?.let(state.categories::get)
    val selectedStatistic = report.expenseByCategory.firstOrNull { it.category.id == categoryId }
    val comparisonAmount = if (isBudgetCycleFilter) {
        state.budget?.amountInCents ?: report.incomeInCents
    } else {
        report.expenseInCents
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Estadísticas") },
                actions = {
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatisticsFilterButton(
                    period = periodLabel,
                    category = selectedCategory?.name ?: "Todas las categorías",
                    onClick = {
                        draftRange = range
                        draftCycleIndex = cycleIndex
                        draftCategoryId = categoryId
                        draftCustomStart = customStart
                        draftCustomEnd = customEnd
                        showFilters = true
                    },
                )
            }
            item { BalanceCard(report) }
            item { IncomeExpenseChart(report) }
            item { ActivityCard(report) }
            val comparisonPeriod = previousPeriod
            val comparisonReport = previousReport
            if (comparisonPeriod != null && comparisonReport != null) {
                item { TrendComparisonCard(current = report, previous = comparisonReport, previousPeriod = comparisonPeriod) }
            }
            if (selectedCategory != null) {
                item {
                    CategoryFocusCard(
                        category = selectedCategory,
                        amountInCents = selectedStatistic?.amountInCents ?: 0,
                        comparisonAmountInCents = comparisonAmount,
                        periodLabel = periodLabel,
                    )
                }
            }
            item {
                Text(
                    if (selectedCategory == null) "GASTOS POR CATEGORÍA" else "DETALLE DE CATEGORÍA",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            val visibleStatistics = if (categoryId == null) {
                report.expenseByCategory
            } else {
                report.expenseByCategory.filter { it.category.id == categoryId }
            }
            if (visibleStatistics.isEmpty()) {
                item {
                    Text(
                        if (selectedCategory == null) "No hay gastos registrados en este periodo."
                        else "No hay gastos de ${selectedCategory.name} en este periodo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visibleStatistics, key = { it.category.id }) { statistic ->
                CategoryBar(
                    statistic = statistic,
                    comparisonAmount = comparisonAmount,
                )
            }
        }
    }

    if (showFilters) {
        val draftValid = draftRange != StatisticsRange.CUSTOM ||
            (draftCustomStart != null && draftCustomEnd != null && !draftCustomStart!!.isAfter(draftCustomEnd!!))
        StatisticsFilterSheet(
            ranges = StatisticsRange.entries,
            selectedRange = draftRange,
            onRangeChange = {
                draftRange = it
                draftCycleIndex = null
            },
            cycles = cycleSchedules,
            selectedCycleIndex = draftCycleIndex,
            onCycleChange = { draftCycleIndex = it },
            customStartDate = draftCustomStart,
            onCustomStartDateChange = { date ->
                draftCustomStart = date
                if (date != null && draftCustomEnd != null && draftCustomEnd!!.isBefore(date)) {
                    draftCustomEnd = date
                }
            },
            customEndDate = draftCustomEnd,
            onCustomEndDateChange = { date ->
                draftCustomEnd = date
                if (date != null && draftCustomStart != null && draftCustomStart!!.isAfter(date)) {
                    draftCustomStart = date
                }
            },
            categories = expenseCategories,
            selectedCategoryId = draftCategoryId,
            onCategoryChange = { draftCategoryId = it },
            rangeLabel = { it.displayLabel(state.budget) },
            applyEnabled = draftValid,
            onClear = {
                draftRange = StatisticsRange.CURRENT_BUDGET
                draftCycleIndex = null
                draftCategoryId = null
                draftCustomStart = null
                draftCustomEnd = null
            },
            onApply = {
                range = draftRange
                cycleIndex = draftCycleIndex
                categoryId = draftCategoryId
                customStart = draftCustomStart
                customEnd = draftCustomEnd
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun StatisticsFilterButton(
    period: String,
    category: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Icon(Icons.Outlined.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.Start) {
            Text("FILTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("$period · $category", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StatisticsFilterSheet(
    ranges: List<StatisticsRange>,
    selectedRange: StatisticsRange,
    onRangeChange: (StatisticsRange) -> Unit,
    cycles: List<BudgetCycleSchedule>,
    selectedCycleIndex: Int?,
    onCycleChange: (Int) -> Unit,
    customStartDate: LocalDate?,
    onCustomStartDateChange: (LocalDate?) -> Unit,
    customEndDate: LocalDate?,
    onCustomEndDateChange: (LocalDate?) -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    rangeLabel: (StatisticsRange) -> String,
    applyEnabled: Boolean,
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
                    Text(
                        "Elige qué quieres analizar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar filtros")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("PERIODO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ranges.forEach { option ->
                    FilterChip(
                        selected = selectedCycleIndex == null && selectedRange == option,
                        onClick = { onRangeChange(option) },
                        label = { Text(rangeLabel(option)) },
                        shape = MaterialTheme.shapes.small,
                        colors = financeFilterChipColors(),
                    )
                }
            }
            if (cycles.isNotEmpty()) {
                Text("MIS CICLOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cycles.forEachIndexed { index, cycle ->
                        FilterChip(
                            selected = selectedCycleIndex == index,
                            onClick = { onCycleChange(index) },
                            label = { Text(cycle.displayLabel(index)) },
                            shape = MaterialTheme.shapes.small,
                            colors = financeFilterChipColors(),
                        )
                    }
                }
            }
            if (selectedRange == StatisticsRange.CUSTOM) {
                Text("RANGO PERSONALIZADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatisticsDateField(
                        label = "Desde",
                        value = customStartDate,
                        onValueChange = onCustomStartDateChange,
                        modifier = Modifier.weight(1f),
                    )
                    StatisticsDateField(
                        label = "Hasta",
                        value = customEndDate,
                        onValueChange = onCustomEndDateChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text("CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("Todas") },
                    shape = MaterialTheme.shapes.small,
                    colors = financeFilterChipColors(),
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategoryChange(category.id) },
                        label = { Text(category.name) },
                        shape = MaterialTheme.shapes.small,
                        colors = financeFilterChipColors(),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                PrimaryButton("Aplicar", onApply, Modifier.weight(1f), enabled = applyEnabled)
            }
        }
    }
}

@Composable
private fun financeFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
)

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsDateField(
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
                Text(
                    value?.format(formatter) ?: "Elige una fecha",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
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
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun TrendComparisonCard(
    current: StatisticsReport,
    previous: StatisticsReport,
    previousPeriod: StatisticsPeriod,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "COMPARACIÓN CON EL PERIODO ANTERIOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            TrendRow("GASTOS", current.expenseInCents, previous.expenseInCents, upIsGood = false)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TrendRow("INGRESOS", current.incomeInCents, previous.incomeInCents, upIsGood = true)
            Text(
                "Periodo anterior: ${previousPeriod.startDate.orDash()} – ${previousPeriod.endDate?.format(trendDateFormatter) ?: "hoy"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val trendDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun LocalDate?.orDash(): String = this?.format(trendDateFormatter) ?: "—"

@Composable
private fun TrendRow(label: String, current: Long, previous: Long, upIsGood: Boolean) {
    val delta = trendDelta(current, previous)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(MoneyFormatter.format(current), style = MaterialTheme.typography.titleLarge)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TrendDeltaLabel(delta, upIsGood)
            Text(
                "Antes ${MoneyFormatter.format(previous)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendDeltaLabel(delta: TrendDelta, upIsGood: Boolean) {
    val goodColor = MaterialTheme.colorScheme.primary
    val badColor = MaterialTheme.colorScheme.error
    when (delta.direction) {
        TrendDirection.NEW -> Text(
            "Nuevo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = goodColor,
        )
        TrendDirection.FLAT -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Remove,
                contentDescription = "Sin cambios",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                if (delta.percent != null) "0%" else "Sin cambios",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TrendDirection.UP -> {
            val color = if (upIsGood) goodColor else badColor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ArrowDropUp,
                    contentDescription = "Subió",
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    "+${delta.percent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
        TrendDirection.DOWN -> {
            val color = if (upIsGood) badColor else goodColor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = "Bajó",
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    "−${delta.percent?.toString()?.trimStart('-')}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun CategoryFocusCard(
    category: Category,
    amountInCents: Long,
    comparisonAmountInCents: Long,
    periodLabel: String,
) {
    val fraction = if (comparisonAmountInCents <= 0) 0f
    else amountInCents.toFloat() / comparisonAmountInCents
    val percent = (fraction * 100).toInt()
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${category.name.uppercase()} · ${periodLabel.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(MoneyFormatter.format(amountInCents), style = MaterialTheme.typography.displaySmall)
            Text(
                "$percent% de ${MoneyFormatter.format(comparisonAmountInCents)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RatioBar(fraction)
        }
    }
}

@Composable
private fun IncomeExpenseChart(report: StatisticsReport) {
    val total = report.incomeInCents + report.expenseInCents
    val incomeFraction = if (total <= 0) 0f else report.incomeInCents.toFloat() / total
    val animatedIncome by animateFloatAsState(incomeFraction.coerceIn(0f, 1f), label = "incomeArc")
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surface

    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("INGRESOS VS. GASTOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = 18.dp.toPx()
                        val inset = strokeWidth / 2
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = style,
                        )
                        if (total > 0) {
                            val incomeSweep = 360f * animatedIncome
                            drawArc(
                                color = incomeColor,
                                startAngle = -90f,
                                sweepAngle = incomeSweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = style,
                            )
                            drawArc(
                                color = expenseColor,
                                startAngle = -90f + incomeSweep,
                                sweepAngle = 360f - incomeSweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = style,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(report.transactionCount.toString(), style = MaterialTheme.typography.headlineMedium)
                        Text("REGISTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChartLegend(
                        label = "Ingresos",
                        amount = report.incomeInCents,
                        percent = if (total <= 0) 0 else (incomeFraction * 100).toInt(),
                        color = incomeColor,
                    )
                    ChartLegend(
                        label = "Gastos",
                        amount = report.expenseInCents,
                        percent = if (total <= 0) 0 else (100 - incomeFraction * 100).toInt(),
                        color = expenseColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, amount: Long, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
        Column(Modifier.weight(1f)) {
            Text("$label · $percent%", style = MaterialTheme.typography.labelLarge)
            Text(
                MoneyFormatter.format(amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalanceCard(report: StatisticsReport) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("BALANCE DEL PERIODO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(
                MoneyFormatter.format(report.balanceInCents),
                style = MaterialTheme.typography.displaySmall,
                color = if (report.balanceInCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatisticAmount("INGRESOS", report.incomeInCents, Modifier.weight(1f))
                StatisticAmount("GASTOS", report.expenseInCents, Modifier.weight(1f), isExpense = true)
            }
            if (report.incomeInCents > 0) {
                val percent = (report.expenseRatio * 100).toInt()
                Text(
                    "Has utilizado $percent% de tus ingresos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RatioBar(report.expenseRatio)
            }
        }
    }
}

@Composable
private fun ActivityCard(report: StatisticsReport) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("MOVIMIENTOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(report.transactionCount.toString(), style = MaterialTheme.typography.headlineMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("GASTO PROMEDIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    MoneyFormatter.format(report.averageExpenseInCents),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun StatisticAmount(label: String, amount: Long, modifier: Modifier, isExpense: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleLarge,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RatioBar(ratio: Float) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val animatedRatio by animateFloatAsState(ratio.coerceIn(0f, 1f), label = "expenseRatio")
        Box(
            Modifier.fillMaxWidth(animatedRatio).height(8.dp)
                .background(
                    if (ratio > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.extraSmall,
                ),
        )
    }
}

@Composable
private fun CategoryBar(statistic: CategoryStatistic, comparisonAmount: Long) {
    val fraction = if (comparisonAmount <= 0) 0f else statistic.amountInCents.toFloat() / comparisonAmount
    val animatedFraction by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "categoryFraction")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(statistic.category.name.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${(fraction * 100).toInt()}% · ${MoneyFormatter.format(statistic.amountInCents)}",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(
            Modifier.fillMaxWidth().height(7.dp).clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(animatedFraction).height(7.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
