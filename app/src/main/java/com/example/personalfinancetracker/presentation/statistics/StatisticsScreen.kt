package com.example.personalfinancetracker.presentation.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.presentation.components.FinanceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var range by remember { mutableStateOf(StatisticsRange.CURRENT_MONTH) }
    val period = remember(range) { statisticsPeriod(range) }
    val report = remember(state, period) {
        calculateStatistics(
            transactions = state.transactions,
            categories = state.categories,
            startDate = period.startDate,
            endDate = period.endDate,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ESTADÍSTICAS", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StatisticsRange.entries) { option ->
                        FilterChip(
                            selected = range == option,
                            onClick = { range = option },
                            label = { Text(option.label) },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }
            item { BalanceCard(report) }
            item { IncomeExpenseChart(report) }
            item { ActivityCard(report) }
            item {
                Text("GASTOS POR CATEGORÍA", style = MaterialTheme.typography.titleLarge)
            }
            if (report.expenseByCategory.isEmpty()) {
                item {
                    Text(
                        "No hay gastos registrados en este periodo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(report.expenseByCategory, key = { it.category.id }) { statistic ->
                CategoryBar(
                    statistic = statistic,
                    totalExpense = report.expenseInCents,
                )
            }
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
private fun CategoryBar(statistic: CategoryStatistic, totalExpense: Long) {
    val fraction = if (totalExpense <= 0) 0f else statistic.amountInCents.toFloat() / totalExpense
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
