package com.angel.mony.presentation.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.presentation.components.FinanceCard

@Composable
internal fun ChartLegend(label: String, amount: Long, percent: Int, color: Color) {
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
internal fun BalanceCard(report: StatisticsReport) {
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
internal fun ActivityCard(report: StatisticsReport) {
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
internal fun StatisticAmount(label: String, amount: Long, modifier: Modifier, isExpense: Boolean = false) {
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
internal fun RatioBar(ratio: Float) {
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
internal fun CategoryBar(statistic: CategoryStatistic, comparisonAmount: Long) {
    val limit = statistic.category.budgetLimitInCents?.takeIf { it > 0 }
    val effectiveComparison = limit ?: comparisonAmount
    val fraction =
        if (effectiveComparison <= 0) 0f else statistic.amountInCents.toFloat() / effectiveComparison
    val animatedFraction by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "categoryFraction")
    val summary = if (limit != null) {
        "${(fraction * 100).toInt()}% del límite (${MoneyFormatter.format(limit)}) · ${MoneyFormatter.format(statistic.amountInCents)}"
    } else {
        "${(fraction * 100).toInt()}% · ${MoneyFormatter.format(statistic.amountInCents)}"
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.angel.mony.ui.iconography.MonyIcon(
                com.angel.mony.ui.iconography.semanticIconForCategory(
                    statistic.category.icon,
                    statistic.category.name,
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                statistic.category.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(summary, style = MaterialTheme.typography.titleSmall)
        }
        Box(
            Modifier.fillMaxWidth().height(7.dp).clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(animatedFraction).height(7.dp)
                    .background(
                        if (limit != null && fraction > 1f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
    }
}



