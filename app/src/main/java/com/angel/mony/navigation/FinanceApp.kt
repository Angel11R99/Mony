package com.angel.mony.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.presentation.home.HomeScreen
import com.angel.mony.presentation.statistics.StatisticsScreen
import com.angel.mony.presentation.fixed.FixedEntriesScreen
import com.angel.mony.presentation.pending.PendingEntriesScreen
import com.angel.mony.presentation.list.ShoppingListScreen
import com.angel.mony.presentation.list.ShoppingListsScreen
import com.angel.mony.presentation.savings.SavingsScreen
import com.angel.mony.presentation.fortnight.FortnightScreen
import com.angel.mony.presentation.fortnight.FortnightTemplatesScreen
import com.angel.mony.presentation.transactions.AddTransactionScreen
import com.angel.mony.presentation.transactions.HistoryScreen
import com.angel.mony.presentation.settings.SettingsScreen
import com.angel.mony.presentation.settings.SettingsRoutes
import com.angel.mony.presentation.settings.AppearanceSettingsRoute
import com.angel.mony.presentation.settings.NavigationSettingsScreen
import com.angel.mony.presentation.settings.FinanceSettingsRoute
import com.angel.mony.presentation.settings.CategoriesSettingsScreen
import com.angel.mony.presentation.background.DecorativeBackground
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.BackgroundDecoration
import com.angel.mony.ui.theme.AppFontFamily
import com.angel.mony.ui.theme.AppShapeStyle
import com.angel.mony.ui.theme.AppThemeMode
import com.angel.mony.ui.iconography.IconColorMode
import com.angel.mony.ui.iconography.IconPack
import com.angel.mony.ui.iconography.MonyColorStyle
import java.time.LocalTime

