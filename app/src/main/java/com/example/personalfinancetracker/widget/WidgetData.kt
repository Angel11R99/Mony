package com.example.personalfinancetracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import androidx.glance.unit.ColorProvider
import com.example.personalfinancetracker.MainActivity
import com.example.personalfinancetracker.core.CyclePreferences
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.availableForBudget
import com.example.personalfinancetracker.domain.model.belongsToActiveBudgetCycle
import com.example.personalfinancetracker.domain.model.budgetPeriodForView
import com.example.personalfinancetracker.ui.theme.AppearancePreferences
import com.example.personalfinancetracker.ui.theme.AppThemeMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

internal data class WidgetSnapshot(
    val budget: BudgetConfig?,
    val availableInCents: Long,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val transactionCount: Int,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
}

internal data class FinanceWidgetColors(
    val dark: Boolean,
    val primary: ColorProvider,
    val accent: ColorProvider,
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val track: ColorProvider,
)

internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    )
    val transactions = entryPoint.transactions().observeAll().first()
    val budget = entryPoint.budgets().observe().first()
    val pinnedView = CyclePreferences(context).pinnedBudgetView.value
    val period = budgetPeriodForView(budget, pinnedView)
    val periodTransactions = transactions.filter {
        it.belongsToActiveBudgetCycle(budget, period)
    }
    return WidgetSnapshot(
        budget = budget,
        availableInCents = availableForBudget(budget, transactions, period),
        incomeInCents = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FinanceTransaction::amountInCents),
        expenseInCents = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf(FinanceTransaction::amountInCents),
        transactionCount = periodTransactions.size,
    )
}

internal fun financeWidgetColors(context: Context): FinanceWidgetColors {
    val appearance = AppearancePreferences.load(context)
    val systemDark = context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val dark = when (appearance.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val primary = Color(appearance.primaryArgb)
    val accent = Color(appearance.accentArgb)
    return FinanceWidgetColors(
        dark = dark,
        primary = ColorProvider(primary),
        accent = ColorProvider(accent),
        primaryText = ColorProvider(if (dark) Color(0xFFF7F4EF) else Color(0xFF1C1722)),
        secondaryText = ColorProvider(if (dark) Color(0xFFD1CED3) else Color(0xFF6D6574)),
        track = ColorProvider(if (dark) Color(0xFF464148) else Color(0xFFE6E1E8)),
    )
}

internal fun openFinanceApp(context: Context) =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal suspend fun updateAllFinanceWidgets(context: Context) {
    FinanceWidget().updateAll(context)
    IncomeExpenseWidget().updateAll(context)
    BudgetProgressWidget().updateAll(context)
    StatisticsWidget().updateAll(context)
}
