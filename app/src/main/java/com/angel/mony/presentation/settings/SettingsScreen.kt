package com.angel.mony.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.navigation.FloatingModuleBarConfig
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.GlobalOutlinedIconButton
import com.angel.mony.presentation.components.ModuleTitle
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.theme.AppAppearance

object SettingsRoutes {
    const val ROOT = "settings"
    const val PERSONALIZATION = "settings/personalization"
    const val NAVIGATION = "settings/navigation"
    const val FINANCE = "settings/finance"
    const val CATEGORIES = "settings/categories"

    val destinations = setOf(PERSONALIZATION, NAVIGATION, FINANCE, CATEGORIES)

    fun parentOf(route: String): String? = if (route in destinations) ROOT else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearance: AppAppearance,
    moduleBarConfig: FloatingModuleBarConfig,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val budget by viewModel.budget.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { ModuleTitle("Ajustes") },
                actions = {
                    GlobalOutlinedIconButton(
                        semanticIcon = MonyIcon.Back,
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
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "¿Qué quieres configurar?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Elige una sección para encontrar sus opciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        SettingsGroupRow(
                            icon = MonyIcon.Appearance,
                            title = "Personalización",
                            description = "Tema, colores, iconos y estilo",
                            summary = appearance.themeMode.label,
                            onClick = { onNavigate(SettingsRoutes.PERSONALIZATION) },
                        )
                        HorizontalDivider()
                        SettingsGroupRow(
                            icon = MonyIcon.Navigation,
                            title = "Navegación",
                            description = "Barra, módulos y animaciones",
                            summary = "${moduleBarConfig.visibleRoutes.size} módulos",
                            onClick = { onNavigate(SettingsRoutes.NAVIGATION) },
                        )
                        HorizontalDivider()
                        val periodLabel = when (budget?.period) {
                            BudgetPeriod.MONTHLY -> "Mensual"
                            BudgetPeriod.FORTNIGHTLY -> "Quincenal"
                            null -> null
                        }
                        val financeSummary = buildList {
                            periodLabel?.let(::add)
                            budget?.amountInCents?.let { add(MoneyFormatter.format(it)) }
                        }.joinToString(" · ").ifEmpty { null }
                        SettingsGroupRow(
                            icon = MonyIcon.Finance,
                            title = "Finanzas",
                            description = "Presupuesto, ciclos y alertas",
                            summary = financeSummary,
                            onClick = { onNavigate(SettingsRoutes.FINANCE) },
                        )
                        HorizontalDivider()
                        SettingsGroupRow(
                            icon = MonyIcon.Category,
                            title = "Categorías",
                            description = "Organiza ingresos y gastos",
                            onClick = { onNavigate(SettingsRoutes.CATEGORIES) },
                        )
                    }
                }
            }
        }
    }
}
