package com.example.personalfinancetracker.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.personalfinancetracker.presentation.components.FinanceCard
import com.example.personalfinancetracker.presentation.components.SecondaryButton
import com.example.personalfinancetracker.ui.theme.AppAppearance
import com.example.personalfinancetracker.ui.theme.AppThemeMode
import com.example.personalfinancetracker.ui.theme.parseHexColor
import com.example.personalfinancetracker.ui.theme.toHexColor

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
                title = { Text("AJUSTES", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
    var hex by remember(currentArgb) { mutableStateOf(currentArgb.toHexColor()) }
    val parsed = parseHexColor(hex)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Elige una muestra o escribe cualquier código hexadecimal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    presets.forEach { argb ->
                        val selected = parsed == argb
                        Surface(
                            modifier = Modifier.size(42.dp).clickable { hex = argb.toHexColor() },
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
                OutlinedTextField(
                    value = hex,
                    onValueChange = { if (it.length <= 7) hex = it.uppercase() },
                    label = { Text("Color hexadecimal") },
                    placeholder = { Text("#7C3AED") },
                    leadingIcon = { Box(Modifier.size(18.dp).background(parsed?.let(::Color) ?: Color.Transparent, CircleShape)) },
                    isError = hex.isNotBlank() && parsed == null,
                    supportingText = if (hex.isNotBlank() && parsed == null) ({ Text("Usa 6 caracteres, por ejemplo #2563EB") }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onSelect) }, enabled = parsed != null) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    )
}

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
