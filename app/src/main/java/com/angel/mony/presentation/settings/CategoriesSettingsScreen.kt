package com.angel.mony.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.angel.mony.presentation.categories.CategoriesTab

@Composable
fun CategoriesSettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Categorías", onBack = onBack)
        CategoriesTab(modifier = Modifier.weight(1f))
    }
}
