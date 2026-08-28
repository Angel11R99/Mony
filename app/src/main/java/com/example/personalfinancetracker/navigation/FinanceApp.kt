package com.example.personalfinancetracker.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.home.HomeScreen
import com.example.personalfinancetracker.presentation.statistics.StatisticsScreen
import com.example.personalfinancetracker.presentation.fixed.FixedEntriesScreen
import com.example.personalfinancetracker.presentation.pending.PendingEntriesScreen
import com.example.personalfinancetracker.presentation.list.ShoppingListScreen
import com.example.personalfinancetracker.presentation.list.ShoppingListsScreen
import com.example.personalfinancetracker.presentation.savings.SavingsScreen
import com.example.personalfinancetracker.presentation.transactions.AddTransactionScreen
import com.example.personalfinancetracker.presentation.transactions.HistoryScreen
import com.example.personalfinancetracker.presentation.settings.SettingsScreen
import com.example.personalfinancetracker.ui.theme.AppAppearance
import com.example.personalfinancetracker.ui.theme.AppThemeMode
import java.time.LocalTime

@Composable
fun FinanceApp(
    isDarkTheme: Boolean,
    moduleBarConfig: FloatingModuleBarConfig,
    initialType: TransactionType? = null,
    initialDestination: String? = null,
    initialEdit: Pair<Long, TransactionType>? = null,
    appearance: AppAppearance,
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onThemeChange: (AppThemeMode) -> Unit,
    onPrimaryChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onResetAppearance: () -> Unit,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onAutomaticCloseTimeChange: (LocalTime) -> Unit,
    onModuleBarVisibleRoutesChange: (Set<String>) -> Unit,
    onModuleBarShowLabelsChange: (Boolean) -> Unit,
    onModuleBarLabelTextSizeChange: (Float) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val systemBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showModuleBar = shouldShowModuleBar(currentRoute)

    fun navigateToModule(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = false
            popUpTo("home") { saveState = false }
        }
    }

    LaunchedEffect(initialType) {
        initialType?.let { navigateToModule("add/${it.name}") }
    }

    LaunchedEffect(initialDestination) {
        if (isValidInitialDestination(initialDestination)) {
            navigateToModule(initialDestination!!)
        }
    }

    LaunchedEffect(initialEdit) {
        initialEdit?.let { (id, type) -> navigateToModule("edit/${type.name}/$id") }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(
                bottom = systemBottomPadding + if (showModuleBar) 84.dp else 0.dp,
            ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable("home") {
                HomeScreen(
                    automaticCycleClose = automaticCycleClose,
                    automaticCloseTime = automaticCloseTime,
                    onAdd = { navigateToModule("add/${it.name}") },
                    onHistory = { navigateToModule("history") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable(
                route = "add/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                AddTransactionScreen(
                    onBack = { if (!navController.popBackStack()) navigateToModule("home") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable(
                route = "edit/{type}/{transactionId}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("transactionId") { type = NavType.LongType },
                ),
            ) {
                AddTransactionScreen(
                    onBack = { if (!navController.popBackStack()) navigateToModule("history") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable("history") {
                HistoryScreen(
                    onEdit = { id, type ->
                        navController.navigate("edit/${type.name}/$id")
                    },
                    onSettings = { navController.navigate("settings") },
                    onViewShoppingList = { navController.navigate("list/$it") },
                )
            }
            composable("statistics") {
                StatisticsScreen(onSettings = { navController.navigate("settings") })
            }
            composable("fixed") {
                FixedEntriesScreen(onSettings = { navController.navigate("settings") })
            }
            composable("pending") {
                PendingEntriesScreen(
                    onSettings = { navController.navigate("settings") },
                    onOpenList = { navController.navigate("list?query=${Uri.encode(it)}") },
                )
            }
            composable(
                route = "list?query={query}",
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                ShoppingListsScreen(
                    initialQuery = backStackEntry.arguments?.getString("query"),
                    onOpen = { navController.navigate("list/$it") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable(
                route = "list/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.LongType }),
            ) {
                ShoppingListScreen(
                    onBack = { if (!navController.popBackStack()) navigateToModule("list") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    appearance = appearance,
                    isDarkTheme = isDarkTheme,
                    moduleBarConfig = moduleBarConfig,
                    automaticCycleClose = automaticCycleClose,
                    automaticCloseTime = automaticCloseTime,
                    onBack = { navController.popBackStack() },
                    onThemeChange = onThemeChange,
                    onPrimaryChange = onPrimaryChange,
                    onAccentChange = onAccentChange,
                    onReset = onResetAppearance,
                    onAutomaticCycleCloseChange = onAutomaticCycleCloseChange,
                    onAutomaticCloseTimeChange = onAutomaticCloseTimeChange,
                    onModuleBarVisibleRoutesChange = onModuleBarVisibleRoutesChange,
                    onModuleBarShowLabelsChange = onModuleBarShowLabelsChange,
                    onModuleBarLabelTextSizeChange = onModuleBarLabelTextSizeChange,
                )
            }
            composable("savings") {
                SavingsScreen(
                    onSettings = { navController.navigate("settings") },
                )
            }
        }

        if (showModuleBar) {
            FloatingModuleBar(
                selectedRoute = currentRoute,
                config = moduleBarConfig,
                onNavigate = ::navigateToModule,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp)
                    .padding(bottom = systemBottomPadding + 8.dp),
            )
        }
    }
}

internal val topLevelRoutes = setOf("home", "history", "statistics", "fixed", "pending", "savings", "list")

internal fun shouldShowModuleBar(route: String?): Boolean = route in topLevelRoutes

internal fun isValidInitialDestination(route: String?): Boolean = route in topLevelRoutes || route == "settings"
