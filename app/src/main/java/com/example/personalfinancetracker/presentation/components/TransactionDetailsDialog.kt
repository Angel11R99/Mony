package com.example.personalfinancetracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(if (isExpense) "Detalle del gasto" else "Detalle del ingreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    (if (isExpense) "−" else "+") + MoneyFormatter.format(transaction.amountInCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                DetailField("Categoría", category?.name ?: "Sin categoría")
                DetailField("Fecha", transaction.date.format(dateFormatter))
                DetailField("Nota", transaction.description?.takeIf { it.isNotBlank() } ?: "Sin nota")
            }
        },
        confirmButton = { PrimaryButton("Cerrar", onDismiss) },
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            label.uppercase(),
            modifier = Modifier.weight(0.35f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(0.65f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
