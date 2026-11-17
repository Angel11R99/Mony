package com.angel.mony.presentation.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.angel.mony.domain.model.Category
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val HISTORY_MILLIS_PER_DAY = 86_400_000L

@Composable
internal fun HistoryFilterButton(
    typeFilter: HistoryTypeFilter,
    cycleFilter: HistoryCycleFilter,
    categoryId: Long?,
    categories: Map<Long, Category>,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onClick: () -> Unit,
) {
    val categoryLabel = categoryId?.let { categories[it]?.name } ?: "Todas categorías"
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val dateLabel = when {
        startDate != null && endDate != null -> "${startDate.format(formatter)} – ${endDate.format(formatter)}"
        startDate != null -> "Desde ${startDate.format(formatter)}"
        endDate != null -> "Hasta ${endDate.format(formatter)}"
        else -> null
    }
    val subtitleParts = buildList {
        add(typeFilter.label)
        add(cycleFilter.label)
        add(categoryLabel)
        dateLabel?.let { add(it) }
    }
    val subtitle = subtitleParts.joinToString(" · ")
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Filter, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.Start) {
            Text("FILTROS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryFilterSheet(
    availableCategories: List<Category>,
    categoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    typeFilter: HistoryTypeFilter,
    onTypeChange: (HistoryTypeFilter) -> Unit,
    cycleFilter: HistoryCycleFilter,
    onCycleChange: (HistoryCycleFilter) -> Unit,
    cycleOptions: List<HistoryCycleFilter>,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySearch by remember(categoryId, availableCategories) {
        mutableStateOf(availableCategories.firstOrNull { it.id == categoryId }?.name.orEmpty())
    }
    val matchingCategories = remember(availableCategories, categorySearch, categoryId) {
        if (categoryId != null) availableCategories
        else searchCategories(availableCategories, categorySearch)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FILTROS", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Refina los movimientos mostrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Close, "Cerrar filtros")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("TIPO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryTypeFilter.entries.forEach { option ->
                    FilterChip(
                        selected = typeFilter == option,
                        onClick = { onTypeChange(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
            Text("CICLO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            HistoryCycleMenu(
                selected = cycleFilter,
                options = cycleOptions,
                onSelect = onCycleChange,
            )
            Text("CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = categorySearch,
                    onValueChange = { value ->
                        categorySearch = value
                        onCategoryChange(null)
                        categoryExpanded = true
                    },
                    label = { Text("Buscar categoría") },
                    placeholder = { Text("Todas las categorías") },
                    leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (categorySearch.isNotBlank() || categoryId != null) {
                                IconButton(
                                    onClick = {
                                        categorySearch = ""
                                        onCategoryChange(null)
                                        categoryExpanded = false
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
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    if (categorySearch.isBlank()) {
                        DropdownMenuItem(
                            text = { Text("Todas las categorías") },
                            onClick = {
                                categorySearch = ""
                                onCategoryChange(null)
                                categoryExpanded = false
                            },
                        )
                    }
                    matchingCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categorySearch = category.name
                                onCategoryChange(category.id)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryDateField("Desde", startDate, onStartDateChange, Modifier.weight(1f))
                HistoryDateField("Hasta", endDate, onEndDateChange, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpiar", onClear, Modifier.weight(1f))
                PrimaryButton("Aplicar", onApply, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun HistorySortMenu(sort: HistorySort, onSortChange: (HistorySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Sort, null, modifier = Modifier.size(18.dp))
            Text(sort.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HistorySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (sort == option) {
                        { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Check, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE) }
                    } else null,
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value?.format(formatter) ?: "Cualquier día", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Calendar, null, tint = MaterialTheme.colorScheme.primary, role = com.angel.mony.ui.iconography.MonyIconRole.STATE)
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).toEpochDay() * HISTORY_MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(pickerState.selectedDateMillis?.let { LocalDate.ofEpochDay(it / HISTORY_MILLIS_PER_DAY) })
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                Row {
                    if (value != null) TextButton(onClick = {
                        onValueChange(null)
                        showPicker = false
                    }) { Text("Quitar") }
                    TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
                }
            },
        ) { DatePicker(pickerState) }
    }
}


