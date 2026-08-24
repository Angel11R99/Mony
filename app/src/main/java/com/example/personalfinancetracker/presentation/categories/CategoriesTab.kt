package com.example.personalfinancetracker.presentation.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinancetracker.core.showToast
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.SecondaryButton

@Composable
fun CategoriesTab(viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "CATEGORÍAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "Organiza las categorías que usas al registrar movimientos. Las inactivas dejan de aparecer en los selectores, pero conservan su historial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SecondaryButton(
                text = "Nueva categoría",
                onClick = {
                    editingCategory = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { GroupHeader("INGRESOS") }
        item {
            CategoryGroupCard(
                categories = state.categories.filter { it.type == TransactionType.INCOME },
                usedIds = state.usedCategoryIds,
                onToggle = viewModel::toggleActive,
                onEdit = { category ->
                    editingCategory = category
                    showEditor = true
                },
                onDelete = viewModel::requestDelete,
            )
        }
        item { GroupHeader("GASTOS") }
        item {
            CategoryGroupCard(
                categories = state.categories.filter { it.type == TransactionType.EXPENSE },
                usedIds = state.usedCategoryIds,
                onToggle = viewModel::toggleActive,
                onEdit = { category ->
                    editingCategory = category
                    showEditor = true
                },
                onDelete = viewModel::requestDelete,
            )
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            category = editingCategory,
            initialType = TransactionType.EXPENSE,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { type, name ->
                val category = editingCategory
                if (category == null) {
                    viewModel.create(name, type) { showEditor = false }
                } else {
                    viewModel.rename(category, name) { showEditor = false }
                }
            },
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("¿Eliminar categoría?") },
            text = { Text("${category.name} se eliminará definitivamente. No tiene movimientos asociados.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun CategoryGroupCard(
    categories: List<Category>,
    usedIds: Set<Long>,
    onToggle: (Category) -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    val sorted = remember(categories) {
        categories.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            sorted.forEachIndexed { index, category ->
                CategoryRow(
                    category = category,
                    isInUse = category.id in usedIds,
                    onToggle = { onToggle(category) },
                    onEdit = { onEdit(category) },
                    onDelete = { onDelete(category) },
                )
                if (index < sorted.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isInUse: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                category.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (category.isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    !category.isActive -> "Inactiva"
                    isInUse -> "En uso"
                    else -> "Sin usar"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Edit, "Editar ${category.name}", modifier = Modifier.size(20.dp))
        }
        if (!isInUse) {
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Eliminar ${category.name}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Switch(checked = category.isActive, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun CategoryEditorDialog(
    category: Category?,
    initialType: TransactionType,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String) -> Unit,
) {
    var type by remember(category) { mutableStateOf(category?.type ?: initialType) }
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (category == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (category == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorTypeChip(TransactionType.EXPENSE, type, { type = it }, Modifier.weight(1f))
                        EditorTypeChip(TransactionType.INCOME, type, { type = it }, Modifier.weight(1f))
                    }
                }
                FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Guardando…" else "Guardar",
                onClick = { onSave(type, name) },
                enabled = !isSaving && name.isNotBlank() &&
                    (category == null || name.trim() != category.name),
            )
        },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun EditorTypeChip(
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
