package com.angel.mony.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.mony.presentation.components.BudgetAmountDialog
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.StaggeredReveal
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.defaultCycleSchedules
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FinanceSettingsRoute(
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onBack: () -> Unit,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onAutomaticCloseTimeChange: (LocalTime) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSavingCycles by viewModel.isSavingCycles.collectAsStateWithLifecycle()
    val isSavingBudget by viewModel.isSavingBudget.collectAsStateWithLifecycle()
    val alertsEnabled by viewModel.alertsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

    FinanceSettingsScreen(
        automaticCycleClose = automaticCycleClose,
        automaticCloseTime = automaticCloseTime,
        currentSchedules = budget?.cycleSchedules
            ?: defaultCycleSchedules(budget?.period ?: BudgetPeriod.FORTNIGHTLY),
        currentPeriod = budget?.period ?: BudgetPeriod.FORTNIGHTLY,
        budgetAmountInCents = budget?.amountInCents,
        isSavingCycles = isSavingCycles,
        isSavingBudget = isSavingBudget,
        alertsEnabled = alertsEnabled,
        onBack = onBack,
        onAutomaticCycleCloseChange = onAutomaticCycleCloseChange,
        onAutomaticCloseTimeChange = onAutomaticCloseTimeChange,
        onAlertsEnabledChange = viewModel::setAlertsEnabled,
        onSchedulesSave = viewModel::updateCycleSchedules,
        onPeriodChange = { period ->
            if (budget == null) {
                viewModel.saveBudget(amount = "", period = period, onSaved = {})
            } else {
                viewModel.updateBudgetPeriod(period)
            }
        },
        onBudgetSave = viewModel::saveBudget,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FinanceSettingsScreen(
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    currentSchedules: List<BudgetCycleSchedule>,
    currentPeriod: BudgetPeriod,
    budgetAmountInCents: Long?,
    isSavingCycles: Boolean,
    isSavingBudget: Boolean,
    alertsEnabled: Boolean,
    onBack: () -> Unit,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onAutomaticCloseTimeChange: (LocalTime) -> Unit,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onSchedulesSave: (List<BudgetCycleSchedule>) -> Unit,
    onPeriodChange: (BudgetPeriod) -> Unit,
    onBudgetSave: (amount: String, period: BudgetPeriod, onSaved: () -> Unit) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf(false) }
    var newBudgetPeriod by remember { mutableStateOf(BudgetPeriod.FORTNIGHTLY) }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Finanzas", onBack = onBack)

        StaggeredReveal(
            screenKey = "settings-finance",
            modifier = Modifier.weight(1f).fillMaxWidth(),
            staggerMillis = 40,
            durationMillis = 180,
            slideOffsetDp = 8.dp,
        ) {
            add {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
        // ── CICLO FINANCIERO ──
        item {
            SectionTitle("CICLO FINANCIERO", "Define la duración, los días y el cierre de cada período.")
        }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("TIPO DE CICLO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Esta selección se aplica en toda la aplicación. Al cambiarla, los días del ciclo vuelven a sus valores predeterminados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = currentPeriod == period,
                                onClick = { onPeriodChange(period) },
                                enabled = !isSavingBudget,
                                label = { Text(if (period == BudgetPeriod.MONTHLY) "Mensual" else "Quincenal") },
                                leadingIcon = if (currentPeriod == period) {
                                    { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Check, null, Modifier.size(17.dp), role = com.angel.mony.ui.iconography.MonyIconRole.STATE) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = MaterialTheme.shapes.small,
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cierre automático", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Cierra el ciclo automáticamente cuando la fecha coincida con uno de tus días de cierre.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = automaticCycleClose,
                            onCheckedChange = onAutomaticCycleCloseChange,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = { showTimePicker = true }),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.angel.mony.ui.iconography.MonyIcon(
                            com.angel.mony.ui.iconography.MonyIcon.Calendar,
                            contentDescription = null,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Hora del cierre automático", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "El ciclo se cerrará al llegar a esta hora en un día de cierre.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            automaticCloseTime.format(timeFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // ── PERÍODOS DEL CICLO ──
        item {
            CycleSchedulesCard(
                currentSchedules = currentSchedules,
                isSaving = isSavingCycles,
                onSave = onSchedulesSave,
            )
        }

        // ── PRESUPUESTO ──
        item {
            SectionTitle("PRESUPUESTO", "Configura cuánto quieres administrar durante cada período.")
        }
        item {
            BudgetSettingsCard(
                amountInCents = budgetAmountInCents,
                isSaving = isSavingBudget,
                alertsEnabled = alertsEnabled,
                onAlertsEnabledChange = onAlertsEnabledChange,
                onEdit = { editingBudget = true },
            )
        }

                }
            }
        }
    }

    // ── Time Picker Dialog ──
    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = automaticCloseTime.hour,
            initialMinute = automaticCloseTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            icon = {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.Calendar,
                    null,
                )
            },
            title = { Text("Hora del cierre automático") },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAutomaticCloseTimeChange(
                            LocalTime.of(pickerState.hour, pickerState.minute)
                        )
                        showTimePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }

    // ── Budget Amount Dialog ──
    if (editingBudget) {
        BudgetAmountDialog(
            currentAmount = budgetAmountInCents,
            isSaving = isSavingBudget,
            description = "Configura el monto que quieres administrar en cada ciclo. El tipo y los días se definen en la sección de ciclo financiero.",
            onDismiss = {
                editingBudget = false
                if (budgetAmountInCents == null) newBudgetPeriod = BudgetPeriod.FORTNIGHTLY
            },
            onSave = { amount ->
                onBudgetSave(
                    amount,
                    budgetAmountInCents?.let { currentPeriod } ?: newBudgetPeriod,
                ) { editingBudget = false }
            },
        )
    }
}

