package com.angel.mony.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.iconography.semanticIconForCategory
import java.time.format.DateTimeFormatter

private val transactionRowDateFormatter = DateTimeFormatter.ofPattern("dd MMM")

@Composable
fun TransactionRow(
    transaction: FinanceTransaction,
    category: Category?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val secondaryText = remember(transaction.description, transaction.date) {
        transaction.description ?: transaction.date.format(transactionRowDateFormatter)
    }
    val formattedAmount = remember(transaction.amountInCents, transaction.type) {
        val sign = if (transaction.type == TransactionType.INCOME) "+" else "−"
        "$sign${MoneyFormatter.format(transaction.amountInCents)}"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonyIcon(
            icon = category?.let { semanticIconForCategory(it.icon, it.name) }
                ?: if (transaction.type == TransactionType.INCOME) MonyIcon.Income else MonyIcon.Expense,
            contentDescription = null,
        )
        Column(Modifier.weight(1f)) {
            Text(category?.name ?: "Sin categoría", style = MaterialTheme.typography.bodyLarge)
            Text(
                secondaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formattedAmount,
            color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
