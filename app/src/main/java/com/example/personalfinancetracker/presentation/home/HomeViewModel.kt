package com.example.personalfinancetracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    val state = combine(
        transactions.observeAll(),
        categories.observeAll(),
        budgetRepository.observe(),
    ) { all, categoryList, budget ->
        val period = DateRange.current(budget?.period ?: BudgetPeriod.FORTNIGHTLY)
        val current = all.filter { it.date in period.start..period.endInclusive }
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun saveBudget(amount: String, period: BudgetPeriod, onSaved: () -> Unit) {
        val amountInCents = MoneyFormatter.parseToCents(amount)
        if (amountInCents == null || amountInCents <= 0) return
        viewModelScope.launch {
            budgetRepository.save(BudgetConfig(amountInCents, period))
            onSaved()
        }
    }
}
