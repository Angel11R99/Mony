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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
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
import com.angel.mony.ui.theme.toComposeFontFamily

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    appearance: AppAppearance,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onEditPrimary: () -> Unit,
    onEditAccent: () -> Unit,
    onReset: () -> Unit,
    onShapeStyleChange: (AppShapeStyle) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onBackgroundDecorationChange: (BackgroundDecoration) -> Unit,
    onBackgroundIntensityChange: (Float) -> Unit,
    editingColor: ColorRole?,
    onEditingColorChange: (ColorRole?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Apariencia", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

        // ── TEMA ──
        item {
            SectionTitle("TEMA", "Elige cuándo usar la versión clara u oscura.")
        }
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

        // ── COLORES ──
        item {
            SectionTitle("COLORES", "Cada selección genera automáticamente sus tonos cercanos.")
        }
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

        // ── FORMAS ──
        item {
            SectionTitle("FORMAS", "Elige la familia geométrica de botones, tarjetas y chips.")
        }
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

        // ── TIPOGRAFÍA ──
        item {
            SectionTitle("TIPOGRAFÍA", "Elige la fuente global de la aplicación.")
        }
        item {
            FontFamilySelector(
                selected = appearance.fontFamily,
                onSelect = onFontFamilyChange,
            )
        }

        // ── FONDO DECORATIVO ──
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        item {
            SectionTitle("FONDO DECORATIVO", "Agrega una decoración sutil detrás del contenido.")
        }
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

        // ── RESTAURAR ──
        item { Spacer(Modifier.height(6.dp)) }
        item {
            SecondaryButton(
                text = "Restaurar apariencia original",
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    }

    // ── Color Picker Dialog ──
    editingColor?.let { role ->
        ColorPickerDialog(
            title = if (role == ColorRole.PRIMARY) "Color principal" else "Color secundario",
            currentArgb = if (role == ColorRole.PRIMARY) appearance.primaryArgb else appearance.accentArgb,
            presets = if (role == ColorRole.PRIMARY) primaryPresets else accentPresets,
            isDarkTheme = isDarkTheme,
            onDismiss = { onEditingColorChange(null) },
            onSelect = { argb ->
                onEditingColorChange(null)
                if (role == ColorRole.PRIMARY) {
                    onEditPrimary()
                } else {
                    onEditAccent()
                }
            },
        )
    }
}

// ── Helper composables ──

@Composable
internal fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ShapeStyleSelector(
    selected: AppShapeStyle,
    onSelect: (AppShapeStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
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
    }
}

@Composable
internal fun ShapePreviewItem(
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
internal fun LiveShapePreviewCard() {
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
internal fun FontFamilySelector(
    selected: AppFontFamily,
    onSelect: (AppFontFamily) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
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
internal fun ColorRoleCard(
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
internal fun ColorPickerDialog(
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
internal fun SaturationBrightnessPalette(
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
internal fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
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

internal fun Int.toHsv(): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(this, it) }

enum class ColorRole { PRIMARY, ACCENT }

internal val AppThemeMode.label: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "Sistema"
        AppThemeMode.LIGHT -> "Claro"
        AppThemeMode.DARK -> "Oscuro"
    }

internal val BackgroundDecoration.label: String
    get() = when (this) {
        BackgroundDecoration.NONE -> "Ninguno"
        BackgroundDecoration.MEDICAL -> "Medical"
        BackgroundDecoration.CATS -> "Cats"
    }