@Composable
fun FinanceApp(
    isDarkTheme: Boolean,
    moduleBarConfig: FloatingModuleBarConfig,
    initialType: TransactionType? = null,
    initialDestination: String? = null,
    voiceOpeningId: String? = null,
    initialEdit: Pair<Long, TransactionType>? = null,
    appearance: AppAppearance,
    automaticCycleClose: Boolean,
    automaticCloseTime: LocalTime,
    onThemeChange: (AppThemeMode) -> Unit,
    onPrimaryChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onResetAppearance: () -> Unit,
    onShapeStyleChange: (AppShapeStyle) -> Unit,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onBackgroundDecorationChange: (BackgroundDecoration) -> Unit,
    onBackgroundIntensityChange: (Float) -> Unit,
 onIconPackChange: (IconPack) -> Unit,
 onIconColorModeChange: (IconColorMode) -> Unit,
 onCustomIconColorChange: (Int) -> Unit,
 onMonyColorStyleChange: (MonyColorStyle) -> Unit,
    onAutomaticCycleCloseChange: (Boolean) -> Unit,
    onAutomaticCloseTimeChange: (LocalTime) -> Unit,
    onModuleBarVisibleRoutesChange: (Set<String>) -> Unit,
    onModuleBarShowLabelsChange: (Boolean) -> Unit,
    onModuleBarLabelTextSizeChange: (Float) -> Unit,
    onModuleTransitionStyleChange: (ModuleTransitionStyle) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val systemBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showModuleBar = shouldShowModuleBar(currentRoute)

    fun navigateToModule(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo("home") { saveState = true }
        }
    }

    LaunchedEffect(initialType) {
        initialType?.let { navigateToModule("add/${it.name}") }
    }

    LaunchedEffect(initialDestination, voiceOpeningId) {
        if (isValidInitialDestination(initialDestination)) {
            navigateToModule(initialDestination!!)
        }
    }

    LaunchedEffect(initialEdit) {
        initialEdit?.let { (id, type) -> navigateToModule("edit/${type.name}/$id") }
    }

    Box(Modifier.fillMaxSize()) {
        DecorativeBackground(
            decoration = appearance.backgroundDecoration,
            intensity = appearance.backgroundIntensity,
        )
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(
                bottom = systemBottomPadding + if (showModuleBar) 84.dp else 0.dp,
            ),
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isAnimatedNavigationTransition(initialRoute, targetRoute)) {
                    moduleEnterTransition(
                        style = moduleBarConfig.transitionStyle,
                        forward = isForwardNavigationTransition(initialRoute, targetRoute),
                    )
                } else {
                    EnterTransition.None
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isAnimatedNavigationTransition(initialRoute, targetRoute)) {
                    moduleExitTransition(
                        style = moduleBarConfig.transitionStyle,
                        forward = isForwardNavigationTransition(initialRoute, targetRoute),
                    )
                } else {
                    ExitTransition.None
                }
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isAnimatedNavigationTransition(initialRoute, targetRoute)) {
                    moduleEnterTransition(
                        style = moduleBarConfig.transitionStyle,
                        forward = isForwardNavigationTransition(initialRoute, targetRoute),
                    )
                } else {
                    EnterTransition.None
                }
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (isAnimatedNavigationTransition(initialRoute, targetRoute)) {
                    moduleExitTransition(
                        style = moduleBarConfig.transitionStyle,
                        forward = isForwardNavigationTransition(initialRoute, targetRoute),
                    )
                } else {
                    ExitTransition.None
                }
            },
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
            composable("fortnight") {
                FortnightScreen(
                    onSettings = { navController.navigate("settings") },
                    onOpenTemplates = { navController.navigate("fortnight/templates") },
                )
            }
            composable("fortnight/templates") {
                FortnightTemplatesScreen(
                    onBack = { if (!navController.popBackStack()) navigateToModule("fortnight") },
                )
            }
            composable(SettingsRoutes.ROOT) {
                SettingsScreen(
                    appearance = appearance,
                    moduleBarConfig = moduleBarConfig,
                    onBack = { navController.popBackStack() },
                    onNavigate = navController::navigate,
                )
            }
            composable("voice") {
                AddTransactionScreen(
                    onBack = { if (!navController.popBackStack()) navigateToModule("home") },
                    onSettings = { navController.navigate("settings") },
                    voiceEntry = true,
                    voiceOpeningId = voiceOpeningId,
                )
            }
            composable(SettingsRoutes.PERSONALIZATION) {
                AppearanceSettingsRoute(
                    appearance = appearance,
                    isDarkTheme = isDarkTheme,
                    onBack = { navController.popBackStack() },
                    onThemeChange = onThemeChange,
                    onPrimaryChange = onPrimaryChange,
                    onAccentChange = onAccentChange,
                    onReset = onResetAppearance,
                    onShapeStyleChange = onShapeStyleChange,
                    onFontFamilyChange = onFontFamilyChange,
                    onBackgroundDecorationChange = onBackgroundDecorationChange,
                    onBackgroundIntensityChange = onBackgroundIntensityChange,
                    onIconPackChange = onIconPackChange,
                    onIconColorModeChange = onIconColorModeChange,
                    onCustomIconColorChange = onCustomIconColorChange,
                    onMonyColorStyleChange = onMonyColorStyleChange,
                )
            }
            composable(SettingsRoutes.NAVIGATION) {
                NavigationSettingsScreen(
                    moduleBarConfig = moduleBarConfig,
                    onBack = { navController.popBackStack() },
                    onModuleBarVisibleRoutesChange = onModuleBarVisibleRoutesChange,
                    onModuleBarShowLabelsChange = onModuleBarShowLabelsChange,
                    onModuleBarLabelTextSizeChange = onModuleBarLabelTextSizeChange,
                    onModuleTransitionStyleChange = onModuleTransitionStyleChange,
                )
            }
            composable(SettingsRoutes.FINANCE) {
                FinanceSettingsRoute(
                    automaticCycleClose = automaticCycleClose,
                    automaticCloseTime = automaticCloseTime,
                    onBack = { navController.popBackStack() },
                    onAutomaticCycleCloseChange = onAutomaticCycleCloseChange,
                    onAutomaticCloseTimeChange = onAutomaticCloseTimeChange,
                )
            }
            composable(SettingsRoutes.CATEGORIES) {
                CategoriesSettingsScreen(onBack = { navController.popBackStack() })
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

    internal val topLevelRoutes = setOf("home", "history", "statistics", "fixed", "pending", "savings", "list", "fortnight")

private const val ModuleEnterDurationMillis = 300
private const val ModuleExitDurationMillis = 240

internal fun isAnimatedNavigationTransition(initialRoute: String?, targetRoute: String?): Boolean {
    val initial = initialRoute?.substringBefore('?')
    val target = targetRoute?.substringBefore('?')
    val isModuleTransition = initial in topLevelRoutes && target in topLevelRoutes
    val isTransactionEditorTransition =
        (initial in topLevelRoutes && isTransactionEditorRoute(target)) ||
            (isTransactionEditorRoute(initial) && target in topLevelRoutes)
    val opensOrClosesSettings =
        (initial in topLevelRoutes && target == SettingsRoutes.ROOT) ||
            (initial == SettingsRoutes.ROOT && target in topLevelRoutes)
    val isSettingsTransition = isSettingsRoute(initial) && isSettingsRoute(target)
    return isModuleTransition || isTransactionEditorTransition ||
        opensOrClosesSettings || isSettingsTransition
}

private fun isSettingsRoute(route: String?): Boolean =
    route == SettingsRoutes.ROOT || route in SettingsRoutes.destinations

private fun isTransactionEditorRoute(route: String?): Boolean {
    val normalized = route?.substringBefore('?') ?: return false
    return normalized == "add/{type}" || normalized.startsWith("add/") ||
        normalized == "edit/{type}/{transactionId}" || normalized.startsWith("edit/")
}

internal fun isForwardNavigationTransition(initialRoute: String?, targetRoute: String?): Boolean {
    val initial = initialRoute?.substringBefore('?')
    val target = targetRoute?.substringBefore('?')
    return when {
        initial in topLevelRoutes && isTransactionEditorRoute(target) -> true
        isTransactionEditorRoute(initial) && target in topLevelRoutes -> false
        initial in topLevelRoutes && target == SettingsRoutes.ROOT -> true
        initial == SettingsRoutes.ROOT && target in topLevelRoutes -> false
        initial == SettingsRoutes.ROOT && target in SettingsRoutes.destinations -> true
        initial in SettingsRoutes.destinations && target == SettingsRoutes.ROOT -> false
        else -> isForwardModuleTransition(initial, target)
    }
}

internal fun isForwardModuleTransition(initialRoute: String?, targetRoute: String?): Boolean {
    val initialIndex = moduleDestinations.indexOfFirst { it.route == initialRoute?.substringBefore('?') }
    val targetIndex = moduleDestinations.indexOfFirst { it.route == targetRoute?.substringBefore('?') }
    return initialIndex < 0 || targetIndex < 0 || targetIndex >= initialIndex
}

internal fun moduleEnterTransition(
    style: ModuleTransitionStyle,
    forward: Boolean,
): EnterTransition = when (style) {
    ModuleTransitionStyle.NONE -> EnterTransition.None
    ModuleTransitionStyle.FADE -> fadeIn(
        animationSpec = tween(ModuleEnterDurationMillis, easing = FastOutSlowInEasing),
    )
    ModuleTransitionStyle.SLIDE -> slideInHorizontally(
        animationSpec = tween(ModuleEnterDurationMillis, easing = FastOutSlowInEasing),
        initialOffsetX = { width -> if (forward) width / 5 else -width / 5 },
    ) + fadeIn(
        animationSpec = tween(ModuleEnterDurationMillis, easing = FastOutSlowInEasing),
    )
    ModuleTransitionStyle.SCALE -> scaleIn(
        initialScale = 0.98f,
        animationSpec = tween(ModuleEnterDurationMillis, easing = FastOutSlowInEasing),
    ) + fadeIn(
        animationSpec = tween(ModuleEnterDurationMillis, easing = FastOutSlowInEasing),
    )
}

internal fun moduleExitTransition(
    style: ModuleTransitionStyle,
    forward: Boolean,
): ExitTransition = when (style) {
    ModuleTransitionStyle.NONE -> ExitTransition.None
    ModuleTransitionStyle.FADE -> fadeOut(
        animationSpec = tween(ModuleExitDurationMillis, easing = FastOutSlowInEasing),
    )
    ModuleTransitionStyle.SLIDE -> slideOutHorizontally(
        animationSpec = tween(ModuleExitDurationMillis, easing = FastOutSlowInEasing),
        targetOffsetX = { width -> if (forward) -width / 8 else width / 8 },
    ) + fadeOut(
        animationSpec = tween(ModuleExitDurationMillis, easing = FastOutSlowInEasing),
    )
    ModuleTransitionStyle.SCALE -> scaleOut(
        targetScale = 0.99f,
        animationSpec = tween(ModuleExitDurationMillis, easing = FastOutSlowInEasing),
    ) + fadeOut(
        animationSpec = tween(ModuleExitDurationMillis, easing = FastOutSlowInEasing),
    )
}

internal fun shouldShowModuleBar(route: String?): Boolean = route in topLevelRoutes

internal fun isValidInitialDestination(route: String?): Boolean =
    route in topLevelRoutes || route == "settings" || route == "voice"
