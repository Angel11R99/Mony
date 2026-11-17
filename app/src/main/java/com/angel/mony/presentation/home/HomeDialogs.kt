package com.angel.mony.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.DateRange
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)

@Composable
internal fun CloseCycleDialog(
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
internal fun BudgetHistoryDialog(cycles: List<BudgetCycle>, onDismiss: () -> Unit) {
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
internal fun HistoryAmount(label: String, amount: Long, isExpense: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
