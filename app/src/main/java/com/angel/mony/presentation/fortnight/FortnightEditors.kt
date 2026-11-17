package com.angel.mony.presentation.fortnight

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.FortnightItemType
import com.angel.mony.domain.model.label
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceSelectionField
import com.angel.mony.presentation.components.FinanceSelectionSheet
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.SelectionOption
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreatePlanSheet(
    state: FortnightUiState,
    budgetText: String,
    isMonthly: Boolean,
    isSaving: Boolean,
    onBudgetChange: (String) -> Unit,
    onCreate: (Set<Long>, Map<Long, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTemplates by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val categoryAmounts = remember { mutableStateOf(emptyMap<Long, String>()) }
    var expandedCategories by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isMonthly) "Plan del mes" else "Plan del ciclo",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "El presupuesto solo guía tu planificación: no registra ningún gasto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FinanceTextField(
                value = budgetText,
                onValueChange = { onBudgetChange(sanitizeAmountInput(it)) },
                label = if (isMonthly) "Presupuesto del mes" else "Presupuesto del ciclo",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = AmountVisualTransformation,
            )

            if (state.applicableTemplates.isNotEmpty()) {
                Text("Desde plantillas", style = MaterialTheme.typography.labelLarge)
                state.applicableTemplates.forEach { template ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = template.id in selectedTemplates,
                                onClick = {
                                    selectedTemplates = if (template.id in selectedTemplates) {
                                        selectedTemplates - template.id
                                    } else {
                                        selectedTemplates + template.id
                                    }
                                },
                                label = { Text(template.description, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                MoneyFormatter.format(template.amountFor(state.slot) ?: 0L),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Categorías",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { expandedCategories = !expandedCategories }) {
                        Text(if (expandedCategories) "Ocultar" else "Mostrar")
                    }
                }
                if (expandedCategories) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.categories.forEach { category ->
                            val selected = category.id in categoryAmounts.value
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    categoryAmounts.value = if (selected) {
                                        categoryAmounts.value - category.id
                                    } else {
                                        categoryAmounts.value + (category.id to "")
                                    }
                                },
                                label = {
                                    Text(category.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                        if (state.categories.size % 2 != 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    val selectedCategories = state.categories.filter { it.id in categoryAmounts.value }
                    if (selectedCategories.isNotEmpty()) {
                        Text("Monto por categoría", style = MaterialTheme.typography.labelLarge)
                        selectedCategories.forEach { category ->
                            FinanceTextField(
                                value = categoryAmounts.value[category.id].orEmpty(),
                                onValueChange = { text ->
                                    val sanitized = sanitizeAmountInput(text)
                                    categoryAmounts.value = categoryAmounts.value + (category.id to sanitized)
                                },
                                label = category.name,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                visualTransformation = AmountVisualTransformation,
                            )
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Crear plan",
                onClick = { onCreate(selectedTemplates, categoryAmounts.value) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            )
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemSheet(
    draft: FortnightItemDraft,
    state: FortnightUiState,
    isSaving: Boolean,
    onChange: ((FortnightItemDraft) -> FortnightItemDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    // El selector se abre en su propia hoja: el formulario permanece montado detrás y el
    // borrador (descripción, monto, nota...) vive en el ViewModel, así que no se pierde nada.
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    var goalPickerOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val selectedCategory = state.categories.firstOrNull { it.id == draft.categoryId }
    val selectedGoal = state.openSavingsGoals.firstOrNull { it.goal.id == draft.savingsGoalId }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (draft.isEditing) "Editar concepto" else "Nuevo concepto",
                style = MaterialTheme.typography.titleMedium,
            )
            FinanceTextField(
                value = draft.description,
                onValueChange = { text -> onChange { it.copy(description = text) } },
                label = "Descripción",
                singleLine = true,
            )
            FinanceTextField(
                value = draft.amountText,
                onValueChange = { text -> onChange { it.copy(amountText = sanitizeAmountInput(text)) } },
                label = "Monto planificado",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = AmountVisualTransformation,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.type == FortnightItemType.EXPENSE,
                    onClick = { onChange { it.copy(type = FortnightItemType.EXPENSE) } },
                    label = { Text("Gasto") },
                    leadingIcon = {
                        com.angel.mony.ui.iconography.MonyIcon(
                            com.angel.mony.ui.iconography.MonyIcon.Expense,
                            null,
                            Modifier.size(16.dp),
                        )
                    },
                )
                FilterChip(
                    selected = draft.type == FortnightItemType.SAVINGS,
                    onClick = { onChange { it.copy(type = FortnightItemType.SAVINGS) } },
                    label = { Text("Ahorro") },
                    leadingIcon = {
                        com.angel.mony.ui.iconography.MonyIcon(
                            com.angel.mony.ui.iconography.MonyIcon.Savings,
                            null,
                            Modifier.size(16.dp),
                        )
                    },
                )
            }

            FinanceSelectionField(
                label = "Categoría",
                value = selectedCategory?.name.orEmpty(),
                placeholder = "Selecciona una categoría",
                onClick = {
                    focusManager.clearFocus(force = true)
                    categoryPickerOpen = true
                },
            )

            if (draft.type == FortnightItemType.SAVINGS) {
                FinanceSelectionField(
                    label = "Meta de ahorro",
                    value = selectedGoal?.goal?.name.orEmpty(),
                    placeholder = "Selecciona la meta de ahorro",
                    onClick = {
                        focusManager.clearFocus(force = true)
                        goalPickerOpen = true
                    },
                )
            }

            FinanceTextField(
                value = draft.note,
                onValueChange = { text -> onChange { it.copy(note = text) } },
                label = "Nota (opcional)",
                singleLine = true,
            )

            PrimaryButton(
                text = if (draft.isEditing) "Guardar cambios" else "Agregar",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (categoryPickerOpen) {
        FinanceSelectionSheet(
            title = "Seleccionar categoría",
            options = state.categories.map { SelectionOption(it.id, it.name) },
            selectedId = draft.categoryId,
            onSelect = { categoryId ->
                onChange { it.copy(categoryId = categoryId) }
                categoryPickerOpen = false
            },
            onDismiss = { categoryPickerOpen = false },
            emptyMessage = "Todavía no tienes categorías activas.",
        )
    }

    if (goalPickerOpen) {
        FinanceSelectionSheet(
            title = "Seleccionar meta de ahorro",
            options = state.openSavingsGoals.map { SelectionOption(it.goal.id, it.goal.name) },
            selectedId = draft.savingsGoalId,
            onSelect = { goalId ->
                onChange { it.copy(savingsGoalId = goalId) }
                goalPickerOpen = false
            },
            onDismiss = { goalPickerOpen = false },
            emptyMessage = "Todavía no tienes metas de ahorro activas.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentDialog(
    draft: FortnightPaymentDraft,
    isSaving: Boolean,
    onChange: ((FortnightPaymentDraft) -> FortnightPaymentDraft) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isValid = (draft.amountInCents ?: 0L) > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar abono") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    draft.itemDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FinanceTextField(
                    value = draft.amountText,
                    onValueChange = { text -> onChange { it.copy(amountText = sanitizeAmountInput(text)) } },
                    label = "Monto del abono",
                    singleLine = true,
                    isError = draft.amountText.isNotBlank() && !isValid,
                    errorMessage = "Indica un monto de abono mayor que cero.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
                SecondaryButton(
                    text = "Fecha: ${draft.date.format(dayMonthFormatter)}",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "El abono se registrará como gasto en tu historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = isValid && !isSaving) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surface,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.date.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onChange { it.copy(date = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)) }
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}