// ── Helper composables for Finance ──

@Composable
private fun BudgetSettingsCard(
    amountInCents: Long?,
    isSaving: Boolean,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MONTO DEL PRESUPUESTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(
                amountInCents?.let(MoneyFormatter::format) ?: "Sin configurar",
                style = MaterialTheme.typography.headlineMedium,
                color = if (amountInCents == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
            PrimaryButton(
                text = if (amountInCents == null) "Configurar presupuesto" else "Editar presupuesto",
                onClick = onEdit,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.angel.mony.ui.iconography.MonyIcon(
                    com.angel.mony.ui.iconography.MonyIcon.AlertsEnabled,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Alertas de presupuesto", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Te avisa al alcanzar el 75% y cuando superes el presupuesto del ciclo actual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = alertsEnabled, onCheckedChange = onAlertsEnabledChange)
            }
        }
    }
}

private data class EditableCycleSchedule(
    val openingDay: String = "",
    val closingDay: String = "",
)

@Composable
private fun CycleSchedulesCard(
    currentSchedules: List<BudgetCycleSchedule>,
    isSaving: Boolean,
    onSave: (List<BudgetCycleSchedule>) -> Unit,
) {
    var schedules by remember(currentSchedules) {
        mutableStateOf(currentSchedules.map {
            EditableCycleSchedule(it.openingDay.toString(), it.closingDay.toString())
        })
    }
    val parsedSchedules = schedules.mapNotNull { schedule ->
        val openingDay = schedule.openingDay.toIntOrNull()
        val closingDay = schedule.closingDay.toIntOrNull()
        if (openingDay == null || openingDay !in 1..31 || closingDay == null || closingDay !in 1..31) null
        else BudgetCycleSchedule(openingDay, closingDay)
    }
    val valid = parsedSchedules.isNotEmpty() &&
        parsedSchedules.size == schedules.size &&
        parsedSchedules.map(BudgetCycleSchedule::openingDay).distinct().size == parsedSchedules.size
    val changed = valid && parsedSchedules != currentSchedules

    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PERÍODOS DEL CICLO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(
                "Define los días inclusivos de apertura y cierre. Si la apertura es mayor que el cierre, el período termina el mes siguiente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            schedules.forEachIndexed { index, schedule ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ciclo ${index + 1}", style = MaterialTheme.typography.labelLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = schedule.openingDay,
                            onValueChange = { newValue ->
                                schedules = schedules.toMutableList().also {
                                    it[index] = schedule.copy(openingDay = newValue.filter(Char::isDigit).take(2))
                                }
                            },
                            label = { Text("Apertura") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = schedule.closingDay,
                            onValueChange = { newValue ->
                                schedules = schedules.toMutableList().also {
                                    it[index] = schedule.copy(closingDay = newValue.filter(Char::isDigit).take(2))
                                }
                            },
                            label = { Text("Cierre") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                schedules = schedules.toMutableList().also { it.removeAt(index) }
                            },
                            enabled = schedules.size > 1,
                        ) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Close, "Quitar ciclo ${index + 1}") }
                    }
                }
            }
            if (!valid) {
                Text(
                    "Completa cada apertura y cierre con un día del 1 al 31. No repitas días de apertura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { schedules = schedules + EditableCycleSchedule() },
                    modifier = Modifier.weight(1f),
                ) {
                    com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Add, null)
                    Text("Agregar ciclo")
                }
                PrimaryButton(
                    text = if (isSaving) "Guardando…" else "Guardar",
                    onClick = { onSave(parsedSchedules) },
                    enabled = valid && changed && !isSaving,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))
