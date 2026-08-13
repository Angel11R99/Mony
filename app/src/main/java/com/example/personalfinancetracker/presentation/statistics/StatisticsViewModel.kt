package com.example.personalfinancetracker.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class StatisticsUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
)

data class CategoryStatistic(
    val category: Category,
    val amountInCents: Long,
)

internal enum class StatisticsRange(val label: String) {
    CURRENT_MONTH("Este mes"),
    LAST_30_DAYS("30 días"),
    CURRENT_YEAR("Este año"),
    ALL_TIME("Todo"),
}

internal data class StatisticsPeriod(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

internal fun statisticsPeriod(
    range: StatisticsRange,
    today: LocalDate = LocalDate.now(),
): StatisticsPeriod = when (range) {
    StatisticsRange.CURRENT_MONTH -> StatisticsPeriod(
        startDate = YearMonth.from(today).atDay(1),
        endDate = today,
    )
    StatisticsRange.LAST_30_DAYS -> StatisticsPeriod(
        startDate = today.minusDays(29),
        endDate = today,
    )
    StatisticsRange.CURRENT_YEAR -> StatisticsPeriod(
        startDate = today.withDayOfYear(1),
        endDate = today,
    )
    StatisticsRange.ALL_TIME -> StatisticsPeriod(startDate = null, endDate = null)
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
) : ViewModel() {
    val state = combine(transactions.observeAll(), categories.observeAll()) { items, categoryList ->
        StatisticsUiState(items, categoryList.associateBy(Category::id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
}

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
