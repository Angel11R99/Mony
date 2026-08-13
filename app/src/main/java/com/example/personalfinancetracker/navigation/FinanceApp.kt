package com.example.personalfinancetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.presentation.home.HomeScreen
import com.example.personalfinancetracker.presentation.transactions.AddTransactionScreen
import com.example.personalfinancetracker.presentation.transactions.HistoryScreen

@Composable
fun FinanceApp(initialType: TransactionType? = null) {
    val navController = rememberNavController()
    val startDestination = initialType?.let { "add/${it.name}" } ?: "home"
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                onAdd = { navController.navigate("add/${it.name}") },
                onHistory = { navController.navigate("history") },
            )
        }
        composable(
            route = "add/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { AddTransactionScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = "edit/{type}/{transactionId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("transactionId") { type = NavType.LongType },
            ),
        ) { AddTransactionScreen(onBack = { navController.popBackStack() }) }
        composable("history") {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id, type ->
                    navController.navigate("edit/${type.name}/$id")
                },
            )
        }
    }
}
