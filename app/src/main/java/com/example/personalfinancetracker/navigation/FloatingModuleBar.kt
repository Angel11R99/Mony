package com.example.personalfinancetracker.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
    ModuleDestination("Estadísticas", "statistics", Icons.Outlined.Insights),
    ModuleDestination("Historial", "history", Icons.Outlined.History),
)

@Composable
internal fun FloatingModuleBar(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(62.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            moduleDestinations.forEach { destination ->
                val selected = isModuleSelected(destination.route, selectedRoute)
                ModuleItem(
                    destination = destination,
                    selected = selected,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun isModuleSelected(
    moduleRoute: String,
    currentRoute: String?,
): Boolean = moduleRoute == currentRoute

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
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, label = "moduleIconScale")
    val itemShape = MaterialTheme.shapes.small
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemShape)
            .background(containerColor)
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
        AutoShrinkingLabel(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

private val MIN_MODULE_LABEL_FONT_SIZE = 8.sp

@Composable
private fun AutoShrinkingLabel(
    text: String,
    style: TextStyle,
    color: Color,
) {
    var fontSize by remember(text) { mutableStateOf(style.fontSize) }
    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > MIN_MODULE_LABEL_FONT_SIZE) {
                val shrunk = fontSize * 0.92f
                fontSize = if (shrunk >= MIN_MODULE_LABEL_FONT_SIZE) shrunk else MIN_MODULE_LABEL_FONT_SIZE
            }
        },
    )
}
