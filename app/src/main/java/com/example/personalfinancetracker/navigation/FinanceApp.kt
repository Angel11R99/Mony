package com.example.personalfinancetracker.navigation

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
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.home.HomeScreen
import com.example.personalfinancetracker.presentation.statistics.StatisticsScreen
import com.example.personalfinancetracker.presentation.fixed.FixedEntriesScreen
import com.example.personalfinancetracker.presentation.transactions.AddTransactionScreen
import com.example.personalfinancetracker.presentation.transactions.HistoryScreen

@Composable
fun FinanceApp(initialType: TransactionType? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val systemBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(bottom = systemBottomPadding + 84.dp),
        ) {
            composable("home") {
                HomeScreen(
                    onAdd = { navigateToModule("add/${it.name}") },
                    onHistory = { navigateToModule("history") },
                )
            }
            composable(
                route = "add/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                AddTransactionScreen(onBack = {
                    if (!navController.popBackStack()) navigateToModule("home")
                })
            }
            composable(
                route = "edit/{type}/{transactionId}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("transactionId") { type = NavType.LongType },
                ),
            ) {
                AddTransactionScreen(onBack = {
                    if (!navController.popBackStack()) navigateToModule("history")
                })
            }
            composable("history") {
                HistoryScreen(
                    onBack = {
                        if (!navController.popBackStack()) navigateToModule("home")
                    },
                    onEdit = { id, type ->
                        navController.navigate("edit/${type.name}/$id")
                    },
                )
            }
            composable("statistics") {
                StatisticsScreen()
            }
            composable("fixed") {
                FixedEntriesScreen()
            }
        }

        FloatingModuleBar(
            selectedRoute = currentRoute,
            onNavigate = ::navigateToModule,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp)
                .padding(bottom = systemBottomPadding + 8.dp),
        )
    }
}
