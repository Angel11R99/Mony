package com.angel.mony.presentation.fortnight

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.FortnightTemplate
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceSelectionField
import com.angel.mony.presentation.components.FinanceSelectionSheet
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SelectionOption
import com.angel.mony.presentation.components.sanitizeAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortnightTemplatesScreen(
    onBack: () -> Unit,
    viewModel: FortnightTemplatesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { context.showToast(it); viewModel.consumeMessage() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    ModuleTitle(if (state.isMonthly) "Plantillas del mes" else "Plantillas de quincena")
                },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver",
                        onClick = onBack,
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalOutlinedIconButton(Icons.Outlined.Add, "Nueva plantilla", viewModel::startCreate)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.hasError -> Text(
                "No se pudieron cargar las plantillas.",
                Modifier.padding(padding).padding(18.dp),
                color = MaterialTheme.colorScheme.error,
            )
            state.templates.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Todavía no tienes plantillas.",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (state.isMonthly) {
                        "Crea una plantilla con los conceptos que se repiten cada mes " +
                            "para planificar más rápido."
                    } else {
                        "Crea una plantilla con los conceptos que se repiten cada quincena " +
                            "para planificar más rápido."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton("Crear plantilla", viewModel::startCreate, Modifier.fillMaxWidth())
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        isMonthly = state.isMonthly,
                        enabled = !isSaving,
                        onEdit = { viewModel.startEdit(template) },
                        onDelete = { viewModel.requestDelete(template) },
                        onActiveChange = { viewModel.setActive(template, it) },
                    )
                }
            }
        }
    }

    draft?.let {
        TemplateSheet(
            draft = it,
            categoryNames = state.categories.map { category -> category.id to category.name },
            isMonthly = state.isMonthly,
            isSaving = isSaving,
            onChange = viewModel::updateDraft,
            onSave = viewModel::save,
            onDismiss = viewModel::cancelDraft,
        )
    }

    pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Eliminar plantilla") },
            text = { Text("¿Eliminar \"${it.description}\"? Los planes ya creados no cambiarán.") },
            confirmButton = { TextButton(viewModel::confirmDelete, enabled = !isSaving) { Text("Eliminar") } },
            dismissButton = { TextButton(viewModel::cancelDelete) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TemplateCard(
    template: FortnightTemplate,
    isMonthly: Boolean,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        template.description,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (isMonthly) {
                            "Monto: " + (template.firstFortnightAmountInCents?.let(MoneyFormatter::format) ?: "—")
                        } else {
                            buildString {
                                append("1ra: ")
                                append(template.firstFortnightAmountInCents?.let(MoneyFormatter::format) ?: "—")
                                append("   2da: ")
                                append(template.secondFortnightAmountInCents?.let(MoneyFormatter::format) ?: "—")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = template.isActive,
                    onCheckedChange = onActiveChange,
                    enabled = enabled,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.Outlined.Edit, "Editar plantilla")
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.Outlined.Delete, "Eliminar plantilla")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (template.isActive) "Activa" else "Pausada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSheet(
    draft: FortnightTemplateDraft,
    categoryNames: List<Pair<Long, String>>,
    isMonthly: Boolean,
    isSaving: Boolean,
    onChange: ((FortnightTemplateDraft) -> FortnightTemplateDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val selectedName = categoryNames.firstOrNull { it.first == draft.categoryId }?.second.orEmpty()

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
                if (draft.isEditing) "Editar plantilla" else "Nueva plantilla",
                style = MaterialTheme.typography.titleMedium,
            )
            FinanceTextField(
                value = draft.description,
                onValueChange = { text -> onChange { it.copy(description = text) } },
                label = "Descripción",
                singleLine = true,
            )
            FinanceSelectionField(
                label = "Categoría",
                value = selectedName,
                placeholder = "Selecciona una categoría",
                onClick = {
                    focusManager.clearFocus(force = true)
                    categoryPickerOpen = true
                },
            )
            FinanceTextField(
                value = draft.firstAmountText,
                onValueChange = { text -> onChange { it.copy(firstAmountText = sanitizeAmountInput(text)) } },
                label = if (isMonthly) "Monto mensual (opcional)" else "Monto 1ra quincena (opcional)",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = AmountVisualTransformation,
            )
            if (!isMonthly) {
                FinanceTextField(
                    value = draft.secondAmountText,
                    onValueChange = { text -> onChange { it.copy(secondAmountText = sanitizeAmountInput(text)) } },
                    label = "Monto 2da quincena (opcional)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
            }
            FinanceTextField(
                value = draft.note,
                onValueChange = { text -> onChange { it.copy(note = text) } },
                label = "Nota (opcional)",
                singleLine = true,
            )
            PrimaryButton(
                text = "Guardar",
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
            options = categoryNames.map { (id, name) -> SelectionOption(id, name) },
            selectedId = draft.categoryId,
            onSelect = { categoryId ->
                onChange { it.copy(categoryId = categoryId) }
                categoryPickerOpen = false
            },
            onDismiss = { categoryPickerOpen = false },
            emptyMessage = "Todavía no tienes categorías activas.",
        )
    }
}
