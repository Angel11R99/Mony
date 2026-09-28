package com.angel.mony.presentation.fortnight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FortnightItemProgress
import com.angel.mony.domain.model.FortnightItemStatus
import com.angel.mony.domain.model.FortnightItemType
import com.angel.mony.domain.model.FortnightPayment
import com.angel.mony.domain.model.FortnightPeriodStyle
import com.angel.mony.domain.model.FortnightPlanDetails
import com.angel.mony.domain.model.FortnightPlanItem
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.fortnightPeriodStyle
import com.angel.mony.domain.model.label
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esDO: Locale = Locale.forLanguageTag("es-DO")
private val dayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", esDO)
private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortnightScreen(
    onSettings: () -> Unit,
    onOpenTemplates: () -> Unit,
    viewModel: FortnightViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isCreatingPlan by viewModel.isCreatingPlanVisible.collectAsStateWithLifecycle()
    val planBudgetText by viewModel.planBudgetText.collectAsStateWithLifecycle()
    val itemDraft by viewModel.itemDraft.collectAsStateWithLifecycle()
    val paymentDraft by viewModel.paymentDraft.collectAsStateWithLifecycle()
    val overpayment by viewModel.overpaymentDraft.collectAsStateWithLifecycle()
    val isCloseConfirm by viewModel.isCloseConfirmVisible.collectAsStateWithLifecycle()
    val isDeletePlanConfirm by viewModel.isDeletePlanConfirmVisible.collectAsStateWithLifecycle()
    val pendingItemDelete by viewModel.pendingItemDelete.collectAsStateWithLifecycle()
    val pendingPaymentDelete by viewModel.pendingPaymentDelete.collectAsStateWithLifecycle()
    val isMonthly = fortnightPeriodStyle(state.budget) == FortnightPeriodStyle.MONTHLY
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { context.showToast(it); viewModel.consumeMessage() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle(if (isMonthly) "Mes" else "Quincena") },
                actions = {
                    if (state.canEditItems) {
                        GlobalOutlinedIconButton(Icons.Outlined.Add, "Agregar concepto", viewModel::startAddItem)
                        Spacer(Modifier.width(8.dp))
                    }
                    GlobalOutlinedIconButton(
                        Icons.Outlined.Inventory2,
                        "Plantillas",
                        onOpenTemplates,
                    )
                    Spacer(Modifier.width(8.dp))
                    GlobalSettingsButton(onSettings)
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).padding(top = 12.dp),
        ) {
            PeriodNavigator(
                periodLabel = buildString {
                    append(if (isMonthly) "Mes" else state.slot.label())
                    append(" · ")
                    append(state.period.start.format(dayMonthFormatter))
                    append(" – ")
                    append(state.period.endInclusive.format(dayMonthFormatter))
                },
                onPrevious = viewModel::goToPreviousPeriod,
                onNext = viewModel::goToNextPeriod,
                onCurrent = viewModel::goToCurrentPeriod,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.hasError -> Text(
                    "No se pudo cargar el período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                !state.hasPlan -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { EmptyPlanCard(isMonthly = isMonthly, onCreate = viewModel::showCreatePlan) }
                }
                else -> PlanContent(
                    state = state,
                    isSaving = isSaving,
                    isMonthly = isMonthly,
                    onPay = viewModel::startPayment,
                    onEditItem = viewModel::startEditItem,
                    onDeleteItem = viewModel::requestDeleteItem,
                    onRevertPayment = viewModel::requestDeletePayment,
                    onClosePlan = viewModel::requestClosePlan,
                    onReopenPlan = viewModel::reopenPlan,
                    onDeletePlan = viewModel::requestDeletePlan,
                )
            }
        }
    }

    if (isCreatingPlan) {
        CreatePlanSheet(
            state = state,
            budgetText = planBudgetText,
            isMonthly = isMonthly,
            isSaving = isSaving,
            onBudgetChange = viewModel::updatePlanBudget,
            onCreate = viewModel::createPlan,
            onDismiss = viewModel::hideCreatePlan,
        )
    }

    itemDraft?.let { draft ->
        ItemSheet(
            draft = draft,
            state = state,
            isSaving = isSaving,
            onChange = viewModel::updateItemDraft,
            onSave = viewModel::saveItem,
            onDismiss = viewModel::cancelItemDraft,
        )
    }

    paymentDraft?.let { draft ->
        PaymentDialog(
            draft = draft,
            isSaving = isSaving,
            onChange = viewModel::updatePaymentDraft,
            onSubmit = { viewModel.submitPayment() },
            onDismiss = viewModel::cancelPayment,
        )
    }

    overpayment?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelOverpayment,
            title = { Text("El abono supera lo pendiente") },
            text = {
                Text(
                    "Este concepto ya está pagado o casi pagado. Registrar este abono " +
                        "generará un gasto mayor que lo planificado.",
                )
            },
            confirmButton = {
                TextButton(viewModel::confirmOverpayment, enabled = !isSaving) { Text("Registrar igual") }
            },
            dismissButton = { TextButton(viewModel::cancelOverpayment) { Text("Cancelar") } },
        )
    }

    if (isCloseConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelClosePlan,
            title = { Text(if (isMonthly) "¿Cerrar el mes?" else "¿Cerrar la quincena?") },
            text = {
                Text(
                    "Dejarás de poder agregar, editar o pagar conceptos de este período. " +
                        "Los abonos ya registrados seguirán en el historial.",
                )
            },
            confirmButton = { TextButton(viewModel::confirmClosePlan, enabled = !isSaving) { Text("Cerrar") } },
            dismissButton = { TextButton(viewModel::cancelClosePlan) { Text("Cancelar") } },
        )
    }

    if (isDeletePlanConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeletePlan,
            title = { Text("¿Eliminar el plan?") },
            text = { Text("Se borrarán los conceptos planificados de este período. Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(viewModel::confirmDeletePlan, enabled = !isSaving) { Text("Eliminar") } },
            dismissButton = { TextButton(viewModel::cancelDeletePlan) { Text("Cancelar") } },
        )
    }

    pendingItemDelete?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteItem,
            title = { Text("Eliminar concepto") },
            text = { Text("¿Quitar \"${item.description}\" del plan?") },
            confirmButton = { TextButton(viewModel::confirmDeleteItem, enabled = !isSaving) { Text("Eliminar") } },
            dismissButton = { TextButton(viewModel::cancelDeleteItem) { Text("Cancelar") } },
        )
    }

    pendingPaymentDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeletePayment,
            title = { Text("Revertir abono") },
            text = { Text("Se eliminará el gasto asociado del historial. El concepto volverá a quedar pendiente.") },
            confirmButton = { TextButton(viewModel::confirmDeletePayment, enabled = !isSaving) { Text("Revertir") } },
            dismissButton = { TextButton(viewModel::cancelDeletePayment) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PeriodNavigator(
    periodLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, "Período anterior")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onCurrent) { Text("Ir a la actual") }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, "Período siguiente")
        }
    }
}

