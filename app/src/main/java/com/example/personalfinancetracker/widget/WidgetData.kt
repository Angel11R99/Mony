package com.example.personalfinancetracker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.example.personalfinancetracker.MainActivity
import com.example.personalfinancetracker.core.CyclePreferences
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.availableForBudget
import com.example.personalfinancetracker.domain.model.belongsToActiveBudgetCycle
import com.example.personalfinancetracker.domain.model.budgetPeriodForView
import com.example.personalfinancetracker.domain.model.previousBudgetPeriod
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.FixedEntryRepository
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
import com.example.personalfinancetracker.domain.repository.SavingsRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Snapshot models (pure data; derivations live in WidgetFormatting.kt / here)
// ---------------------------------------------------------------------------

data class WidgetCoreSnapshot(
    val budget: BudgetConfig?,
    val period: DateRange,
    val today: LocalDate,
    val availableInCents: Long,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val transactionCount: Int,
    val latestExpense: FinanceTransaction?,
    val recent: List<MovementLine>,
    val topExpenseCategories: List<CategorySlice>,
    val previousCycleExpenseInCents: Long?,
    val todayExpenseInCents: Long = 0L,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents

    /** Remaining daily allowance; null when no budget is configured. */
    val dailyAllowanceInCents: Long?
        get() = dailyAllowanceInCents(
            budgetInCents = budget?.amountInCents,
            spentInCycleInCents = expenseInCents,
            daysLeft = cycleDaysLeft(period, today),
        )

    companion object {
        fun empty(today: LocalDate = LocalDate.now()) = WidgetCoreSnapshot(
            budget = null,
            period = DateRange.currentFortnight(today),
            today = today,
            availableInCents = 0L,
            incomeInCents = 0L,
            expenseInCents = 0L,
            transactionCount = 0,
            latestExpense = null,
            recent = emptyList(),
            topExpenseCategories = emptyList(),
            previousCycleExpenseInCents = null,
        )
    }
}

data class MovementLine(
    val id: Long,
    val categoryName: String?,
    val description: String?,
    val amountInCents: Long,
    val isIncome: Boolean,
    val date: LocalDate,
)

data class CategorySlice(
    val name: String,
    val amountInCents: Long,
    /** 0f..1f share of total cycle expenses. */
    val fraction: Float,
    /** Configured category spending limit; null when the category has none. */
    val limitInCents: Long? = null,
)

data class PendingWidgetSnapshot(
    val items: List<PendingLine>,
    val toPayInCents: Long,
    val toCollectInCents: Long,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        fun empty() = PendingWidgetSnapshot(emptyList(), 0L, 0L)
    }
}

data class PendingLine(
    val id: Long,
    val type: PendingType,
    val description: String,
    val amountInCents: Long,
    val date: LocalDate,
)

data class SavingsWidgetSnapshot(
    val goals: List<SavingsGoalProgress>,
) {
    val isEmpty: Boolean get() = goals.isEmpty()
    val totalSavedInCents: Long get() = goals.sumOf(SavingsGoalProgress::savedInCents)
    val totalTargetInCents: Long get() = goals.sumOf { it.goal.targetAmountInCents }

    companion object {
        fun empty() = SavingsWidgetSnapshot(emptyList())
    }
}

data class FixedWidgetSnapshot(
    val activeCount: Int,
    val expenseTotalInCents: Long,
    val incomeTotalInCents: Long,
    val nextRunAt: Instant?,
    val topEntries: List<FixedLine>,
) {
    val isEmpty: Boolean get() = activeCount == 0

    companion object {
        fun empty() = FixedWidgetSnapshot(0, 0L, 0L, null, emptyList())
    }
}

data class FixedLine(
    val description: String,
    val amountInCents: Long,
    val isIncome: Boolean,
)

// ---------------------------------------------------------------------------
// Loaders — each widget loads only the data it displays.
// ---------------------------------------------------------------------------

internal suspend fun loadCoreSnapshot(context: Context): WidgetCoreSnapshot {
    val entryPoint = entryPoint(context)
    val transactions = entryPoint.transactions().observeAll().first()
    val categoriesById = loadCategories(entryPoint)
    val budget = entryPoint.budgets().observe().first()
    val pinnedView = CyclePreferences(context).pinnedBudgetView.value
    val period = budgetPeriodForView(budget, pinnedView)
    val today = LocalDate.now()
    val periodTransactions = transactions.filter { it.belongsToActiveBudgetCycle(budget, period) }

    val previousCycleExpense = budget?.let { config ->
        val previousPeriod = previousBudgetPeriod(config, today)
        transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.belongsToActiveBudgetCycle(config, previousPeriod)
            }
            .sumOf(FinanceTransaction::amountInCents)
    }

    val totalExpenses = periodTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf(FinanceTransaction::amountInCents)

    val expensesByCategory = periodTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy(FinanceTransaction::categoryId)
        .mapNotNull { (categoryId, items) ->
            val category = categoriesById[categoryId] ?: return@mapNotNull null
            CategorySlice(
                name = category.name,
                amountInCents = items.sumOf(FinanceTransaction::amountInCents),
                fraction = 0f,
                limitInCents = category.budgetLimitInCents?.takeIf { it > 0L },
            )
        }
        .sortedByDescending(CategorySlice::amountInCents)

    return WidgetCoreSnapshot(
        budget = budget,
        period = period,
        today = today,
        availableInCents = availableForBudget(budget, transactions, period),
        incomeInCents = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FinanceTransaction::amountInCents),
        expenseInCents = totalExpenses,
        transactionCount = periodTransactions.size,
        latestExpense = transactions.firstOrNull {
            it.type == TransactionType.EXPENSE && it.belongsToActiveBudgetCycle(budget, period)
        },
        recent = transactions.take(RECENT_MOVEMENTS_LIMIT).map { it.toMovementLine(categoriesById) },
        topExpenseCategories = expensesByCategory.take(TOP_CATEGORIES_LIMIT).map { slice ->
            slice.copy(
                fraction = if (totalExpenses > 0) slice.amountInCents.toFloat() / totalExpenses else 0f,
            )
        },
        previousCycleExpenseInCents = previousCycleExpense,
        todayExpenseInCents = transactions
            .filter {
                it.type == TransactionType.EXPENSE && it.date == today
            }
            .sumOf(FinanceTransaction::amountInCents),
    )
}

