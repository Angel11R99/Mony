package com.angel.mony.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.core.showToast
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.GlobalSaveButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val editing by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val suggestedCategoryId by viewModel.suggestedCategoryId.collectAsStateWithLifecycle()
    val suggestedDate by viewModel.suggestedDate.collectAsStateWithLifecycle()
    val activePeriod by viewModel.activePeriod.collectAsStateWithLifecycle()
    val periodDateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-DO"))
    }
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var dateSuggestionApplied by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(editing?.id) {
        editing?.let {
            amount = java.math.BigDecimal.valueOf(it.amountInCents, 2).stripTrailingZeros().toPlainString()
            note = it.description.orEmpty()
            date = it.date.toString()
            categoryId = it.categoryId
        }
    }
    LaunchedEffect(suggestedCategoryId, categories) {
        if (!viewModel.isEditing && categoryId == null) {
            categoryId = suggestedCategoryId?.takeIf { suggested ->
                categories.any { it.id == suggested }
            }
        }
    }
    LaunchedEffect(suggestedDate) {
        if (!viewModel.isEditing && !dateSuggestionApplied && suggestedDate != null) {
            date = suggestedDate.toString()
            dateSuggestionApplied = true
        }
    }
    LaunchedEffect(error) {
        error?.let { message ->
            context.showToast(message)
            viewModel.consumeError()
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                ModuleTitle(
                    if (editing == null) {
                        if (viewModel.type == TransactionType.EXPENSE) "Registrar gasto" else "Registrar ingreso"
                    } else "Editar movimiento",
                )
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
            actions = {
                GlobalSaveButton(
                    onClick = {
                        viewModel.save(amount, categoryId, note, date, onBack)
                    },
                    enabled = !saving,
                )
                Spacer(Modifier.width(8.dp))
                GlobalSettingsButton(onClick = onSettings)
                Spacer(Modifier.width(14.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                FinanceTextField(
                    value = amount,
                    onValueChange = { amount = sanitizeAmountInput(it) },
                    label = "Monto en RD$",
                    placeholder = "0.00",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Categoría", style = MaterialTheme.typography.titleMedium)
                    CategorySearchSelect(
                        categories = categories,
                        selectedCategoryId = categoryId,
                        onCategoryChange = { categoryId = it },
                    )
                }
            }
            item { Text("Detalles", style = MaterialTheme.typography.titleMedium) }
            item {
                Box {
                    FinanceTextField(
                        value = date,
                        onValueChange = {},
                        label = "Fecha",
                        placeholder = "AAAA-MM-DD",
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Seleccionar fecha",
                            ) { showDatePicker = true },
                    )
                }
            }
            val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
            val outsidePeriod = !viewModel.isEditing && parsedDate != null &&
                (parsedDate.isBefore(activePeriod.start) || parsedDate.isAfter(activePeriod.endInclusive))
            if (outsidePeriod) {
                item {
                    Text(
                        "Esta fecha está fuera del periodo actual (${activePeriod.start.format(periodDateFormatter)} / ${activePeriod.endInclusive.format(periodDateFormatter)}). El movimiento se registrará en el periodo correspondiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                FinanceTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Nota",
                    placeholder = "Añade un detalle opcional",
                )
            }
        }
    }

    if (showDatePicker) {
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY).toString()
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySearchSelect(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    // Null while untouched so the field mirrors the current selection;
    // typing takes over the text until a new category is picked.
    var draftQuery by remember { mutableStateOf<String?>(null) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()
    val query = draftQuery ?: selectedName
    val matchingCategories = remember(categories, query) {
        searchCategories(categories, query)
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        FinanceTextField(
            value = query,
            onValueChange = { value ->
                draftQuery = value
                if (selectedCategoryId != null) onCategoryChange(null)
                expanded = true
            },
            label = "Buscar o seleccionar categoría",
            placeholder = "Nombre de la categoría",
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                draftQuery = ""
                                onCategoryChange(null)
                                expanded = true
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
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                draftQuery = null
            },
        ) {
            matchingCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    trailingIcon = if (category.id == selectedCategoryId) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                    onClick = {
                        draftQuery = null
                        onCategoryChange(category.id)
                        expanded = false
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
