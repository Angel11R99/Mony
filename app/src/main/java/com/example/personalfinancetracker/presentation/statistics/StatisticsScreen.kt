package com.example.personalfinancetracker.presentation.statistics

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.presentation.components.FinanceCard
import java.time.LocalDate
import java.time.YearMonth

private enum class StatisticsRange(val label: String) {
    CURRENT_MONTH("Este mes"),
    ALL_TIME("Todo"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var range by remember { mutableStateOf(StatisticsRange.CURRENT_MONTH) }
    val monthStart = remember { YearMonth.now().atDay(1) }
    val report = remember(state, range) {
        calculateStatistics(
            transactions = state.transactions,
            categories = state.categories,
            startDate = if (range == StatisticsRange.CURRENT_MONTH) monthStart else null,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatisticsRange.entries.forEach { option ->
                        FilterChip(
                            selected = range == option,
                            onClick = { range = option },
                            label = { Text(option.label) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }
            item { BalanceCard(report) }
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
            Text(MoneyFormatter.format(statistic.amountInCents), style = MaterialTheme.typography.titleSmall)
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
