package com.angel.mony.presentation.fixed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.FixedScheduleMode
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.previousFortnightEnd
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)

@Composable
internal fun FixedEntryManualActionsDialog(
    entry: FixedEntry,
    onDismiss: () -> Unit,
    onAdd: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar ${entry.description}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Elige la fecha. Al tocar una opción, el movimiento se agrega de inmediato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = "Hoy",
                    onClick = { onAdd(today) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Ciclo anterior",
                    onClick = { onAdd(previousFortnightEnd(today)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Mes anterior",
                    onClick = { onAdd(YearMonth.from(today).minusMonths(1).atEndOfMonth()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Fecha específica",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = today.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onAdd(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    showDatePicker = false
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FixedEntryConfigurationDialog(
    entry: FixedEntry,
    onDismiss: () -> Unit,
    onSave: (FixedScheduleMode, Int, LocalDate?) -> Unit,
) {
    var scheduleMode by remember(entry) { mutableStateOf(entry.scheduleMode) }
    var scheduleDate by remember(entry) { mutableStateOf(entry.scheduleSpecificDate) }
    var scheduleHour by remember(entry) { mutableStateOf(entry.scheduleHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programación automática") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "PROGRAMACIÓN AUTOMÁTICA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FixedScheduleMode.entries.forEach { option ->
                            ConfigChip(
                                selected = scheduleMode == option,
                                text = scheduleModeLabel(option),
                                onClick = { scheduleMode = option },
                            )
                        }
                    }
                }
                if (scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME) {
                    item {
                        FixedDateField(
                            label = "Fecha programada",
                            value = scheduleDate,
                            onValueChange = { scheduleDate = it },
                        )
                    }
                }
                if (scheduleMode != FixedScheduleMode.MANUAL) {
                    item {
                        FixedHourField(
                            hour = scheduleHour,
                            onHourChange = { scheduleHour = it },
                        )
                    }
                    item {
                        Text(
                            if (scheduleMode == FixedScheduleMode.SPECIFIC_DATE_TIME) {
                                "Al registrarse se desactivará automáticamente."
                            } else {
                                "Se repetirá y mostrará la próxima ejecución en la tarjeta."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton("Guardar", {
                onSave(scheduleMode, scheduleHour, scheduleDate)
            })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
internal fun ConfigChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FixedDateField(label: String, value: LocalDate?, onValueChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(value?.format(formatter) ?: "Seleccionar fecha")
            }
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Calendar, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FixedHourField(hour: Int, onHourChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val display = remember(hour) {
        LocalDate.now().atTime(hour.coerceIn(0, 23), 0)
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO")))
    }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hora aproximada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(display)
            }
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Time, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        }
    }
    if (showPicker) {
        val timeState = rememberTimePickerState(initialHour = hour.coerceIn(0, 23), initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Elegir hora") },
            text = { TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onHourChange(timeState.hour)
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

internal fun scheduleModeLabel(mode: FixedScheduleMode): String = when (mode) {
    FixedScheduleMode.MANUAL -> "Sin programación"
    FixedScheduleMode.AFTER_FORTNIGHT -> "Después de cada ciclo"
    FixedScheduleMode.AFTER_MONTH -> "Después de cada mes"
    FixedScheduleMode.SPECIFIC_DATE_TIME -> "Fecha específica"
}

private const val MILLIS_PER_DAY = 86_400_000L

internal val timingDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.forLanguageTag("es-DO"))

internal val timingDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))

internal val scheduleHourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))

private val searchDateNumericFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun filterFixedEntries(
    entries: List<FixedEntry>,
    type: TransactionType,
    query: String,
    categories: Map<Long, Category>,
): List<FixedEntry> {
    val normalizedQuery = query.trim()
    val digitQuery = normalizedQuery.filter(Char::isDigit)
    return entries
        .filter { it.type == type }
        .filter {
            normalizedQuery.isEmpty() || it.matchesFixedQuery(normalizedQuery, digitQuery, categories)
        }
}

internal fun FixedEntry.matchesFixedQuery(
    query: String,
    digitQuery: String,
    categories: Map<Long, Category>,
): Boolean {
    val haystack = listOfNotNull(
        description,
        comment,
        categories[categoryId]?.name,
        if (type == TransactionType.EXPENSE) "Gastos" else "Ingresos",
        MoneyFormatter.format(amountInCents),
        scheduleModeLabel(scheduleMode),
        scheduleSpecificDate?.format(searchDateNumericFormatter),
        lastAddedDate?.format(searchDateNumericFormatter),
    ).joinToString(" ")
    return haystack.contains(query, ignoreCase = true) ||
        (digitQuery.isNotEmpty() && amountInCents.toString().contains(digitQuery))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FixedEntryDialog(
    entry: FixedEntry?,
    initialType: TransactionType,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String, String, Long?, String, Boolean) -> Unit,
) {
    var type by remember(entry) { mutableStateOf(entry?.type ?: initialType) }
    var description by remember(entry) { mutableStateOf(entry?.description.orEmpty()) }
    var amount by remember(entry) {
        mutableStateOf(entry?.amountInCents?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var categoryId by remember(entry) { mutableStateOf(entry?.categoryId) }
    var comment by remember(entry) { mutableStateOf(entry?.comment.orEmpty()) }
    var active by remember(entry) { mutableStateOf(entry?.isActive ?: true) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(entry, categories) {
        mutableStateOf(categories.firstOrNull { it.id == entry?.categoryId }?.name.orEmpty())
    }
    val formState = remember { FormState() }
    val availableCategories = categories.filter { it.type == type && it.isActive }.sortedBy(Category::name)
    val matchingCategories = remember(availableCategories, categorySearch, categoryId) {
        if (categoryId != null) availableCategories
        else availableCategories.filter { it.name.contains(categorySearch.trim(), ignoreCase = true) }
    }

    LaunchedEffect(type) {
        if (availableCategories.none { it.id == categoryId }) {
            categoryId = null
            categorySearch = ""
        }
        categoryExpanded = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Nueva plantilla" else "Editar plantilla") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeChip(TransactionType.EXPENSE, type, { type = it }, Modifier.weight(1f))
                        TypeChip(TransactionType.INCOME, type, { type = it }, Modifier.weight(1f))
                    }
                }
                item {
                    FinanceTextField(
                        description,
                        { description = it; formState.clearError("description") },
                        "Descripción",
                        singleLine = true,
                        isError = formState.hasError("description"),
                        errorMessage = formState["description"],
                    )
                }
                item {
                    FinanceTextField(
                        amount,
                        { amount = sanitizeAmountInput(it); formState.clearError("amount") },
                        "Monto (RD$)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = AmountVisualTransformation,
                        isError = formState.hasError("amount"),
                        errorMessage = formState["amount"],
                    )
                }
                item {
                    val categoryError = formState.hasError("category")
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = categorySearch,
                            onValueChange = { value ->
                                categorySearch = value
                                categoryId = null
                                categoryExpanded = true
                                formState.clearError("category")
                            },
                            label = { Text("Buscar categoría") },
                            placeholder = { Text("Escribe o selecciona") },
                            leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Search, null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (categorySearch.isNotBlank() || categoryId != null) {
                                        IconButton(
                                            onClick = {
                                                categorySearch = ""
                                                categoryId = null
                                                categoryExpanded = true
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            com.angel.mony.ui.iconography.MonyIcon(
                                                com.angel.mony.ui.iconography.MonyIcon.Close,
                                                contentDescription = "Limpiar categoría",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                                }
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            singleLine = true,
                            isError = categoryError,
                            supportingText = if (categoryError) {
                                { Text(formState["category"] ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            matchingCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        categorySearch = category.name
                                        categoryId = category.id
                                        categoryExpanded = false
                                    },
                                )
                            }
                            if (matchingCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No se encontraron categorías") },
                                    onClick = {},
                                    enabled = false,
                                )
                            }
                        }
                    }
                }
                item { FinanceTextField(comment, { comment = it }, "Comentario", placeholder = "Opcional") }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Plantilla activa", fontWeight = FontWeight.SemiBold)
                            Text("Permite agregarla al historial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton("Guardar", onClick = {
                formState.clearAll()
                if (description.isBlank()) formState.setError("description", "Escribe una descripción")
                val cents = MoneyFormatter.parseToCents(amount)
                if (cents == null || cents <= 0) formState.setError("amount", "Ingresa un monto válido")
                if (categoryId == null) formState.setError("category", "Selecciona una categoría")
                if (!formState.isValid()) return@PrimaryButton
                onSave(type, description, amount, categoryId, comment, active)
            })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

