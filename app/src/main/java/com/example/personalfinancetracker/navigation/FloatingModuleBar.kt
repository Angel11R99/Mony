package com.example.personalfinancetracker.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal data class ModuleDestination(
    val label: String,
    val route: String,
    val icon: ImageVector,
)

internal val moduleDestinations = listOf(
    ModuleDestination("Inicio", "home", Icons.Outlined.AccountBalanceWallet),
    ModuleDestination("Gasto", "add/EXPENSE", Icons.AutoMirrored.Outlined.TrendingDown),
    ModuleDestination("Ingreso", "add/INCOME", Icons.AutoMirrored.Outlined.TrendingUp),
    ModuleDestination("Historial", "history", Icons.Outlined.History),
)

@Composable
internal fun FloatingModuleBar(
    selectedRoute: String?,
    selectedTransactionType: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        moduleDestinations.forEach { destination ->
            val selected = isModuleSelected(destination.route, selectedRoute, selectedTransactionType)
            ModuleItem(
                destination = destination,
                selected = selected,
                onClick = { onNavigate(destination.route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun String?.isTransactionRoute(): Boolean =
    this?.startsWith("add/") == true || this?.startsWith("edit/") == true

internal fun isModuleSelected(
    moduleRoute: String,
    currentRoute: String?,
    transactionType: String?,
): Boolean = when {
    moduleRoute == "home" -> currentRoute == "home"
    moduleRoute == "history" -> currentRoute == "history"
    moduleRoute.endsWith("EXPENSE") ->
        currentRoute.isTransactionRoute() && transactionType == "EXPENSE"
    moduleRoute.endsWith("INCOME") ->
        currentRoute.isTransactionRoute() && transactionType == "INCOME"
    else -> false
}

@Composable
private fun ModuleItem(
    destination: ModuleDestination,
    selected: Boolean,
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
        else MaterialTheme.colorScheme.surface,
        label = "moduleItemContainer",
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        label = "moduleItemBorder",
    )
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, label = "moduleIconScale")
    val itemShape = MaterialTheme.shapes.small
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemShape)
            .background(containerColor)
            .border(1.dp, borderColor, itemShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp).scale(iconScale),
            tint = contentColor,
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}
