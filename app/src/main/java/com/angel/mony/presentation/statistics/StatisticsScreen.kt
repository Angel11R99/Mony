package com.angel.mony.presentation.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.LoadingContent
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.SkeletonBox
import com.angel.mony.presentation.components.SkeletonCard
import com.angel.mony.presentation.components.SkeletonCircle
import com.angel.mony.presentation.components.SkeletonHost
import com.angel.mony.presentation.components.SkeletonLine
import com.angel.mony.presentation.components.SkeletonTone
import com.angel.mony.presentation.components.StaggeredReveal
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.presentation.components.localDateNullableSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class StatisticsReports(
    val current: StatisticsReport,
    val previous: StatisticsReport?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    onSettings: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recommendationState by viewModel.recommendationState.collectAsStateWithLifecycle()
    val isGeneratingRecommendation = recommendationState.isGenerating
    val recommendationHistory by viewModel.recommendationHistory.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(StatisticsRange.CURRENT_BUDGET) }
    var cycleIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var customStart by rememberSaveable(stateSaver = localDateNullableSaver) { mutableStateOf<LocalDate?>(null) }
    var customEnd by rememberSaveable(stateSaver = localDateNullableSaver) { mutableStateOf<LocalDate?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showRecommendations by remember { mutableStateOf(false) }
    var showRecommendationHistory by remember { mutableStateOf(false) }
    var recommendationHistorySearch by rememberSaveable { mutableStateOf("") }
    var expandedRecommendationId by remember { mutableStateOf<Long?>(null) }
    val visibleRecommendationHistory = remember(recommendationHistory, recommendationHistorySearch) {
        val query = recommendationHistorySearch.trim()
        if (query.isBlank()) recommendationHistory.take(5)
        else recommendationHistory.filter { it.scope.contains(query, ignoreCase = true) || it.response.contains(query, ignoreCase = true) }
    }
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
    val previousPeriod = remember(range, selectedCycle, state.budget, period) {
        previousStatisticsPeriod(
            range = range,
            selectedCycle = selectedCycle,
            budget = state.budget,
            current = period,
        )
    }
    val reports by produceState<StatisticsReports?>(
        initialValue = null,
        state.transactions,
        state.categories,
        period,
        previousPeriod,
    ) {
        if (!state.isReady) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            StatisticsReports(
                current = calculateStatistics(
                    transactions = state.transactions,
                    categories = state.categories,
                    startDate = period.startDate,
                    endDate = period.endDate,
                ),
                previous = previousPeriod?.let { previous ->
                    calculateStatistics(
                        transactions = state.transactions,
                        categories = state.categories,
                        startDate = previous.startDate,
                        endDate = previous.endDate,
                    )
                },
            )
        }
    }
    val report = reports?.current ?: StatisticsReport(
        incomeInCents = 0,
        expenseInCents = 0,
        transactionCount = 0,
        averageExpenseInCents = 0,
        expenseByCategory = emptyList(),
    )
    val previousReport = reports?.previous
    val selectedCategory = categoryId?.let(state.categories::get)
    val selectedStatistic = report.expenseByCategory.firstOrNull { it.category.id == categoryId }
    val categoryFocusLimit = selectedStatistic?.category?.budgetLimitInCents?.takeIf { it > 0 }
    val comparisonAmount = if (isBudgetCycleFilter) {
        state.budget?.amountInCents ?: report.incomeInCents
    } else {
        report.expenseInCents
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Estadísticas") },
                actions = {
                    GlobalOutlinedIconButton(
                        semanticIcon = MonyIcon.Sparkles,
                        contentDescription = if (isGeneratingRecommendation) "Recomendaciones con IA; analizando" else "Recomendaciones con IA",
                        loading = isGeneratingRecommendation,
                        onClick = {
                        showRecommendations = true
                        val scope = "$periodLabel · ${selectedCategory?.name ?: "Todas las categorías"}"
                        val hasVisibleData = report.transactionCount > 0 && (categoryId == null || selectedStatistic != null)
                        val resultMatchesCurrentScope = recommendationState.text != null && recommendationState.scope == scope
                        if (!isGeneratingRecommendation && !resultMatchesCurrentScope && hasVisibleData) {
                            val moduleContext = if (selectedCategory == null) buildList {
                                state.budget?.let { add("Presupuesto configurado: ${MoneyFormatter.format(it.amountInCents)}.") }
                                state.fixedEntries.filter { it.isActive }.takeIf { it.isNotEmpty() }?.let { entries ->
                                    add("Fijos activos: ${entries.size}; total registrado como referencia ${MoneyFormatter.format(entries.sumOf { it.amountInCents })}.")
                                }
                                state.pendingEntries.filterNot { it.isDone }.takeIf { it.isNotEmpty() }?.let { entries ->
                                    add("Pendientes abiertos: ${entries.size}; monto ${MoneyFormatter.format(entries.sumOf { it.amountInCents })}.")
                                }
                                state.savingsGoals.filter { it.isActive }.takeIf { it.isNotEmpty() }?.let { goals ->
                                    add("Metas de ahorro activas: ${goals.size}; ${goals.joinToString { "${it.goal.name} (${it.percent}%)" }}.")
                                }
                                state.shoppingLists.filter { it.status != ShoppingListStatus.COMPLETED }.takeIf { it.isNotEmpty() }?.let { lists ->
                                    add("Listas de compra sin completar: ${lists.size}; presupuestos disponibles ${MoneyFormatter.format(lists.mapNotNull { it.budgetInCents }.sum())}.")
                                }
                                state.fortnightPlans.filter { !it.plan.isClosed }.takeIf { it.isNotEmpty() }?.let { plans ->
                                    add("Planes de ciclo abiertos: ${plans.size}; pendiente planificado ${MoneyFormatter.format(plans.sumOf { it.totalPendingInCents })}.")
                                }
                            } else emptyList()
                            val prompt = statisticsRecommendationPrompt(periodLabel, report, selectedCategory, previousReport, moduleContext)
                            viewModel.generateRecommendation(prompt, scope)
                        }
                    },
                        size = 54.dp,
                    )
                    Spacer(Modifier.width(8.dp))
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
        val isLoadingReport = !state.isReady || reports == null
        SkeletonHost(isLoading = isLoadingReport) {
            LoadingContent(
                isLoading = isLoadingReport,
                modifier = Modifier.padding(padding),
                minVisibleMillis = 0,
                skeleton = { StatisticsSkeleton() },
            ) {
                StaggeredReveal(
                    screenKey = "statistics",
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 24.dp),
                    staggerMillis = 40,
                    durationMillis = 180,
                    slideOffsetDp = 8.dp,
                ) {
                    add {
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

                    add {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            BalanceCard(report)
                            ActivityCard(report)
                        }
                    }

                    add {
                        IncomeExpenseChart(report)
                    }

                    add {
                        val comparisonPeriod = previousPeriod
                        val comparisonReport = previousReport
                        if (comparisonPeriod != null && comparisonReport != null) {
                            TrendComparisonCard(current = report, previous = comparisonReport, previousPeriod = comparisonPeriod)
                        }
                    }

                    add {
                        if (selectedCategory != null) {
                            CategoryFocusCard(
                                category = selectedCategory,
                                amountInCents = selectedStatistic?.amountInCents ?: 0,
                                comparisonAmountInCents = categoryFocusLimit ?: comparisonAmount,
                                periodLabel = periodLabel,
                            )
                        }
                    }

                    add {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                if (selectedCategory == null) "GASTOS POR CATEGORÍA" else "DETALLE DE CATEGORÍA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val visibleStatistics = if (categoryId == null) {
                                report.expenseByCategory
                            } else {
                                report.expenseByCategory.filter { it.category.id == categoryId }
                            }
                            if (visibleStatistics.isEmpty()) {
                                Text(
                                    if (selectedCategory == null) "No hay gastos registrados en este periodo."
                                    else "No hay gastos de ${selectedCategory.name} en este periodo.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    visibleStatistics.forEach { statistic ->
                                        CategoryBar(
                                            statistic = statistic,
                                            comparisonAmount = comparisonAmount,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecommendations) {
        ModalBottomSheet(
            onDismissRequest = { showRecommendations = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showRecommendationHistory) {
                        GlobalOutlinedIconButton(
                            semanticIcon = MonyIcon.Back,
                            contentDescription = "Volver a recomendaciones",
                            onClick = { showRecommendationHistory = false },
                            size = 42.dp,
                        )
                    } else {
                        com.angel.mony.ui.iconography.MonyIcon(MonyIcon.Sparkles, contentDescription = null, size = 26.dp)
                    }
                    Text(
                        if (showRecommendationHistory) "Historial de recomendaciones" else "Recomendaciones",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (!showRecommendationHistory) GlobalOutlinedIconButton(
                        semanticIcon = MonyIcon.History,
                        contentDescription = "Historial de recomendaciones",
                        onClick = { showRecommendationHistory = true },
                        size = 42.dp,
                    )
                }
                if (showRecommendationHistory) {
                    if (recommendationHistory.isEmpty()) {
                        Text("Todavía no hay recomendaciones guardadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = recommendationHistorySearch,
                                onValueChange = { recommendationHistorySearch = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Buscar en el historial") },
                                placeholder = { Text("Texto o periodo") },
                                leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(MonyIcon.Search, contentDescription = null) },
                                singleLine = true,
                            )
                            if (recommendationHistorySearch.isBlank()) {
                                Text("Últimas ${visibleRecommendationHistory.size} recomendaciones · busca para ver anteriores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("${visibleRecommendationHistory.size} resultados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (visibleRecommendationHistory.isEmpty()) {
                                Text("No hay recomendaciones que coincidan con la búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            visibleRecommendationHistory.forEach { entry ->
                                val expanded = expandedRecommendationId == entry.createdAt
                                FinanceCard(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Contraer recomendación" else "Ver recomendación completa") {
                                            expandedRecommendationId = if (expanded) null else entry.createdAt
                                        }.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault())
                                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(entry.scope, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        if (expanded) RecommendationText(entry.response)
                                        else Text(
                                            entry.response,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (expanded) "Toca para resumir" else "Toca para leer completa",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                Text("Generadas en este dispositivo a partir de las estadísticas visibles.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    isGeneratingRecommendation -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Analizando tus estadísticas…", Modifier.padding(start = 12.dp))
                    }
                    report.transactionCount == 0 || (selectedCategory != null && selectedStatistic == null) -> Text("No hay movimientos en este filtro para analizar. Registra uno y vuelve a intentarlo.",
                        style = MaterialTheme.typography.bodyMedium)
                    recommendationState.error != null -> Text(
                        recommendationState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    recommendationState.text != null -> RecommendationText(recommendationState.text!!)
                }
                }
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
private fun RecommendationText(text: String) {
    val formatted = buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val opening = text.indexOf("**", cursor)
            if (opening < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, opening))
            val closing = text.indexOf("**", opening + 2)
            if (closing < 0) {
                append(text.substring(opening))
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text.substring(opening + 2, closing))
            }
            cursor = closing + 2
        }
    }
    Text(formatted, style = MaterialTheme.typography.bodyLarge)
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
        com.angel.mony.ui.iconography.MonyIcon(
            com.angel.mony.ui.iconography.MonyIcon.Filter,
            contentDescription = null,
        )
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
                        "Elige qué quieres analizar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    com.angel.mony.ui.iconography.MonyIcon(
                        com.angel.mony.ui.iconography.MonyIcon.Close,
                        contentDescription = "Cerrar filtros",
                    )
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
            com.angel.mony.ui.iconography.MonyIcon(
                com.angel.mony.ui.iconography.MonyIcon.Calendar,
                contentDescription = null,
            )
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
            com.angel.mony.ui.iconography.MonyIcon(
                com.angel.mony.ui.iconography.MonyIcon.TrendFlat,
                contentDescription = "Sin cambios",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.TrendUp,
                    contentDescription = "Subió",
                    tint = color,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.TrendDown,
                    contentDescription = "Bajó",
                    tint = color,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
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
private fun StatisticsSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SkeletonBox(Modifier.fillMaxWidth().height(52.dp), MaterialTheme.shapes.small)
        }
        item {
            SkeletonCard(contentPadding = PaddingValues(16.dp)) {
                SkeletonLine(Modifier.width(150.dp), height = 11.dp, tone = SkeletonTone.Accent)
                SkeletonLine(Modifier.fillMaxWidth(0.5f), height = 36.dp, tone = SkeletonTone.Accent)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            SkeletonLine(Modifier.fillMaxWidth(0.7f), height = 10.dp, tone = SkeletonTone.Accent)
                            SkeletonLine(Modifier.fillMaxWidth(), height = 20.dp, tone = SkeletonTone.Accent)
                        }
                    }
                }
                SkeletonLine(Modifier.fillMaxWidth(0.6f), height = 11.dp, tone = SkeletonTone.Accent)
                SkeletonLine(Modifier.fillMaxWidth(), height = 8.dp, tone = SkeletonTone.Accent)
            }
        }
        item {
            SkeletonCard(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonLine(Modifier.width(140.dp), height = 11.dp, tone = SkeletonTone.Accent)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonCircle(132.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(2) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                SkeletonBox(Modifier.size(10.dp), MaterialTheme.shapes.extraSmall)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SkeletonLine(Modifier.fillMaxWidth(0.6f), height = 12.dp, tone = SkeletonTone.Accent)
                                    SkeletonLine(Modifier.fillMaxWidth(0.45f), height = 12.dp, tone = SkeletonTone.Accent)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SkeletonCard(contentPadding = PaddingValues(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        SkeletonLine(Modifier.width(90.dp), height = 10.dp, tone = SkeletonTone.Accent)
                        SkeletonLine(Modifier.width(56.dp), height = 26.dp, tone = SkeletonTone.Accent)
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        SkeletonLine(Modifier.width(100.dp), height = 10.dp, tone = SkeletonTone.Accent)
                        SkeletonLine(Modifier.width(72.dp), height = 22.dp, tone = SkeletonTone.Accent)
                    }
                }
            }
        }
        item {
            SkeletonLine(Modifier.width(170.dp), height = 13.dp, tone = SkeletonTone.Base)
        }
        items(4) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonLine(Modifier.width(110.dp), height = 14.dp)
                    SkeletonLine(Modifier.width(130.dp), height = 14.dp)
                }
                SkeletonLine(Modifier.fillMaxWidth(), height = 7.dp, tone = SkeletonTone.Accent)
            }
        }
    }
}
