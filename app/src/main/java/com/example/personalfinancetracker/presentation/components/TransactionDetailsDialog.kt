package com.example.personalfinancetracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionDetailsDialog(
    transaction: FinanceTransaction,
    category: Category?,
    onDismiss: () -> Unit,
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val movementColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-DO"))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = movementColor,
            )
        },
        title = { Text("Detalle del ${if (isExpense) "gasto" else "ingreso"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    (if (isExpense) "−" else "+") + MoneyFormatter.format(transaction.amountInCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = movementColor,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FinanceDetailRow(
                    label = "Categoría",
                    value = category?.name ?: "Sin categoría",
                    valueColor = movementColor,
                )
                FinanceDetailRow(
                    label = "Fecha",
                    value = transaction.date.format(dateFormatter),
                )
                transaction.description?.takeIf { it.isNotBlank() }?.let { note ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "NOTA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { PrimaryButton("Cerrar", onDismiss) },
    )
}
