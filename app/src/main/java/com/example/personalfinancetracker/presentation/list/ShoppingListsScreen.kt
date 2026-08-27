package com.example.personalfinancetracker.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListOverview
import com.example.personalfinancetracker.domain.model.ShoppingListStatus
import com.example.personalfinancetracker.presentation.components.AmountVisualTransformation
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.FinanceTextField
import com.example.personalfinancetracker.presentation.components.GlobalOutlinedIconButton
import com.example.personalfinancetracker.presentation.components.GlobalSettingsButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.presentation.components.PrimaryButton
import com.example.personalfinancetracker.presentation.components.sanitizeAmountInput
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: ShoppingListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { context.showToast(it); viewModel.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Lista") },
                actions = {
                    GlobalOutlinedIconButton(Icons.Outlined.Add, "Nueva lista", { showCreate = true })
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onSettings)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(); Text("Cargando listas…", Modifier.padding(top = 12.dp)) }
            state.hasError -> EmptyListsCard(
                "No se pudieron cargar las listas.",
                Modifier.padding(padding).padding(18.dp),
            )
            state.lists.isEmpty() -> EmptyListsCard(
                "Todavía no tienes listas de compra.",
                Modifier.padding(padding).padding(18.dp),
                onCreate = { showCreate = true },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.lists, key = { it.list.id }) { overview ->
                    val list = overview.list
                    ShoppingListCard(
                        overview = overview,
                        onOpen = { onOpen(list.id) },
                        onDuplicate = { viewModel.duplicate(list, onOpen) },
                        onDelete = { viewModel.requestDelete(list) },
                        enabled = !isSaving,
                    )
                }
            }
        }
    }

    if (showCreate) CreateListDialog(
        isSaving = isSaving,
        onDismiss = { if (!isSaving) showCreate = false },
        onCreate = { name, budget -> viewModel.create(name, budget) { showCreate = false; onOpen(it) } },
    )
    pendingDelete?.let { list ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Eliminar lista") },
            text = { Text("¿Eliminar “${list.name}” y todos sus productos y ajustes?") },
            confirmButton = { TextButton(viewModel::confirmDelete, enabled = !isSaving) { Text("Eliminar") } },
            dismissButton = { TextButton(viewModel::cancelDelete) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EmptyListsCard(text: String, modifier: Modifier, onCreate: (() -> Unit)? = null) {
    FinanceCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (onCreate != null) {
                Text("Organiza productos, presupuesto y el gasto final desde un solo lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton("Crear la primera lista", onCreate, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShoppingListCard(
    overview: ShoppingListOverview,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    val list = overview.list
    FinanceCard(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onOpen)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(list.status.spanishLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onDuplicate, enabled = enabled) { Icon(Icons.Outlined.ContentCopy, "Duplicar lista") }
                if (list.status != ShoppingListStatus.COMPLETED) {
                    IconButton(onClick = onDelete, enabled = enabled) { Icon(Icons.Outlined.Delete, "Eliminar lista") }
                }
            }
            val date = list.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-DO")))
            Text("Actualizada $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${overview.itemCount} producto${if (overview.itemCount == 1) "" else "s"} · ${MoneyFormatter.format(overview.totalInCents)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            list.budgetInCents?.let { Text("Presupuesto: ${MoneyFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun CreateListDialog(isSaving: Boolean, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
                FinanceTextField(
                    budget, { budget = sanitizeAmountInput(it) }, "Presupuesto (opcional)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
            }
        },
        confirmButton = { TextButton({ onCreate(name, budget) }, enabled = !isSaving) { Text(if (isSaving) "Guardando…" else "Crear") } },
        dismissButton = { TextButton(onDismiss, enabled = !isSaving) { Text("Cancelar") } },
    )
}

internal fun ShoppingListStatus.spanishLabel(): String = when (this) {
    ShoppingListStatus.PENDING -> "Pendiente"
    ShoppingListStatus.SHOPPING -> "En compra"
    ShoppingListStatus.COMPLETED -> "Finalizada"
}
