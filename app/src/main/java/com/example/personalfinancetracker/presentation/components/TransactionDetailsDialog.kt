package com.example.personalfinancetracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
        title = {
            Text(
                buildAnnotatedString {
                    append("Detalle del ")
                    withStyle(SpanStyle(color = movementColor, fontWeight = FontWeight.Bold)) {
                        append(if (isExpense) "gasto" else "ingreso")
                    }
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    (if (isExpense) "−" else "+") + MoneyFormatter.format(transaction.amountInCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = movementColor,
                )
                DetailField(
                    label = "Categoría",
                    value = category?.name ?: "Sin categoría",
                    labelColor = MaterialTheme.colorScheme.secondary,
                    valueColor = movementColor,
                )
                DetailField(
                    label = "Fecha",
                    value = transaction.date.format(dateFormatter),
                    labelColor = MaterialTheme.colorScheme.secondary,
                )
                DetailField(
                    label = "Nota",
                    value = transaction.description?.takeIf { it.isNotBlank() } ?: "Sin nota",
                    labelColor = MaterialTheme.colorScheme.secondary,
                    valueColor = if (transaction.description.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
        confirmButton = { PrimaryButton("Cerrar", onDismiss) },
    )
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            label.uppercase(),
            modifier = Modifier.width(88.dp).alignByBaseline(),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Text(
            value,
            modifier = Modifier.weight(1f).alignByBaseline(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
