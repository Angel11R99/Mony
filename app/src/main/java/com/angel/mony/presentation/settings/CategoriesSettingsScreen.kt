package com.angel.mony.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.mony.presentation.categories.CategoriesTab
import com.angel.mony.presentation.components.StaggeredReveal

@Composable
fun CategoriesSettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Categorías", onBack = onBack)
        StaggeredReveal(
            screenKey = "settings-categories",
            modifier = Modifier.weight(1f).fillMaxWidth(),
            staggerMillis = 40,
            durationMillis = 180,
            slideOffsetDp = 8.dp,
        ) {
            add {
                CategoriesTab(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
