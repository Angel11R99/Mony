package com.angel.mony.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast

@Composable
fun BudgetAmountDialog(
    currentAmount: Long?,
    isSaving: Boolean,
    description: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var amount by remember(currentAmount) {
        mutableStateOf(currentAmount?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    val formState = remember { FormState() }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Tu presupuesto", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FinanceTextField(
                    value = amount,
                    onValueChange = { amount = sanitizeAmountInput(it); formState.clearError("amount") },
                    label = "Monto (RD$)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                    isError = formState.hasError("amount"),
                    errorMessage = formState["amount"],
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Guardando…" else "Guardar",
                onClick = {
                    formState.clearAll()
                    val cents = MoneyFormatter.parseToCents(amount)
                    if (cents == null || cents <= 0) formState.setError("amount", "El monto debe ser mayor que cero")
                    if (!formState.isValid()) return@PrimaryButton
                    onSave(amount)
                },
                enabled = !isSaving,
            )
        },
        dismissButton = {
            if (!isSaving) SecondaryButton("Cancelar", onDismiss)
        },
    )
}
