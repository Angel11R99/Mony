package com.example.personalfinancetracker.presentation.fixed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.FixedDateMode
import com.example.personalfinancetracker.domain.model.FixedScheduleMode
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedEntriesScreen(viewModel: FixedEntriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editorEntry by remember { mutableStateOf<FixedEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FixedEntry?>(null) }
    var configEntry by remember { mutableStateOf<FixedEntry?>(null) }
    val visibleEntries = remember(state.entries, selectedType) {
        state.entries.filter { it.type == selectedType }
    }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FIJOS", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {
                        editorEntry = null
                        showEditor = true
                    }) { Icon(Icons.Outlined.Add, "Nueva plantilla") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(TransactionType.EXPENSE, selectedType, { selectedType = it }, Modifier.weight(1f))
                    TypeChip(TransactionType.INCOME, selectedType, { selectedType = it }, Modifier.weight(1f))
                }
            }
            item {
                Text(
                    if (selectedType == TransactionType.EXPENSE) "GASTOS RECURRENTES" else "INGRESOS RECURRENTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (visibleEntries.isEmpty()) {
                item {
                    FinanceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Todavía no tienes plantillas", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Guarda aquí los movimientos que repites y agrégalos con la fecha que elijas.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton("Crear la primera", {
                                editorEntry = null
                                showEditor = true
                            }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            items(visibleEntries, key = FixedEntry::id) { entry ->
                FixedEntryCard(
                    entry = entry,
                    category = state.categories[entry.categoryId],
                    onToggle = { viewModel.toggle(entry) },
                    onAddNow = { viewModel.addNow(entry) },
                    onConfigure = { configEntry = entry },
                    onEdit = {
                        editorEntry = entry
                        showEditor = true
                    },
                    onDelete = { pendingDelete = entry },
                )
            }
        }
    }

    if (showEditor) {
        FixedEntryDialog(
            entry = editorEntry,
            initialType = editorEntry?.type ?: selectedType,
            categories = state.categories.values.toList(),
            onDismiss = { showEditor = false },
            onSave = { type, description, amount, categoryId, comment, active ->
                viewModel.save(editorEntry, type, description, amount, categoryId, comment, active) {
                    selectedType = type
                    showEditor = false
                }
            },
        )
    }

    configEntry?.let { entry ->
        FixedEntryConfigurationDialog(
            entry = entry,
            onDismiss = { configEntry = null },
            onSave = { dateMode, manualDate, scheduleMode, hour, scheduleDate ->
                viewModel.configure(
                    entry = entry,
                    manualDateMode = dateMode,
                    manualSpecificDate = manualDate,
                    scheduleMode = scheduleMode,
                    scheduleHour = hour,
                    scheduleSpecificDate = scheduleDate,
                ) { configEntry = null }
            },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar plantilla?") },
            text = { Text("${entry.description} dejará de aparecer en Fijos. Los movimientos ya agregados no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry)
                    pendingDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun TypeChip(
    type: TransactionType,
    selectedType: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selectedType == type,
        onClick = { onSelect(type) },
        label = { Text(if (type == TransactionType.EXPENSE) "Gastos" else "Ingresos") },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selectedType == type,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun FixedEntryCard(
    entry: FixedEntry,
    category: Category?,
    onToggle: () -> Unit,
    onAddNow: () -> Unit,
    onConfigure: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.description, style = MaterialTheme.typography.titleLarge)
                    Text(
                        category?.name ?: "Sin categoría",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Switch(checked = entry.isActive, onCheckedChange = { onToggle() })
            }
            Text(
                MoneyFormatter.format(entry.amountInCents),
                style = MaterialTheme.typography.headlineMedium,
                color = if (entry.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
            entry.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FixedEntryTimingStatus(entry)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(
                    text = when {
                        entry.isActive -> "Agregar ahora"
                        entry.lastAddedAt != null -> "Agregado"
                        else -> "Inactivo"
                    },
                    onClick = onAddNow,
                    modifier = Modifier.weight(1f),
                    enabled = entry.isActive,
                )
                IconButton(onClick = onConfigure) { Icon(Icons.Outlined.Settings, "Configurar fechas") }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FixedEntryTimingStatus(entry: FixedEntry) {
    val zone = remember { ZoneId.systemDefault() }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.forLanguageTag("es-DO"))
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "AL AGREGAR · " + if (entry.manualDateMode == FixedDateMode.SPECIFIC_DATE) {
                entry.manualSpecificDate?.format(dateFormatter) ?: "FECHA PENDIENTE"
            } else {
                manualDateModeLabel(entry.manualDateMode).uppercase()
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        entry.nextRunAt?.let {
            Text(
                "PRÓXIMO · ${it.atZone(zone).format(dateTimeFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        entry.lastAddedAt?.let {
            Text(
                "ÚLTIMO AGREGADO · ${it.atZone(zone).format(dateTimeFormatter)}" +
                    (entry.lastAddedDate?.let { date -> " · FECHA ${date.format(dateFormatter)}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FixedEntryConfigurationDialog(
    entry: FixedEntry,
    onDismiss: () -> Unit,
    onSave: (FixedDateMode, LocalDate?, FixedScheduleMode, Int, LocalDate?) -> Unit,
) {
    var dateMode by remember(entry) { mutableStateOf(entry.manualDateMode) }
    var manualDate by remember(entry) { mutableStateOf(entry.manualSpecificDate) }
    var scheduleMode by remember(entry) { mutableStateOf(entry.scheduleMode) }
    var scheduleDate by remember(entry) { mutableStateOf(entry.scheduleSpecificDate) }
    var scheduleHour by remember(entry) { mutableStateOf(entry.scheduleHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar fechas") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "AL AGREGAR MANUALMENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                item {
                    Text(
                        "El movimiento se guardará usando esta fecha.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FixedDateMode.entries.forEach { option ->
                            ConfigChip(
                                selected = dateMode == option,
                                text = manualDateModeLabel(option),
                                onClick = { dateMode = option },
                            )
                        }
                    }
                }
                if (dateMode == FixedDateMode.SPECIFIC_DATE) {
                    item {
                        FixedDateField(
                            label = "Fecha del movimiento",
                            value = manualDate,
                            onValueChange = { manualDate = it },
                        )
                    }
                    item {
                        Text(
                            "Después de agregarla una vez, la plantilla se desactivará.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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
                onSave(dateMode, manualDate, scheduleMode, scheduleHour, scheduleDate)
            })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun ConfigChip(selected: Boolean, text: String, onClick: () -> Unit) {
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
private fun FixedDateField(label: String, value: LocalDate?, onValueChange: (LocalDate) -> Unit) {
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
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
private fun FixedHourField(hour: Int, onHourChange: (Int) -> Unit) {
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
            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

private fun manualDateModeLabel(mode: FixedDateMode): String = when (mode) {
    FixedDateMode.TODAY -> "Hoy"
    FixedDateMode.PREVIOUS_FORTNIGHT -> "Quincena anterior"
    FixedDateMode.PREVIOUS_MONTH -> "Mes anterior"
    FixedDateMode.SPECIFIC_DATE -> "Fecha específica"
}

private fun scheduleModeLabel(mode: FixedScheduleMode): String = when (mode) {
    FixedScheduleMode.MANUAL -> "Sin programación"
    FixedScheduleMode.AFTER_FORTNIGHT -> "Después de cada quincena"
    FixedScheduleMode.AFTER_MONTH -> "Después de cada mes"
    FixedScheduleMode.SPECIFIC_DATE_TIME -> "Fecha específica"
}

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedEntryDialog(
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
                item { FinanceTextField(description, { description = it }, "Descripción", singleLine = true) }
                item {
                    FinanceTextField(
                        amount,
                        { amount = sanitizeAmountInput(it) },
                        "Monto (RD$)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = AmountVisualTransformation,
                    )
                }
                item {
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
                            },
                            label = { Text("Buscar categoría") },
                            placeholder = { Text("Escribe o selecciona") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
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
                                            Icon(
                                                Icons.Outlined.Close,
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
            PrimaryButton("Guardar", { onSave(type, description, amount, categoryId, comment, active) })
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}
