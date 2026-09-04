package com.angel.mony.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.defaultCycleSchedules
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.AppFontFamily
import com.angel.mony.ui.theme.AppShapeStyle
import com.angel.mony.ui.theme.AppThemeMode
import com.angel.mony.ui.theme.BackgroundDecoration

import java.time.LocalTime

private enum class SettingsGroup { MAIN, APPEARANCE, NAVIGATION, FINANCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearance: AppAppearance,
    isDarkTheme: Boolean,
    moduleBarConfig: com.angel.mony.navigation.FloatingModuleBarConfig,
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onBack: () -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onPrimaryChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onReset: () -> Unit,
    onShapeStyleChange: (AppShapeStyle) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onBackgroundDecorationChange: (BackgroundDecoration) -> Unit,
    onBackgroundIntensityChange: (Float) -> Unit,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onAutomaticCloseTimeChange: (LocalTime) -> Unit,
    onModuleBarVisibleRoutesChange: (Set<String>) -> Unit,
    onModuleBarShowLabelsChange: (Boolean) -> Unit,
    onModuleBarLabelTextSizeChange: (Float) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var currentGroup by rememberSaveable { mutableStateOf(SettingsGroup.MAIN) }
    var editingColor by remember { mutableStateOf<ColorRole?>(null) }
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

    when (currentGroup) {
        SettingsGroup.MAIN -> SettingsMainScreen(
            appearance = appearance,
            moduleBarConfig = moduleBarConfig,
            currentPeriod = budget?.period,
            budgetAmountInCents = budget?.amountInCents,
            onBack = onBack,
            onGroupClick = { currentGroup = it },
        )
        SettingsGroup.APPEARANCE -> AppearanceSettingsScreen(
            appearance = appearance,
            isDarkTheme = isDarkTheme,
            onBack = { currentGroup = SettingsGroup.MAIN },
            onThemeChange = onThemeChange,
            onEditPrimary = { editingColor = ColorRole.PRIMARY },
            onEditAccent = { editingColor = ColorRole.ACCENT },
            onReset = onReset,
            onShapeStyleChange = onShapeStyleChange,
            onFontFamilyChange = onFontFamilyChange,
            onBackgroundDecorationChange = onBackgroundDecorationChange,
            onBackgroundIntensityChange = onBackgroundIntensityChange,
            editingColor = editingColor,
            onEditingColorChange = { editingColor = it },
        )
        SettingsGroup.NAVIGATION -> NavigationSettingsScreen(
            moduleBarConfig = moduleBarConfig,
            onBack = { currentGroup = SettingsGroup.MAIN },
            onModuleBarVisibleRoutesChange = onModuleBarVisibleRoutesChange,
            onModuleBarShowLabelsChange = onModuleBarShowLabelsChange,
            onModuleBarLabelTextSizeChange = onModuleBarLabelTextSizeChange,
        )
        SettingsGroup.FINANCE -> FinanceSettingsScreen(
            automaticCycleClose = automaticCycleClose,
            automaticCloseTime = automaticCloseTime,
            currentSchedules = budget?.cycleSchedules
                ?: defaultCycleSchedules(budget?.period ?: BudgetPeriod.FORTNIGHTLY),
            currentPeriod = budget?.period ?: BudgetPeriod.FORTNIGHTLY,
            budgetAmountInCents = budget?.amountInCents,
            isSavingCycles = isSavingCycles,
            isSavingBudget = isSavingBudget,
            alertsEnabled = alertsEnabled,
            onBack = { currentGroup = SettingsGroup.MAIN },
            onAutomaticCycleCloseChange = onAutomaticCycleCloseChange,
            onAutomaticCloseTimeChange = onAutomaticCloseTimeChange,
            onAlertsEnabledChange = viewModel::setAlertsEnabled,
            onSchedulesSave = viewModel::updateCycleSchedules,
            onPeriodChange = { period ->
                if (budget == null) {
                    viewModel.saveBudget(
                        amount = "",
                        period = period,
                        onSaved = {},
                    )
                } else {
                    viewModel.updateBudgetPeriod(period)
                }
            },
            onBudgetSave = { amount, period, onSaved ->
                viewModel.saveBudget(amount = amount, period = period, onSaved = onSaved)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainScreen(
    appearance: AppAppearance,
    moduleBarConfig: com.angel.mony.navigation.FloatingModuleBarConfig,
    currentPeriod: BudgetPeriod?,
    budgetAmountInCents: Long?,
    onBack: () -> Unit,
    onGroupClick: (SettingsGroup) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Ajustes") },
                actions = {
                    GlobalOutlinedIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver",
                        onClick = onBack,
                    )
                    Spacer(Modifier.width(14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Personaliza cómo funciona la aplicación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── APARIENCIA ──
            item {
                SettingsGroupRow(
                    icon = Icons.Outlined.Palette,
                    title = "Apariencia",
                    description = "Tema, colores, estilo y fondo",
                    summary = appearance.themeMode.label,
                    onClick = { onGroupClick(SettingsGroup.APPEARANCE) },
                )
            }

            // ── NAVEGACIÓN ──
            item {
                val visibleCount = moduleBarConfig.visibleRoutes.size
                SettingsGroupRow(
                    icon = Icons.Outlined.Tune,
                    title = "Navegación",
                    description = "Barra y módulos visibles",
                    summary = "$visibleCount módulos",
                    onClick = { onGroupClick(SettingsGroup.NAVIGATION) },
                )
            }

            // ── FINANZAS ──
            item {
                val periodLabel = when (currentPeriod) {
                    BudgetPeriod.MONTHLY -> "Mensual"
                    BudgetPeriod.FORTNIGHTLY -> "Quincenal"
                    null -> null
                }
                val summaryParts = buildList {
                    periodLabel?.let { add(it) }
                    if (budgetAmountInCents != null) {
                        add(com.angel.mony.core.MoneyFormatter.format(budgetAmountInCents))
                    }
                }
                SettingsGroupRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Finanzas",
                    description = "Ciclos, presupuesto y categorías",
                    summary = summaryParts.joinToString(" · ").ifEmpty { null },
                    onClick = { onGroupClick(SettingsGroup.FINANCE) },
                )
            }
        }
    }
}
