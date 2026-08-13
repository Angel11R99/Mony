package com.example.personalfinancetracker.presentation.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.LocalDate

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
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
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
    LaunchedEffect(error) {
        error?.let { message ->
            context.showToast(message)
            viewModel.consumeError()
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    if (editing == null) {
                        if (viewModel.type == TransactionType.EXPENSE) "Registrar gasto" else "Registrar ingreso"
                    } else "Editar movimiento",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
            actions = {
                GlobalSettingsButton(onClick = onSettings)
                Spacer(Modifier.width(14.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                PrimaryButton(
                    text = if (saving) "Guardando…" else if (editing == null) "Guardar movimiento" else "Guardar cambios",
                    onClick = { viewModel.save(amount, categoryId, note, date, onBack) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
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
                    categories.chunked(2).forEach { rowCategories ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowCategories.forEach { category ->
                                CategoryOption(
                                    name = category.name,
                                    selected = category.id == categoryId,
                                    onClick = { categoryId = category.id },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowCategories.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
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

@Composable
private fun CategoryOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .heightIn(min = 50.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
