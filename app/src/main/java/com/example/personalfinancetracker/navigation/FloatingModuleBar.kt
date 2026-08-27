package com.example.personalfinancetracker.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ModuleDestination(
    val label: String,
    val route: String,
    val icon: ImageVector,
)

internal val moduleDestinations = listOf(
    ModuleDestination("Inicio", "home", Icons.Outlined.AccountBalanceWallet),
    ModuleDestination("Fijos", "fixed", Icons.Outlined.Repeat),
    ModuleDestination("Recordatorios", "pending", Icons.Outlined.Notifications),
    ModuleDestination("Ahorros", "savings", Icons.Outlined.Savings),
    ModuleDestination("Lista", "list", Icons.Outlined.ShoppingCart),
    ModuleDestination("Estadísticas", "statistics", Icons.Outlined.Insights),
    ModuleDestination("Historial", "history", Icons.Outlined.History),
)

@Composable
internal fun FloatingModuleBar(
    selectedRoute: String?,
    config: FloatingModuleBarConfig,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleDestinations = rememberVisibleDestinations(config.visibleRoutes)
    val barHeight = if (config.showLabels) 68.dp else 62.dp
    Surface(
        modifier = modifier.fillMaxWidth().height(barHeight),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(visibleDestinations.size, key = { visibleDestinations[it].route }) { index ->
                val destination = visibleDestinations[index]
                val selected = isModuleSelected(destination.route, selectedRoute)
                ModuleItem(
                    destination = destination,
                    selected = selected,
                    showLabel = config.showLabels,
                    labelTextSize = config.labelTextSize,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.width(if (config.showLabels) 86.dp else 58.dp),
                )
            }
        }
    }
}

@Composable
private fun rememberVisibleDestinations(visibleRoutes: Set<String>): List<ModuleDestination> =
    moduleDestinations.filter { it.route in visibleRoutes }

internal fun isModuleSelected(
    moduleRoute: String,
    currentRoute: String?,
): Boolean = moduleRoute == currentRoute

@Composable
private fun ModuleItem(
    destination: ModuleDestination,
    selected: Boolean,
    showLabel: Boolean,
    labelTextSize: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "moduleItemColor",
    )
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "moduleItemContainer",
    )
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, label = "moduleIconScale")
    val itemShape = MaterialTheme.shapes.small
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemShape)
            .background(containerColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, itemShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            modifier = Modifier.size(21.dp).scale(iconScale),
            tint = contentColor,
        )
        if (showLabel) {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelTextSize.sp),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
