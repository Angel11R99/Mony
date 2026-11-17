package com.angel.mony.presentation.pending

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.PendingType
import com.angel.mony.domain.model.isPendingReminderInFuture
import com.angel.mony.domain.model.isPendingDateValid
import com.angel.mony.domain.model.label
import com.angel.mony.domain.model.toTransactionType
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)

@Composable
internal fun PendingEntryDetailsDialog(
    entry: PendingEntry,
    category: Category?,
    formatter: DateTimeFormatter,
    onDismiss: () -> Unit,
) {
    val amountColor = pendingAmountColor(entry.type)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = {
            com.angel.mony.ui.iconography.MonyIcon(
                com.angel.mony.ui.iconography.MonyIcon.Pending,
                contentDescription = null,
                tint = amountColor,
                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
            )
        },
        title = { Text("Detalle del recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    MoneyFormatter.format(entry.amountInCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = amountColor,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FinanceDetailRow("Tipo", entry.type.label(), valueColor = amountColor)
                FinanceDetailRow("Categoría", category?.name ?: "Sin categoría")
                FinanceDetailRow(
                    "Fecha",
                    entry.date.format(formatter) + (entry.reminderTime?.takeUnless { entry.isDone }
                        ?.let { " · Alerta ${it.format(reminderTimeFormatter)}" } ?: ""),
                )
                FinanceDetailRow("Estado", if (entry.isDone) "Hecho" else "Pendiente")
                entry.doneAt?.let {
                    FinanceDetailRow(
                        "Registrado",
                        it.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter),
                    )
                }
                entry.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "COMENTARIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { PrimaryButton("Cerrar", onDismiss) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PendingEntryDialog(
    entry: PendingEntry?,
    initialType: PendingType,
    initialCategoryId: Long? = null,
    initialDate: LocalDate? = null,
    initialReminderTime: LocalTime? = null,
    categories: List<Category>,
    isSaving: Boolean,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (PendingType, String, String, Long?, String, LocalDate, LocalTime?) -> Unit,
) {
    var type by remember(entry) { mutableStateOf(entry?.type ?: initialType) }
    var description by remember(entry) { mutableStateOf(entry?.description.orEmpty()) }
    var amount by remember(entry) {
        mutableStateOf(entry?.amountInCents?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var categoryId by remember(entry) { mutableStateOf(entry?.categoryId ?: initialCategoryId) }
    var comment by remember(entry) { mutableStateOf(entry?.comment.orEmpty()) }
    var date by remember(entry) { mutableStateOf(entry?.date ?: initialDate ?: LocalDate.now()) }
    var reminderTime by remember(entry) { mutableStateOf(entry?.reminderTime ?: initialReminderTime) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(entry, categories) {
        mutableStateOf(categories.firstOrNull { it.id == (entry?.categoryId ?: initialCategoryId) }?.name.orEmpty())
    }
    val formState = remember { FormState() }
    val availableCategories = remember(categories, type) {
        categories.filter { it.type == type.toTransactionType() && it.isActive }.sortedBy(Category::name)
    }
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
        title = { Text(if (entry == null) "Nuevo recordatorio" else "Editar recordatorio") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PendingTypeChip(PendingType.PAYMENT, type, { type = it }, Modifier.weight(1f))
                        PendingTypeChip(PendingType.COLLECTION, type, { type = it }, Modifier.weight(1f))
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
                item {
                    PendingDateField(
                        label = "Fecha del pago o cobro",
                        value = date,
                        minimumDate = LocalDate.now(),
                        onValueChange = { date = it; formState.clearError("date") },
                        isError = formState.hasError("date"),
                        errorMessage = formState["date"],
                    )
                }
                item {
                    PendingReminderField(
                        date = date,
                        value = reminderTime,
                        notificationsGranted = notificationsGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onValueChange = { reminderTime = it },
                    )
                }
                item { FinanceTextField(comment, { comment = it }, "Comentario", placeholder = "Opcional") }
            }
        },
        confirmButton = {
            PrimaryButton(
                if (isSaving) "Guardando…" else "Guardar",
                onClick = {
                    formState.clearAll()
                    if (description.isBlank()) formState.setError("description", "Escribe una descripción")
                    val cents = MoneyFormatter.parseToCents(amount)
                    if (cents == null || cents <= 0) formState.setError("amount", "Ingresa un monto válido")
                    if (categoryId == null) formState.setError("category", "Selecciona una categoría")
                    if (!formState.isValid()) return@PrimaryButton
                    onSave(type, description, amount, categoryId, comment, date, reminderTime)
                },
                enabled = !isSaving,
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PendingReminderField(
    date: LocalDate,
    value: LocalTime?,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onValueChange: (LocalTime?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var errorMessage by remember(date, value) { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) {
                if (!isPendingDateValid(date)) {
                    errorMessage = "Selecciona hoy o una fecha futura para activar la alerta"
                    return@clickable
                }
                if (!notificationsGranted) onRequestNotificationPermission()
                showPicker = true
            },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Alerta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(value?.format(reminderTimeFormatter) ?: "Sin alerta")
                if (value != null && !notificationsGranted) {
                    Text(
                        "Se necesita permiso de notificaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (value != null) {
                IconButton(onClick = { onValueChange(null) }) {
                    com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.AlertsDisabled, "Quitar alerta")
                }
            } else {
                com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.AlertsEnabled, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = value?.hour ?: 8,
            initialMinute = value?.minute ?: 0,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Hora de la alerta") },
            text = { TimePicker(pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime = LocalTime.of(pickerState.hour, pickerState.minute)
                    if (isPendingReminderInFuture(date, selectedTime)) {
                        errorMessage = null
                        onValueChange(selectedTime)
                        showPicker = false
                    } else {
                        errorMessage = "Elige una hora que todavía no haya pasado"
                        showPicker = false
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PendingDateField(
    label: String,
    value: LocalDate,
    minimumDate: LocalDate? = null,
    onValueChange: (LocalDate) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val borderColor by animateColorAsState(
        if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        animationSpec = tween(),
        label = "dateBorder",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                Text(value.format(formatter))
                if (isError && errorMessage != null) {
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Calendar, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        }
    }
    if (showPicker) {
        val selectableDates = remember(minimumDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minimumDate == null ||
                        LocalDate.ofEpochDay(utcTimeMillis / MILLIS_PER_DAY) >= minimumDate
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = selectableDates,
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

private const val MILLIS_PER_DAY = 86_400_000L

internal val reminderTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))

internal val searchDateLongFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))

internal val searchDateNumericFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun filterPendingEntries(
    entries: List<PendingEntry>,
    type: PendingType,
    range: DateRange?,
    query: String,
    categories: Map<Long, Category>,
    status: PendingStatusFilter = PendingStatusFilter.ALL,
): List<PendingEntry> {
    val normalizedQuery = query.trim()
    val digitQuery = normalizedQuery.filter(Char::isDigit)
    return entries
        .filter { it.type == type }
        .filter { range == null || it.date in range.start..range.endInclusive }
        .filter {
            when (status) {
                PendingStatusFilter.ALL -> true
                PendingStatusFilter.PENDING -> !it.isDone
                PendingStatusFilter.DONE -> it.isDone
            }
        }
        .filter {
            normalizedQuery.isEmpty() || it.matchesPendingQuery(normalizedQuery, digitQuery, categories)
        }
        .sortedWith(compareBy<PendingEntry> { it.isDone }.thenBy(PendingEntry::date))
}


