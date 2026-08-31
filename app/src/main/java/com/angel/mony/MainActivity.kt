package com.angel.mony

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.core.CyclePreferences
import com.angel.mony.core.showToast
import com.angel.mony.navigation.FinanceApp
import com.angel.mony.navigation.FloatingModuleBarPreferences
import com.angel.mony.ui.theme.PersonalFinanceTrackerTheme
import com.angel.mony.ui.theme.AppearancePreferences
import com.angel.mony.ui.theme.AppThemeMode
import com.angel.mony.widget.updateAllFinanceWidgets
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            val cyclePreferences = remember { CyclePreferences(applicationContext) }
            val moduleBarPreferences = remember { FloatingModuleBarPreferences(applicationContext) }
            val appearance by appearancePreferences.settings.collectAsStateWithLifecycle()
            val automaticCycleClose by cyclePreferences.automaticClose.collectAsStateWithLifecycle()
            val automaticCloseTime by cyclePreferences.automaticCloseTime.collectAsStateWithLifecycle()
            val moduleBarConfig by moduleBarPreferences.config.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appearance.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            LaunchedEffect(useDarkTheme) {
                val result = appearancePreferences.ensureColorsCompatible(useDarkTheme)
                if (result != null && result.anyChanged) {
                    runCatching { updateAllFinanceWidgets(applicationContext) }
                    val message = when {
                        result.primaryChanged && result.accentChanged ->
                            "Colores ajustados automáticamente por contraste con el tema."
                        result.primaryChanged ->
                            "Color principal ajustado automáticamente por contraste con el tema."
                        else ->
                            "Color secundario ajustado automáticamente por contraste con el tema."
                    }
                    applicationContext.showToast(message)
                }
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
                shapeStyle = appearance.shapeStyle,
                fontFamily = appearance.fontFamily,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val initialType = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    val initialEditId = intent.getLongExtra(EXTRA_EDIT_TRANSACTION_ID, -1L)
                        .takeIf { it >= 0L }
                    FinanceApp(
                        isDarkTheme = useDarkTheme,
                        moduleBarConfig = moduleBarConfig,
                        // When an edit was requested the type extra only selects
                        // the editor route; the add flow must not trigger too.
                        initialType = if (initialEditId == null) initialType else null,
                        initialDestination = intent.getStringExtra(EXTRA_DESTINATION),
                        initialEdit = if (initialEditId != null && initialType != null) {
                            initialEditId to initialType
                        } else {
                            null
                        },
                        appearance = appearance,
                        automaticCycleClose = automaticCycleClose,
                        automaticCloseTime = automaticCloseTime,
                        onThemeChange = {
                            val targetDark = when (it) {
                                AppThemeMode.SYSTEM -> systemDark
                                AppThemeMode.LIGHT -> false
                                AppThemeMode.DARK -> true
                            }
                            val result = appearancePreferences.setThemeModeWithAutoCorrection(it, targetDark)
                            lifecycleScope.launch { runCatching { updateAllFinanceWidgets(applicationContext) } }
                            if (result.anyChanged) {
                                val message = when {
                                    result.primaryChanged && result.accentChanged ->
                                        "Tema cambiado. Colores ajustados automáticamente por contraste."
                                    result.primaryChanged ->
                                        "Tema cambiado. Color principal ajustado automáticamente por contraste."
                                    else ->
                                        "Tema cambiado. Color secundario ajustado automáticamente por contraste."
                                }
                                applicationContext.showToast(message)
                            }
                        },
                        onPrimaryChange = {
                            appearancePreferences.setPrimaryColor(it)
                            lifecycleScope.launch { runCatching { updateAllFinanceWidgets(applicationContext) } }
                        },
                        onAccentChange = {
                            appearancePreferences.setAccentColor(it)
                            lifecycleScope.launch { runCatching { updateAllFinanceWidgets(applicationContext) } }
                        },
                        onResetAppearance = {
                            appearancePreferences.reset()
                            lifecycleScope.launch { runCatching { updateAllFinanceWidgets(applicationContext) } }
                        },
                        onShapeStyleChange = appearancePreferences::setShapeStyle,
                        onFontFamilyChange = appearancePreferences::setFontFamily,
                        onBackgroundDecorationChange = appearancePreferences::setBackgroundDecoration,
                        onBackgroundIntensityChange = appearancePreferences::setBackgroundIntensity,
                        onAutomaticCycleCloseChange = cyclePreferences::setAutomaticClose,
                        onAutomaticCloseTimeChange = cyclePreferences::setAutomaticCloseTime,
                        onModuleBarVisibleRoutesChange = moduleBarPreferences::setVisibleRoutes,
                        onModuleBarShowLabelsChange = moduleBarPreferences::setShowLabels,
                        onModuleBarLabelTextSizeChange = moduleBarPreferences::setLabelTextSize,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { runCatching { updateAllFinanceWidgets(applicationContext) } }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_EDIT_TRANSACTION_ID = "edit_transaction_id"
    }
}
