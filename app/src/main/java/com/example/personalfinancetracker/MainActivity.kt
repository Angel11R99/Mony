package com.example.personalfinancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.navigation.FinanceApp
import com.example.personalfinancetracker.ui.theme.PersonalFinanceTrackerTheme
import com.example.personalfinancetracker.ui.theme.AppearancePreferences
import com.example.personalfinancetracker.ui.theme.AppThemeMode
import com.example.personalfinancetracker.widget.FinanceWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            val appearance by appearancePreferences.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appearance.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
            PersonalFinanceTrackerTheme(
                darkTheme = useDarkTheme,
                primarySeed = Color(appearance.primaryArgb),
                accentSeed = Color(appearance.accentArgb),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val initialType = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    FinanceApp(
                        initialType = initialType,
                        appearance = appearance,
                        onThemeChange = {
                            appearancePreferences.setThemeMode(it)
                            lifecycleScope.launch { FinanceWidget().updateAll(applicationContext) }
                        },
                        onPrimaryChange = {
                            appearancePreferences.setPrimaryColor(it)
                            lifecycleScope.launch { FinanceWidget().updateAll(applicationContext) }
                        },
                        onAccentChange = {
                            appearancePreferences.setAccentColor(it)
                            lifecycleScope.launch { FinanceWidget().updateAll(applicationContext) }
                        },
                        onResetAppearance = {
                            appearancePreferences.reset()
                            lifecycleScope.launch { FinanceWidget().updateAll(applicationContext) }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }
}
