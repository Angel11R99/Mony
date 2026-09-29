package com.angel.mony.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.angel.mony.ui.theme.LocalAppShapes

/** Opción disponible en un selector abierto dentro de un bottom sheet. */
data class SelectionOption(val id: Long, val label: String)

/**
 * Campo de formulario que delega la elección a un selector ([FinanceSelectionSheet]).
 *
 * Reutiliza [FinanceTextField] como base visual para conservar silueta, borde y colores,
 * y superpone una capa táctil con rol de botón: el campo no acepta escritura, solo abre
 * el selector. Así el campo sigue leyéndose como parte del formulario y deja claro que es pulsable.
 */
@Composable
fun FinanceSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecciona una opción",
) {
    val shape = LocalAppShapes.current.textFieldShape
    Box(modifier.fillMaxWidth()) {
        FinanceTextField(
            value = value,
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick),
        )
    }
}

/**
 * Selector de opciones en bottom sheet pensado para listas largas.
 *
 * Sustituye a los `DropdownMenu`, que con muchas opciones se extendían por casi toda la
 * pantalla y tapaban el formulario. La hoja usa el drag handle estándar de Mony, la lista
 * se desplaza sola y queda acotada a un porcentaje de la pantalla para que nunca se salga
 * de la vista ni tape por completo el formulario que la abrió.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceSelectionSheet(
    title: String,
    options: List<SelectionOption>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    emptyMessage: String,
) {
    val listMaxHeight = selectionListMaxHeight()
    val listState = rememberLazyListState()
    val currentOptions by rememberUpdatedState(options)

    // Al reabrir, la opción elegida queda a la vista en vez de obliged a recorrer la lista.
    LaunchedEffect(selectedId) {
        val index = currentOptions.indexOfFirst { it.id == selectedId }
        if (index > 0) listState.scrollToItem(index)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar el selector")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (options.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = listMaxHeight),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(options, key = { it.id }) { option ->
                        SelectionRow(
                            option = option,
                            selected = option.id == selectedId,
                            onClick = { onSelect(option.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alto máximo de la lista como fracción de la pantalla.
 * Escala con el equipo (teléfono pequeño, grande o apaisado) sin depender de medidas fijas.
 */
@Composable
private fun selectionListMaxHeight(): Dp =
    (LocalConfiguration.current.screenHeightDp * LIST_HEIGHT_FRACTION).dp

private const val LIST_HEIGHT_FRACTION = 0.62f

@Composable
private fun SelectionRow(
    option: SelectionOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