@Composable
private fun EmptyPlanCard(isMonthly: Boolean, onCreate: () -> Unit) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Text(
                if (isMonthly) "Este mes todavía no tiene plan." else "Esta quincena todavía no tiene plan.",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (isMonthly) {
                    "Crea el plan con el presupuesto del mes y agrega los gastos y ahorros " +
                        "que prevés. Después registra un abono cada vez que pagues. Planificar no " +
                        "registra ningún gasto."
                } else {
                    "Crea el plan con el presupuesto de la quincena y agrega los gastos y ahorros " +
                        "que prevés. Después registra un abono cada vez que pagues. Planificar no " +
                        "registra ningún gasto."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton("Crear plan", onCreate, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlanContent(
    state: FortnightUiState,
    isSaving: Boolean,
    isMonthly: Boolean,
    onPay: (FortnightPlanItem) -> Unit,
    onEditItem: (FortnightPlanItem) -> Unit,
    onDeleteItem: (FortnightPlanItem) -> Unit,
    onRevertPayment: (Long) -> Unit,
    onClosePlan: () -> Unit,
    onReopenPlan: () -> Unit,
    onDeletePlan: () -> Unit,
) {
    val details = state.details ?: return
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(details, isMonthly) }

        if (details.items.isEmpty()) {
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "No has planificado conceptos.",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (!state.isClosed) {
                            Text(
                                if (isMonthly) {
                                    "Agrega tus gastos y ahorros previstos del mes."
                                } else {
                                    "Agrega tus gastos y ahorros previstos de la quincena."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            items(details.itemProgress, key = { it.item.id }) { progress ->
                FortnightItemCard(
                    progress = progress,
                    categories = state.categories,
                    savingsGoals = state.savingsGoals,
                    canEdit = state.canEditItems,
                    enabled = !isSaving,
                    onPay = { onPay(progress.item) },
                    onEdit = { onEditItem(progress.item) },
                    onDelete = { onDeleteItem(progress.item) },
                )
            }
        }

        if (details.payments.isNotEmpty()) {
            item {
                Text(
                    "Abonos registrados",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(details.payments, key = { it.id }) { payment ->
                PaymentRow(
                    payment = payment,
                    itemDescription = details.items
                        .firstOrNull { it.id == payment.itemId }?.description ?: "Concepto",
                    canRevert = !state.isClosed,
                    enabled = !isSaving,
                    onRevert = { onRevertPayment(payment.id) },
                )
            }
        }

        item {
            PlanActions(
                isClosed = state.isClosed,
                isMonthly = isMonthly,
                enabled = !isSaving,
                onClosePlan = onClosePlan,
                onReopenPlan = onReopenPlan,
                onDeletePlan = onDeletePlan,
            )
        }
    }
}

@Composable
private fun SummaryCard(details: FortnightPlanDetails, isMonthly: Boolean) {
    val errorColor = MaterialTheme.colorScheme.error
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (isMonthly) "Resumen del mes" else "Resumen de la quincena",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (details.plan.isClosed) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Cerrada") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                }
            }
            Spacer(Modifier.padding(top = 4.dp))
            FinanceDetailRow("Presupuesto", MoneyFormatter.format(details.plan.budgetInCents))
            FinanceDetailRow("Planificado", MoneyFormatter.format(details.totalPlannedInCents))
            FinanceDetailRow(
                "Pagado",
                MoneyFormatter.format(details.totalPaidInCents),
                valueColor = MaterialTheme.colorScheme.primary,
            )
            FinanceDetailRow("Pendiente", MoneyFormatter.format(details.totalPendingInCents))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            FinanceDetailRow(
                label = if (details.remainingInCents < 0) "Excedido" else "Dinero restante",
                value = MoneyFormatter.format(details.remainingInCents),
                valueColor = if (details.remainingInCents < 0) errorColor else MaterialTheme.colorScheme.onSurface,
            )
            if (details.isOverAssigned) {
                Text(
                    if (isMonthly) {
                        "Has planificado más que el presupuesto del mes."
                    } else {
                        "Has planificado más que el presupuesto de la quincena."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = errorColor,
                )
            }
        }
    }
}

@Composable
private fun FortnightItemCard(
    progress: FortnightItemProgress,
    categories: List<Category>,
    savingsGoals: List<SavingsGoalProgress>,
    canEdit: Boolean,
    enabled: Boolean,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val item = progress.item
    val category = categories.firstOrNull { it.id == item.categoryId }
    val goal = savingsGoals.firstOrNull { it.goal.id == item.savingsGoalId }
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.description,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append(category?.name ?: "Sin categoría")
                            if (item.type == FortnightItemType.SAVINGS && goal != null) {
                                append(" · ")
                                append(goal.goal.name)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(progress.status)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AmountBlock("Planificado", MoneyFormatter.format(item.plannedAmountInCents))
                AmountBlock("Pagado", MoneyFormatter.format(progress.paidInCents))
                AmountBlock("Pendiente", MoneyFormatter.format(progress.pendingInCents))
            }

            if (progress.status != FortnightItemStatus.PAID) {
                PrimaryButton(
                    text = "Registrar abono",
                    onClick = onPay,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit && enabled,
                )
            }
            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit, enabled = enabled) {
                        Icon(Icons.Outlined.Edit, "Editar concepto")
                    }
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(Icons.Outlined.Delete, "Eliminar concepto")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusChip(status: FortnightItemStatus) {
    val (container, content) = when (status) {
        FortnightItemStatus.PAID -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        FortnightItemStatus.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        FortnightItemStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        FortnightItemStatus.PAID -> Icons.Outlined.Payments
        FortnightItemStatus.PARTIAL -> Icons.Outlined.CalendarMonth
        FortnightItemStatus.PENDING -> Icons.Outlined.CalendarMonth
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status.label()) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content,
            disabledLeadingIconContentColor = content,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun PaymentRow(
    payment: FortnightPayment,
    itemDescription: String,
    canRevert: Boolean,
    enabled: Boolean,
    onRevert: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(itemDescription, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    payment.date.format(dayMonthFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(MoneyFormatter.format(payment.amountInCents), style = MaterialTheme.typography.bodyMedium)
            if (canRevert) {
                IconButton(onClick = onRevert, enabled = enabled) {
                    Icon(Icons.Outlined.Undo, "Revertir abono")
                }
            }
        }
    }
}

@Composable
private fun PlanActions(
    isClosed: Boolean,
    isMonthly: Boolean,
    enabled: Boolean,
    onClosePlan: () -> Unit,
    onReopenPlan: () -> Unit,
    onDeletePlan: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isClosed) {
            SecondaryButton(
                text = if (isMonthly) "Reabrir mes" else "Reabrir quincena",
                onClick = onReopenPlan,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Outlined.LockOpen,
                enabled = enabled,
            )
            Text(
                if (isMonthly) {
                    "Este mes está cerrado. Reábrelo para volver a registrar abonos."
                } else {
                    "Esta quincena está cerrada. Reábrela para volver a registrar abonos."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SecondaryButton(
                text = if (isMonthly) "Cerrar mes" else "Cerrar quincena",
                onClick = onClosePlan,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Outlined.Lock,
                enabled = enabled,
            )
        }
        SecondaryButton(
            text = "Eliminar plan",
            onClick = onDeletePlan,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Delete,
            enabled = enabled && !isClosed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreatePlanSheet(
    state: FortnightUiState,
    budgetText: String,
    isMonthly: Boolean,
    isSaving: Boolean,
    onBudgetChange: (String) -> Unit,
    onCreate: (Set<Long>, Map<Long, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTemplates by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val categoryAmounts = remember { mutableStateOf(emptyMap<Long, String>()) }
    var expandedCategories by remember { mutableStateOf(false) }

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
                if (isMonthly) "Plan del mes" else "Plan de la quincena",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "El presupuesto solo guía tu planificación: no registra ningún gasto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FinanceTextField(
                value = budgetText,
                onValueChange = { onBudgetChange(sanitizeAmountInput(it)) },
                label = if (isMonthly) "Presupuesto del mes" else "Presupuesto de la quincena",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = AmountVisualTransformation,
            )

            if (state.applicableTemplates.isNotEmpty()) {
                Text("Desde plantillas", style = MaterialTheme.typography.labelLarge)
                state.applicableTemplates.forEach { template ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = template.id in selectedTemplates,
                                onClick = {
                                    selectedTemplates = if (template.id in selectedTemplates) {
                                        selectedTemplates - template.id
                                    } else {
                                        selectedTemplates + template.id
                                    }
                                },
                                label = { Text(template.description, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                MoneyFormatter.format(template.amountFor(state.slot) ?: 0L),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Categorías",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { expandedCategories = !expandedCategories }) {
                        Text(if (expandedCategories) "Ocultar" else "Mostrar")
                    }
                }
                if (expandedCategories) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.categories.forEach { category ->
                            val selected = category.id in categoryAmounts.value
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    categoryAmounts.value = if (selected) {
                                        categoryAmounts.value - category.id
                                    } else {
                                        categoryAmounts.value + (category.id to "")
                                    }
                                },
                                label = {
                                    Text(category.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                        if (state.categories.size % 2 != 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    val selectedCategories = state.categories.filter { it.id in categoryAmounts.value }
                    if (selectedCategories.isNotEmpty()) {
                        Text("Monto por categoría", style = MaterialTheme.typography.labelLarge)
                        selectedCategories.forEach { category ->
                            FinanceTextField(
                                value = categoryAmounts.value[category.id].orEmpty(),
                                onValueChange = { text ->
                                    val sanitized = sanitizeAmountInput(text)
                                    categoryAmounts.value = categoryAmounts.value + (category.id to sanitized)
                                },
                                label = category.name,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                visualTransformation = AmountVisualTransformation,
                            )
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Crear plan",
                onClick = { onCreate(selectedTemplates, categoryAmounts.value) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            )
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemSheet(
    draft: FortnightItemDraft,
    state: FortnightUiState,
    isSaving: Boolean,
    onChange: ((FortnightItemDraft) -> FortnightItemDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var goalMenuOpen by remember { mutableStateOf(false) }
    val selectedCategory = state.categories.firstOrNull { it.id == draft.categoryId }
    val selectedGoal = state.openSavingsGoals.firstOrNull { it.goal.id == draft.savingsGoalId }

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
                if (draft.isEditing) "Editar concepto" else "Nuevo concepto",
                style = MaterialTheme.typography.titleMedium,
            )
            FinanceTextField(
                value = draft.description,
                onValueChange = { text -> onChange { it.copy(description = text) } },
                label = "Descripción",
                singleLine = true,
            )
            FinanceTextField(
                value = draft.amountText,
                onValueChange = { text -> onChange { it.copy(amountText = sanitizeAmountInput(text)) } },
                label = "Monto planificado",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = AmountVisualTransformation,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.type == FortnightItemType.EXPENSE,
                    onClick = { onChange { it.copy(type = FortnightItemType.EXPENSE) } },
                    label = { Text("Gasto") },
                    leadingIcon = { Icon(Icons.Outlined.Payments, null, Modifier.size(16.dp)) },
                )
                FilterChip(
                    selected = draft.type == FortnightItemType.SAVINGS,
                    onClick = { onChange { it.copy(type = FortnightItemType.SAVINGS) } },
                    label = { Text("Ahorro") },
                    leadingIcon = { Icon(Icons.Outlined.Savings, null, Modifier.size(16.dp)) },
                )
            }

            Box {
                SecondaryButton(
                    text = selectedCategory?.name ?: "Selecciona una categoría",
                    onClick = { categoryMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categoryMenuOpen = false
                                onChange { it.copy(categoryId = category.id) }
                            },
                        )
                    }
                }
            }

            if (draft.type == FortnightItemType.SAVINGS) {
                Box {
                    SecondaryButton(
                        text = selectedGoal?.goal?.name ?: "Selecciona la meta de ahorro",
                        onClick = { goalMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = goalMenuOpen, onDismissRequest = { goalMenuOpen = false }) {
                        state.openSavingsGoals.forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(goal.goal.name) },
                                onClick = {
                                    goalMenuOpen = false
                                    onChange { it.copy(savingsGoalId = goal.goal.id) }
                                },
                            )
                        }
                    }
                }
            }

            FinanceTextField(
                value = draft.note,
                onValueChange = { text -> onChange { it.copy(note = text) } },
                label = "Nota (opcional)",
                singleLine = true,
            )

            PrimaryButton(
                text = if (draft.isEditing) "Guardar cambios" else "Agregar",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            )
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDialog(
    draft: FortnightPaymentDraft,
    isSaving: Boolean,
    onChange: ((FortnightPaymentDraft) -> FortnightPaymentDraft) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isValid = (draft.amountInCents ?: 0L) > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar abono") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    draft.itemDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FinanceTextField(
                    value = draft.amountText,
                    onValueChange = { text -> onChange { it.copy(amountText = sanitizeAmountInput(text)) } },
                    label = "Monto del abono",
                    singleLine = true,
                    isError = draft.amountText.isNotBlank() && !isValid,
                    errorMessage = "Indica un monto de abono mayor que cero.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                )
                SecondaryButton(
                    text = "Fecha: ${draft.date.format(dayMonthFormatter)}",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "El abono se registrará como gasto en tu historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = isValid && !isSaving) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surface,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.date.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onChange { it.copy(date = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)) }
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(pickerState) }
    }
}