private fun FinanceTransaction.toMovementLine(categoriesById: Map<Long, Category>) = MovementLine(
    id = id,
    categoryName = categoriesById[categoryId]?.name,
    description = description?.takeIf { it.isNotBlank() },
    amountInCents = amountInCents,
    isIncome = type == TransactionType.INCOME,
    date = date,
)

internal suspend fun loadPendingSnapshot(context: Context): PendingWidgetSnapshot {
    val entries = entryPoint(context).pendingEntries().observeAll().first().filterNot(PendingEntry::isDone)
    if (entries.isEmpty()) return PendingWidgetSnapshot.empty()
    return PendingWidgetSnapshot(
        items = entries.take(PENDING_ITEMS_LIMIT).map { entry ->
            PendingLine(
                id = entry.id,
                type = entry.type,
                description = entry.description,
                amountInCents = entry.amountInCents,
                date = entry.date,
            )
        },
        toPayInCents = entries
            .filter { it.type == PendingType.PAYMENT }
            .sumOf(PendingEntry::amountInCents),
        toCollectInCents = entries
            .filter { it.type == PendingType.COLLECTION }
            .sumOf(PendingEntry::amountInCents),
    )
}

internal suspend fun loadSavingsSnapshot(context: Context): SavingsWidgetSnapshot =
    SavingsWidgetSnapshot(sortSavingsGoals(entryPoint(context).savings().observeGoals().first()))

internal suspend fun loadFixedSnapshot(context: Context): FixedWidgetSnapshot {
    val now = Instant.now()
    val active = entryPoint(context).fixedEntries().observeAll().first().filter(FixedEntry::isActive)
    if (active.isEmpty()) return FixedWidgetSnapshot.empty()
    return FixedWidgetSnapshot(
        activeCount = active.size,
        expenseTotalInCents = active
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf(FixedEntry::amountInCents),
        incomeTotalInCents = active
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FixedEntry::amountInCents),
        nextRunAt = active.mapNotNull(FixedEntry::nextRunAt).filter { it.isAfter(now) }.minOrNull(),
        topEntries = active
            .sortedByDescending(FixedEntry::amountInCents)
            .take(FIXED_TOP_LIMIT)
            .map { FixedLine(it.description, it.amountInCents, it.type == TransactionType.INCOME) },
    )
}

private suspend fun loadCategories(entryPoint: WidgetEntryPoint): Map<Long, Category> =
    entryPoint.categories().observeAll().first().associateBy(Category::id)

internal fun entryPoint(context: Context): WidgetEntryPoint = EntryPointAccessors.fromApplication(
    context.applicationContext,
    WidgetEntryPoint::class.java,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactions(): TransactionRepository
    fun budgets(): BudgetRepository
    fun categories(): CategoryRepository
    fun pendingEntries(): PendingEntryRepository
    fun savings(): SavingsRepository
    fun fixedEntries(): FixedEntryRepository
}

// ---------------------------------------------------------------------------
// Navigation helpers
// ---------------------------------------------------------------------------

internal fun openFinanceApp(context: Context) = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}

internal fun openDestinationIntent(context: Context, destination: String) =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_DESTINATION, destination)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal fun addTransactionIntent(context: Context, type: TransactionType) =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_TRANSACTION_TYPE, type.name)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal fun editTransactionIntent(context: Context, transactionId: Long, isIncome: Boolean) =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_EDIT_TRANSACTION_ID, transactionId)
        putExtra(
            MainActivity.EXTRA_TRANSACTION_TYPE,
            (if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE).name,
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

// ---------------------------------------------------------------------------
// Central refresh
// ---------------------------------------------------------------------------

suspend fun updateAllFinanceWidgets(context: Context) {
    FinanceWidget().updateAll(context)
    IncomeExpenseWidget().updateAll(context)
    BudgetProgressWidget().updateAll(context)
    StatisticsWidget().updateAll(context)
    RecentMovementsWidget().updateAll(context)
    PendingRemindersWidget().updateAll(context)
    SavingsGoalsWidget().updateAll(context)
    FixedCommitmentsWidget().updateAll(context)
    QuickAccessWidget().updateAll(context)
    DailySpendingWidget().updateAll(context)
    CategoryLimitsWidget().updateAll(context)
}

private const val RECENT_MOVEMENTS_LIMIT = 6
private const val TOP_CATEGORIES_LIMIT = 4
private const val PENDING_ITEMS_LIMIT = 5
private const val FIXED_TOP_LIMIT = 3
