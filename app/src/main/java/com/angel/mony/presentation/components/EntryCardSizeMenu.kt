package com.angel.mony.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.mony.domain.model.EntryCardSize
import com.angel.mony.domain.model.label
import com.angel.mony.ui.iconography.MonyIcon
import com.angel.mony.ui.iconography.MonyIconRole

@Composable
fun EntryCardSizeMenu(
    selected: EntryCardSize,
    onSelect: (EntryCardSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }) {
            MonyIcon(MonyIcon.ListView, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(selected.label, modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EntryCardSize.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (selected == option) {
                        {
                            MonyIcon(
                                MonyIcon.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                role = MonyIconRole.STATE,
                            )
                        }
                    } else null,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
