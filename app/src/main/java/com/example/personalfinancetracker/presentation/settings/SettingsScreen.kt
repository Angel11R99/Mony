package com.example.personalfinancetracker.presentation.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.presentation.components.ModuleTitle
import com.example.personalfinancetracker.ui.theme.AppAppearance
import com.example.personalfinancetracker.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    appearance: AppAppearance,
    onBack: () -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onPrimaryChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    var editingColor by remember { mutableStateOf<ColorRole?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModuleTitle("AJUSTES") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                    onClick = { editingColor = ColorRole.PRIMARY },
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
                    onClick = { editingColor = ColorRole.ACCENT },
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
            item {
                SecondaryButton(
                    text = "Restaurar apariencia original",
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    editingColor?.let { role ->
        ColorPickerDialog(
            title = if (role == ColorRole.PRIMARY) "Color principal" else "Color secundario",
            currentArgb = if (role == ColorRole.PRIMARY) appearance.primaryArgb else appearance.accentArgb,
            presets = if (role == ColorRole.PRIMARY) primaryPresets else accentPresets,
            onDismiss = { editingColor = null },
            onSelect = {
                if (role == ColorRole.PRIMARY) onPrimaryChange(it) else onAccentChange(it)
                editingColor = null
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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

    fun selectPreset(argb: Int) {
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
                        Surface(
                            modifier = Modifier.size(42.dp).clickable { selectPreset(argb) },
                            color = Color(argb),
                            shape = CircleShape,
                            border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline),
                        ) {
                            if (selected) Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Check,
                                    null,
                                    tint = if (Color(argb).luminance() > 0.48f) Color.Black else Color.White,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selectedArgb) }) { Text("Aplicar") }
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

private val AppThemeMode.label: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "Sistema"
        AppThemeMode.LIGHT -> "Claro"
        AppThemeMode.DARK -> "Oscuro"
    }

private val primaryPresets = listOf(
    0xFF7C3AED.toInt(), 0xFF2563EB.toInt(), 0xFF0891B2.toInt(), 0xFF059669.toInt(),
    0xFFCA8A04.toInt(), 0xFFEA580C.toInt(), 0xFFDB2777.toInt(), 0xFF52525B.toInt(),
)
private val accentPresets = listOf(
    0xFFFF6B73.toInt(), 0xFFDC2626.toInt(), 0xFFF97316.toInt(), 0xFFDB2777.toInt(),
    0xFF9333EA.toInt(), 0xFF2563EB.toInt(), 0xFF0D9488.toInt(), 0xFF52525B.toInt(),
)
