package com.angel.mony.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.navigation.FloatingModuleBarConfig
import com.angel.mony.navigation.FloatingModuleBarPreferences
import com.angel.mony.navigation.ModuleTransitionStyle
import com.angel.mony.navigation.moduleEnterTransition
import com.angel.mony.navigation.moduleExitTransition
import com.angel.mony.navigation.moduleDestinations

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(
    moduleBarConfig: FloatingModuleBarConfig,
    onBack: () -> Unit,
    onModuleBarVisibleRoutesChange: (Set<String>) -> Unit,
    onModuleBarShowLabelsChange: (Boolean) -> Unit,
    onModuleBarLabelTextSizeChange: (Float) -> Unit,
    onModuleTransitionStyleChange: (ModuleTransitionStyle) -> Unit,
) {
    var previewStep by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Navegación", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item {
            SectionTitle("MÓDULOS VISIBLES", "Selecciona qué módulos aparecen en la barra de navegación.")
        }
        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    moduleDestinations.forEach { dest ->
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                    "Pequeño" to FloatingModuleBarPreferences.MIN_TEXT_SIZE,
                                    "Normal" to 10f,
                                    "Grande" to FloatingModuleBarPreferences.MAX_TEXT_SIZE,
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

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        item {
            SectionTitle(
                "ANIMACIÓN ENTRE MÓDULOS",
                "Elige cómo cambia el contenido al tocar otro módulo de la barra.",
            )
        }

        item {
            FinanceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModuleTransitionStyle.entries.forEach { style ->
                            FilterChip(
                                selected = moduleBarConfig.transitionStyle == style,
                                onClick = {
                                    onModuleTransitionStyleChange(style)
                                    previewStep++
                                },
                                label = { Text(style.label) },
                                shape = MaterialTheme.shapes.small,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }

                    Text(
                        moduleBarConfig.transitionStyle.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    AnimatedContent(
                        targetState = previewStep,
                        transitionSpec = {
                            moduleEnterTransition(moduleBarConfig.transitionStyle, forward = true) togetherWith
                                moduleExitTransition(moduleBarConfig.transitionStyle, forward = true)
                        },
                        label = "Vista previa de transición",
                    ) { step ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(76.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    if (step % 2 == 0) "Módulo Inicio" else "Módulo Estadísticas",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Vista previa · toca cualquier opción para repetir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
