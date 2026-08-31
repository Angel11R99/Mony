package com.angel.mony.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.categories.CategoriesTab
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.defaultCycleSchedules
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.AppFontFamily
import com.angel.mony.ui.theme.AppShapeStyle
import com.angel.mony.ui.theme.AppThemeMode
import com.angel.mony.ui.theme.BackgroundDecoration
import com.angel.mony.ui.theme.accentPresets
import com.angel.mony.ui.theme.createAppShapes
import com.angel.mony.ui.theme.createAppTypography
import com.angel.mony.ui.theme.displayName
import com.angel.mony.ui.theme.isColorCompatible
import com.angel.mony.ui.theme.primaryPresets
import com.angel.mony.ui.theme.recommendedFont
import com.angel.mony.ui.theme.subtitle
import com.angel.mony.ui.theme.toComposeFontFamily
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var editingColor by remember { mutableStateOf<ColorRole?>(null) }
    var showingTimePicker by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.APPEARANCE) }
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSavingCycles by viewModel.isSavingCycles.collectAsStateWithLifecycle()
    val alertsEnabled by viewModel.alertsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            viewModel.consumeMessage()
        }
    }

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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsTab.entries.forEach { tab ->
                    SettingsTabItem(
                        tab = tab,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            when (selectedTab) {
                SettingsTab.APPEARANCE -> AppearanceTab(
                    appearance = appearance,
                    isDarkTheme = isDarkTheme,
                    moduleBarConfig = moduleBarConfig,
                    onThemeChange = onThemeChange,
                    onEditPrimary = { editingColor = ColorRole.PRIMARY },
                    onEditAccent = { editingColor = ColorRole.ACCENT },
                    onReset = onReset,
                    onShapeStyleChange = onShapeStyleChange,
                    onFontFamilyChange = onFontFamilyChange,
                    onBackgroundDecorationChange = onBackgroundDecorationChange,
                    onBackgroundIntensityChange = onBackgroundIntensityChange,
                    onModuleBarVisibleRoutesChange = onModuleBarVisibleRoutesChange,
                    onModuleBarShowLabelsChange = onModuleBarShowLabelsChange,
                    onModuleBarLabelTextSizeChange = onModuleBarLabelTextSizeChange,
                )
                SettingsTab.CYCLES -> CyclesTab(
                    automaticCycleClose = automaticCycleClose,
                    automaticCloseTime = automaticCloseTime,
                    onAutomaticCycleCloseChange = onAutomaticCycleCloseChange,
                    onPickCloseTime = { showingTimePicker = true },
                    currentSchedules = budget?.cycleSchedules
                        ?: defaultCycleSchedules(budget?.period ?: BudgetPeriod.FORTNIGHTLY),
                    isSaving = isSavingCycles,
                    alertsEnabled = alertsEnabled,
                    onAlertsEnabledChange = viewModel::setAlertsEnabled,
                    onSchedulesSave = viewModel::updateCycleSchedules,
                )
                SettingsTab.CATEGORIES -> CategoriesTab()
            }
        }
    }

    editingColor?.let { role ->
        ColorPickerDialog(
            title = if (role == ColorRole.PRIMARY) "Color principal" else "Color secundario",
            currentArgb = if (role == ColorRole.PRIMARY) appearance.primaryArgb else appearance.accentArgb,
            presets = if (role == ColorRole.PRIMARY) primaryPresets else accentPresets,
            isDarkTheme = isDarkTheme,
            onDismiss = { editingColor = null },
            onSelect = {
                if (role == ColorRole.PRIMARY) onPrimaryChange(it) else onAccentChange(it)
                editingColor = null
            },
        )
    }

    if (showingTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = automaticCloseTime.hour,
            initialMinute = automaticCloseTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showingTimePicker = false },
            icon = { Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
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
                        showingTimePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showingTimePicker = false }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun SettingsTabItem(
    tab: SettingsTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                tab.label,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceTab(
    appearance: AppAppearance,
    isDarkTheme: Boolean,
    moduleBarConfig: com.angel.mony.navigation.FloatingModuleBarConfig,
    onThemeChange: (AppThemeMode) -> Unit,
    onEditPrimary: () -> Unit,
    onEditAccent: () -> Unit,
    onReset: () -> Unit,
    onShapeStyleChange: (AppShapeStyle) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onBackgroundDecorationChange: (BackgroundDecoration) -> Unit,
    onBackgroundIntensityChange: (Float) -> Unit,
    onModuleBarVisibleRoutesChange: (Set<String>) -> Unit,
    onModuleBarShowLabelsChange: (Boolean) -> Unit,
    onModuleBarLabelTextSizeChange: (Float) -> Unit,
) {
    var selectedSection by rememberSaveable { mutableStateOf(AppearanceSection.THEME_AND_COLOR) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "SECCIÓN DE APARIENCIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppearanceSection.entries.forEach { section ->
                            FilterChip(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section },
                                label = { Text(section.label) },
                                leadingIcon = if (selectedSection == section) {
                                    { Icon(Icons.Outlined.Check, null, Modifier.size(17.dp)) }
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
                }
            }
        }
        if (selectedSection == AppearanceSection.THEME_AND_COLOR) {
        item { SectionTitle("TEMA", "Elige cuándo usar la versión clara u oscura.") }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appearance.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = if (appearance.themeMode == mode) {
                            { Icon(Icons.Outlined.Check, null, Modifier.size(17.dp)) }
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
        }
        item { SectionTitle("COLORES", "Cada selección genera automáticamente sus tonos cercanos.") }
        item {
            ColorRoleCard(
                title = "Color principal",
                description = "Botones, selección, ingresos y elementos destacados.",
                color = Color(appearance.primaryArgb),
                family = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                ),
                onClick = onEditPrimary,
            )
        }
        item {
            ColorRoleCard(
                title = "Color secundario",
                description = "Gastos, alertas y acciones que requieren atención.",
                color = Color(appearance.accentArgb),
                family = listOf(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.error,
                ),
                onClick = onEditAccent,
            )
        }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("VISTA PREVIA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("RD$25,000.00", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ingreso", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("−RD$2,000.00", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
        if (selectedSection == AppearanceSection.STYLE) {
        item { SectionTitle("FORMAS", "Elige la familia geométrica de botones, tarjetas y chips.") }
        item {
            ShapeStyleSelector(
                selected = appearance.shapeStyle,
                onSelect = onShapeStyleChange,
            )
            val rec = appearance.shapeStyle.recommendedFont()
            if (rec != appearance.fontFamily) {
                Text(
                    "Combina bien con: ${rec.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item {
            LiveShapePreviewCard()
        }
        item { SectionTitle("TIPOGRAFÍA", "Elige la fuente global de la aplicación.") }
        item {
            FontFamilySelector(
                selected = appearance.fontFamily,
                onSelect = onFontFamilyChange,
            )
        }
        }
        if (selectedSection == AppearanceSection.BACKGROUND) {
        item { SectionTitle("FONDO DECORATIVO", "Agrega una decoración sutil detrás del contenido.") }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackgroundDecoration.entries.forEach { decoration ->
                    FilterChip(
                        selected = appearance.backgroundDecoration == decoration,
                        onClick = { onBackgroundDecorationChange(decoration) },
                        label = { Text(decoration.label) },
                        leadingIcon = if (appearance.backgroundDecoration == decoration) {
                            { Icon(Icons.Outlined.Check, null, Modifier.size(17.dp)) }
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
        }
        if (appearance.backgroundDecoration != BackgroundDecoration.NONE) {
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Intensidad del fondo", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${(appearance.backgroundIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            value = appearance.backgroundIntensity,
                            onValueChange = onBackgroundIntensityChange,
                            valueRange = 0f..1f,
                        )
                    }
                }
            }
        }
        }
        if (selectedSection == AppearanceSection.MODULE_BAR) {
        item { SectionTitle("BARRA DE MÓDULOS", "Personaliza la barra de navegación inferior.") }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("MÓDULOS VISIBLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    com.angel.mony.navigation.moduleDestinations.forEach { dest ->
                        val isVisible = dest.route in moduleBarConfig.visibleRoutes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(dest.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(dest.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { enabled ->
                                    val newRoutes = if (enabled) {
                                        moduleBarConfig.visibleRoutes + dest.route
                                    } else {
                                        moduleBarConfig.visibleRoutes - dest.route
                                    }
                                    if (newRoutes.isNotEmpty()) onModuleBarVisibleRoutesChange(newRoutes)
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Mostrar nombres", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Muestra el nombre del módulo debajo del icono.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = moduleBarConfig.showLabels,
                            onCheckedChange = onModuleBarShowLabelsChange,
                        )
                    }
                    if (moduleBarConfig.showLabels) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tamaño del texto", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    "Pequeño" to com.angel.mony.navigation.FloatingModuleBarPreferences.MIN_TEXT_SIZE,
                                    "Normal" to 10f,
                                    "Grande" to com.angel.mony.navigation.FloatingModuleBarPreferences.MAX_TEXT_SIZE,
                                ).forEach { (label, size) ->
                                    FilterChip(
                                        selected = moduleBarConfig.labelTextSize == size,
                                        onClick = { onModuleBarLabelTextSizeChange(size) },
                                        label = { Text(label) },
                                        modifier = Modifier.weight(1f),
                                        shape = MaterialTheme.shapes.small,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        item {
            SecondaryButton(
                text = "Restaurar apariencia original",
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShapeStyleSelector(
    selected: AppShapeStyle,
    onSelect: (AppShapeStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(AppShapeStyle.entries.size) { idx ->
                val style = AppShapeStyle.entries[idx]
                val isSelected = style == selected
                ShapePreviewItem(
                    style = style,
                    selected = isSelected,
                    onClick = { onSelect(style) },
                )
            }
        }
        // Grid compacta alternativa en 2 filas usando FlowRow para teléfonos pequeños sin scroll excesivo
        // Se mantiene LazyRow principal; FlowRow secundario no necesario.
    }
}

@Composable
private fun ShapePreviewItem(
    style: AppShapeStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewShapes = remember(style) { createAppShapes(style) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(84.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(width = 84.dp, height = 48.dp)
                .clip(previewShapes.buttonShape)
                .clickable(onClick = onClick),
            shape = previewShapes.buttonShape,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 14.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            previewShapes.extraSmall,
                        ),
                )
                if (selected) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp).align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
                    )
                }
            }
        }
        Text(
            style.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveShapePreviewCard() {
    FinanceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("VISTA PREVIA EN VIVO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            PrimaryButton(text = "Botón principal", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton(text = "Botón secundario", onClick = {}, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Chip") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Filtro") },
                    shape = MaterialTheme.shapes.small,
                )
            }
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tarjeta de ejemplo", style = MaterialTheme.typography.titleMedium)
                    Text("RD$ 1,250.00", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            androidx.compose.material3.OutlinedTextField(
                value = "Campo de texto",
                onValueChange = {},
                readOnly = true,
                label = { Text("Ejemplo") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

@Composable
private fun FontFamilySelector(
    selected: AppFontFamily,
    onSelect: (AppFontFamily) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(AppFontFamily.entries.size) { idx ->
                val family = AppFontFamily.entries[idx]
                val isSelected = family == selected
                val previewTypography = remember(family) { createAppTypography(family) }
                Surface(
                    modifier = Modifier
                        .width(112.dp)
                        .height(72.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSelect(family) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Aa",
                                style = previewTypography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 22.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                family.displayName,
                                style = previewTypography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (isSelected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Vista previa real — la tipografía se aplica en toda la app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CyclesTab(
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onPickCloseTime: () -> Unit,
    currentSchedules: List<BudgetCycleSchedule>,
    isSaving: Boolean,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onSchedulesSave: (List<BudgetCycleSchedule>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("CICLOS", "Controla cuándo y a qué hora se cierra el periodo del presupuesto.") }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                        Modifier.fillMaxWidth().clickable(onClick = onPickCloseTime),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Alertas de presupuesto", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Te avisa al alcanzar el 75% del presupuesto y cuando lo superes en el ciclo actual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = onAlertsEnabledChange,
                        )
                    }
                }
            }
        }
        item {
            CycleSchedulesCard(
                currentSchedules = currentSchedules,
                isSaving = isSaving,
                onSave = onSchedulesSave,
            )
        }
    }
}

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    APPEARANCE("Apariencia", Icons.Outlined.Palette),
    CYCLES("Ciclos", Icons.Outlined.Schedule),
    CATEGORIES("Categorías", Icons.Outlined.Category),
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
                        ) { Icon(Icons.Outlined.Close, "Quitar ciclo ${index + 1}") }
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
                    Icon(Icons.Outlined.Add, null)
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

private data class EditableCycleSchedule(
    val openingDay: String = "",
    val closingDay: String = "",
)

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-DO"))

@Composable
private fun ColorRoleCard(
    title: String,
    description: String,
    color: Color,
    family: List<Color>,
    onClick: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = color,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {}
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    family.forEach { tone ->
                        Box(Modifier.size(width = 28.dp, height = 6.dp).background(tone, MaterialTheme.shapes.extraSmall))
                    }
                }
            }
            Icon(Icons.Outlined.Palette, contentDescription = "Cambiar $title", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    title: String,
    currentArgb: Int,
    presets: List<Int>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val initialHsv = remember(currentArgb) { currentArgb.toHsv() }
    var hue by remember(currentArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(currentArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(currentArgb) { mutableFloatStateOf(initialHsv[2]) }
    val selectedColor = remember(hue, saturation, brightness) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }
    val selectedArgb = selectedColor.toArgb()

    val incompatiblePresets = remember(presets, isDarkTheme) {
        presets.filter { presetArgb -> !isColorCompatible(presetArgb, isDarkTheme) }.toSet()
    }
    val isCustomColorIncompatible = remember(selectedArgb, isDarkTheme) {
        !isColorCompatible(selectedArgb, isDarkTheme)
    }

    fun selectPreset(argb: Int) {
        if (argb in incompatiblePresets) return
        val hsv = argb.toHsv()
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Arrastra sobre la paleta hasta encontrar el color que quieras.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SaturationBrightnessPalette(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { newSaturation, newBrightness ->
                        saturation = newSaturation
                        brightness = newBrightness
                    },
                )
                HueBar(hue = hue, onHueChange = { hue = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        color = selectedColor,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {}
                    Column {
                        Text("Color seleccionado", style = MaterialTheme.typography.labelLarge)
                        Text("Puedes ajustarlo libremente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("COLORES RÁPIDOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    presets.forEach { argb ->
                        val selected = selectedArgb == argb
                        val incompatible = argb in incompatiblePresets
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .then(
                                    if (incompatible) Modifier
                                    else Modifier.clickable { selectPreset(argb) }
                                ),
                            color = Color(argb).copy(alpha = if (incompatible) 0.35f else 1f),
                            shape = CircleShape,
                            border = BorderStroke(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.onSurface
                                else if (incompatible) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline,
                            ),
                        ) {
                            if (selected) Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Check,
                                    null,
                                    tint = if (Color(argb).luminance() > 0.48f) Color.Black else Color.White,
                                )
                            }
                            if (incompatible) Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Close,
                                    null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                if (incompatiblePresets.isNotEmpty()) {
                    Text(
                        "Algunos colores no son compatibles con el tema actual por contraste.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isCustomColorIncompatible) {
                    Text(
                        "Este color no es compatible con el tema actual por bajo contraste.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelect(selectedArgb) },
                enabled = !isCustomColorIncompatible,
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun SaturationBrightnessPalette(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    fun update(position: Offset) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        onChange(
            (position.x / canvasSize.width).coerceIn(0f, 1f),
            (1f - position.y / canvasSize.height).coerceIn(0f, 1f),
        )
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.small)
            .onSizeChanged { canvasSize = it }
            .pointerInput(hue) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position)
                    do {
                        val change = awaitPointerEvent().changes.first()
                        update(change.position)
                        change.consume()
                    } while (change.pressed)
                }
            }
    ) {
        drawRect(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val center = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.7f), radius = 10.dp.toPx(), center = center, style = Stroke(4.dp.toPx()))
        drawCircle(Color.White, radius = 9.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    fun update(position: Offset) {
        if (canvasSize.width == 0) return
        onHueChange((position.x / canvasSize.width).coerceIn(0f, 1f) * 360f)
    }
    val colors = remember {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position)
                    do {
                        val change = awaitPointerEvent().changes.first()
                        update(change.position)
                        change.consume()
                    } while (change.pressed)
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(colors))
        val x = (hue / 360f).coerceIn(0f, 1f) * size.width
        drawLine(Color.Black.copy(alpha = 0.75f), Offset(x, 0f), Offset(x, size.height), 6.dp.toPx())
        drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx())
    }
}

private fun Int.toHsv(): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(this, it) }

private enum class ColorRole { PRIMARY, ACCENT }

private enum class AppearanceSection(val label: String) {
    THEME_AND_COLOR("Tema y color"),
    STYLE("Estilo"),
    BACKGROUND("Fondo"),
    MODULE_BAR("Barra"),
}

private val AppThemeMode.label: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "Sistema"
        AppThemeMode.LIGHT -> "Claro"
        AppThemeMode.DARK -> "Oscuro"
    }

private val BackgroundDecoration.label: String
    get() = when (this) {
        BackgroundDecoration.NONE -> "Ninguno"
        BackgroundDecoration.MEDICAL -> "Medical"
        BackgroundDecoration.CATS -> "Cats"
    }
