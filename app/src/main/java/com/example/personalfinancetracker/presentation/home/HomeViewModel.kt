package com.example.personalfinancetracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.core.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate

data class CategorySpending(val category: Category, val amountInCents: Long)

data class HomeUiState(
    val availableInCents: Long = 0,
    val periodIncomeInCents: Long = 0,
    val periodExpenseInCents: Long = 0,
    val recent: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val spending: List<CategorySpending> = emptyList(),
    val period: DateRange = DateRange.currentFortnight(),
    val budget: BudgetConfig? = null,
    val cycleHistory: List<BudgetCycle> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    val closingCycle = MutableStateFlow(false)

    val state = combine(
        transactions.observeAll(),
        categories.observeAll(),
        budgetRepository.observe(),
        budgetRepository.observeHistory(),
    ) { all, categoryList, budget, history ->
        val period = activeBudgetPeriod(budget)
        val current = all.filter { it.belongsToActiveBudgetCycle(budget, period) }
        val byId = categoryList.associateBy(Category::id)
        val income = current.filter { it.type == TransactionType.INCOME }.sumOf { it.amountInCents }
        val expense = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountInCents }
        HomeUiState(
            availableInCents = all.sumOf { if (it.type == TransactionType.INCOME) it.amountInCents else -it.amountInCents },
            periodIncomeInCents = income,
            periodExpenseInCents = expense,
            recent = all.take(5),
            categories = byId,
            spending = current.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapNotNull { (id, items) -> byId[id]?.let { CategorySpending(it, items.sumOf(FinanceTransaction::amountInCents)) } }
                .sortedByDescending(CategorySpending::amountInCents),
            period = period,
            budget = budget,
            cycleHistory = history,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun saveBudget(amount: String, period: BudgetPeriod, onSaved: () -> Unit) {
        val amountInCents = MoneyFormatter.parseToCents(amount)
        if (amountInCents == null || amountInCents <= 0) return
        viewModelScope.launch {
            val existing = state.value.budget
            budgetRepository.save(
                BudgetConfig(
                    amountInCents = amountInCents,
                    period = period,
                    cycleStart = existing?.cycleStart,
                    cycleStartedAt = existing?.cycleStartedAt,
                )
            )
            onSaved()
        }
    }

    fun closeCurrentCycle(onClosed: () -> Unit) {
        val current = state.value
        val budget = current.budget ?: return
        if (closingCycle.value) return
        viewModelScope.launch {
            closingCycle.value = true
            val now = Instant.now()
            val today = LocalDate.now()
            val closedCycle = BudgetCycle(
                period = budget.period,
                budgetAmountInCents = budget.amountInCents,
                incomeInCents = current.periodIncomeInCents,
                expenseInCents = current.periodExpenseInCents,
                startDate = current.period.start,
                endDate = today.coerceAtLeast(current.period.start),
                closedAt = now,
            )
            val nextConfig = budget.copy(cycleStart = today, cycleStartedAt = now)
            runCatching { budgetRepository.closeCycle(closedCycle, nextConfig) }
                .onSuccess { onClosed() }
            closingCycle.value = false
        }
    }
}

internal fun activeBudgetPeriod(
    budget: BudgetConfig?,
    today: LocalDate = LocalDate.now(),
): DateRange {
    val calendarPeriod = DateRange.current(budget?.period ?: BudgetPeriod.FORTNIGHTLY, today)
    val activeCycleStart = budget?.cycleStart?.takeIf {
        it in calendarPeriod.start..calendarPeriod.endInclusive
    }
    return DateRange(activeCycleStart ?: calendarPeriod.start, calendarPeriod.endInclusive)
}

internal fun FinanceTransaction.belongsToActiveBudgetCycle(
    budget: BudgetConfig?,
    period: DateRange,
): Boolean {
    if (date !in period.start..period.endInclusive) return false
    val boundary = budget?.cycleStartedAt?.takeIf { budget.cycleStart == period.start }
    return boundary == null || !createdAt.isBefore(boundary)
}
