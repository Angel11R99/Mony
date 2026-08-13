package com.example.personalfinancetracker.presentation.fixed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput

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
                                "Guarda aquí los movimientos que repites y agrégalos con la fecha del día.",
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
                    onAddToday = { viewModel.addToday(entry) },
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
    onAddToday: () -> Unit,
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(
                    text = if (entry.isActive) "Agregar hoy" else "Inactivo",
                    onClick = onAddToday,
                    modifier = Modifier.weight(1f),
                    enabled = entry.isActive,
                )
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

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
