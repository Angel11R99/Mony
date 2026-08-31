package com.angel.mony.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.model.budgetPeriodForSchedule
import com.angel.mony.domain.model.previousBudgetPeriod
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StatisticsUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val budget: BudgetConfig? = null,
    val isReady: Boolean = false,
)

data class CategoryStatistic(
    val category: Category,
    val amountInCents: Long,
)

internal enum class StatisticsRange(val label: String) {
    CURRENT_BUDGET("Ciclo actual"),
    CURRENT_MONTH("Este mes"),
    CURRENT_YEAR("Este año"),
    ALL_TIME("Todo"),
    CUSTOM("Personalizado"),
}

internal data class StatisticsPeriod(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

internal fun statisticsPeriod(
    range: StatisticsRange,
    today: LocalDate = LocalDate.now(),
    budget: BudgetConfig? = null,
    customStart: LocalDate? = null,
    customEnd: LocalDate? = null,
): StatisticsPeriod = when (range) {
    StatisticsRange.CURRENT_BUDGET -> activeBudgetPeriod(budget, today).let {
        StatisticsPeriod(startDate = it.start, endDate = it.endInclusive)
    }
    StatisticsRange.CURRENT_MONTH -> StatisticsPeriod(
        startDate = YearMonth.from(today).atDay(1),
        endDate = YearMonth.from(today).atEndOfMonth(),
    )
    StatisticsRange.CURRENT_YEAR -> StatisticsPeriod(
        startDate = today.withDayOfYear(1),
        endDate = today,
    )
    StatisticsRange.ALL_TIME -> StatisticsPeriod(startDate = null, endDate = null)
    StatisticsRange.CUSTOM -> StatisticsPeriod(startDate = customStart, endDate = customEnd)
}

internal fun previousStatisticsPeriod(
    range: StatisticsRange,
    selectedCycle: BudgetCycleSchedule?,
    budget: BudgetConfig?,
    current: StatisticsPeriod,
    today: LocalDate = LocalDate.now(),
): StatisticsPeriod? = when {
    range == StatisticsRange.ALL_TIME || range == StatisticsRange.CUSTOM -> null
    selectedCycle != null -> budgetPeriodForSchedule(
        selectedCycle,
        (current.startDate ?: today).minusMonths(1),
    ).let { StatisticsPeriod(it.start, it.endInclusive) }
    range == StatisticsRange.CURRENT_BUDGET -> previousBudgetPeriod(budget, today).let {
        StatisticsPeriod(it.start, it.endInclusive)
    }
    range == StatisticsRange.CURRENT_MONTH -> YearMonth.from(today).minusMonths(1).let {
        StatisticsPeriod(it.atDay(1), it.atEndOfMonth())
    }
    else -> today.minusYears(1).let {
        StatisticsPeriod(it.withDayOfYear(1), it.withDayOfYear(it.lengthOfYear()))
    }
}

internal enum class TrendDirection { UP, DOWN, FLAT, NEW }

internal data class TrendDelta(val direction: TrendDirection, val percent: Int?)

internal fun trendDelta(current: Long, previous: Long): TrendDelta {
    if (previous <= 0L) {
        return if (current <= 0L) TrendDelta(TrendDirection.FLAT, null)
        else TrendDelta(TrendDirection.NEW, null)
    }
    val percent = ((current - previous) * 100 / previous).toInt()
    val direction = when {
        percent > 0 -> TrendDirection.UP
        percent < 0 -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }
    return TrendDelta(direction, percent)
}

internal fun statisticsPeriod(
    schedule: BudgetCycleSchedule,
    today: LocalDate = LocalDate.now(),
): StatisticsPeriod = budgetPeriodForSchedule(schedule, today).let {
    StatisticsPeriod(startDate = it.start, endDate = it.endInclusive)
}

data class StatisticsReport(
    val incomeInCents: Long,
    val expenseInCents: Long,
    val transactionCount: Int,
    val averageExpenseInCents: Long,
    val expenseByCategory: List<CategoryStatistic>,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
    val expenseRatio: Float get() =
        if (incomeInCents <= 0) 0f else (expenseInCents.toFloat() / incomeInCents).coerceAtLeast(0f)
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    transactions: TransactionRepository,
    categories: CategoryRepository,
    budget: BudgetRepository,
) : ViewModel() {
    val state = combine(
        transactions.observeAll(),
        categories.observeAll(),
        budget.observe(),
    ) { items, categoryList, budgetConfig ->
        StatisticsUiState(
            transactions = items,
            categories = categoryList.associateBy(Category::id),
            budget = budgetConfig,
            isReady = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
}

internal fun StatisticsRange.displayLabel(budget: BudgetConfig?): String =
    if (this != StatisticsRange.CURRENT_BUDGET) label
    else if (budget?.period == BudgetPeriod.MONTHLY) "Este ciclo mensual" else "Esta quincena"

private val customRangeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun customRangeLabel(start: LocalDate?, end: LocalDate?): String {
    val formattedStart = start?.format(customRangeFormatter)
    val formattedEnd = end?.format(customRangeFormatter)
    return when {
        formattedStart != null && formattedEnd != null -> "$formattedStart – $formattedEnd"
        formattedStart != null -> "Desde $formattedStart"
        formattedEnd != null -> "Hasta $formattedEnd"
        else -> "Personalizado"
    }
}

internal fun BudgetCycleSchedule.displayLabel(index: Int): String =
    "Ciclo ${index + 1} · días $openingDay-$closingDay"

internal fun calculateStatistics(
    transactions: List<FinanceTransaction>,
    categories: Map<Long, Category>,
    startDate: LocalDate?,
    endDate: LocalDate? = null,
): StatisticsReport {
    val filtered = transactions.filter {
        (startDate == null || !it.date.isBefore(startDate)) &&
            (endDate == null || !it.date.isAfter(endDate))
    }
    val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf(FinanceTransaction::amountInCents)
    val expenses = filtered.filter { it.type == TransactionType.EXPENSE }
    val expense = expenses.sumOf(FinanceTransaction::amountInCents)
    val breakdown = expenses
        .groupBy(FinanceTransaction::categoryId)
        .mapNotNull { (categoryId, items) ->
            categories[categoryId]?.let { category ->
                CategoryStatistic(category, items.sumOf(FinanceTransaction::amountInCents))
            }
        }
        .sortedByDescending(CategoryStatistic::amountInCents)
    return StatisticsReport(
        incomeInCents = income,
        expenseInCents = expense,
        transactionCount = filtered.size,
        averageExpenseInCents = if (expenses.isEmpty()) 0 else expense / expenses.size,
        expenseByCategory = breakdown,
    )
}
