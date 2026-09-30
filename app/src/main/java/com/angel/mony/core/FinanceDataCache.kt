package com.angel.mony.core

import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.FortnightPlanSummary
import com.angel.mony.domain.model.FortnightTemplate
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.ShoppingList
import com.angel.mony.domain.model.ShoppingListOverview
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.domain.repository.FortnightRepository
import com.angel.mony.domain.repository.PendingEntryRepository
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.repository.ShoppingListRepository
import com.angel.mony.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Caché reactiva de los datos compartidos por los módulos principales.
 *
 * Se crea durante el arranque, mantiene una sola colección de cada consulta Room y entrega el
 * último valor inmediatamente a los ViewModels. Room continúa siendo la fuente de verdad.
 */
@Singleton
class FinanceDataCache @Inject constructor(
    transactionsRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    fixedEntryRepository: FixedEntryRepository,
    pendingEntryRepository: PendingEntryRepository,
    savingsRepository: SavingsRepository,
    shoppingListRepository: ShoppingListRepository,
    fortnightRepository: FortnightRepository,
) {
    private data class LoadStatus(val complete: Boolean, val failure: Throwable?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadedSources = MutableStateFlow(emptySet<String>())
    private val loadFailure = MutableStateFlow<Throwable?>(null)

    private val mutableTransactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    val transactions: StateFlow<List<FinanceTransaction>> = mutableTransactions.asStateFlow()

    private val mutableCategories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = mutableCategories.asStateFlow()

    private val mutableBudget = MutableStateFlow<BudgetConfig?>(null)
    val budget: StateFlow<BudgetConfig?> = mutableBudget.asStateFlow()

    private val mutableBudgetHistory = MutableStateFlow<List<BudgetCycle>>(emptyList())
    val budgetHistory: StateFlow<List<BudgetCycle>> = mutableBudgetHistory.asStateFlow()

    private val mutableFixedEntries = MutableStateFlow<List<FixedEntry>>(emptyList())
    val fixedEntries: StateFlow<List<FixedEntry>> = mutableFixedEntries.asStateFlow()

    private val mutablePendingEntries = MutableStateFlow<List<PendingEntry>>(emptyList())
    val pendingEntries: StateFlow<List<PendingEntry>> = mutablePendingEntries.asStateFlow()

    private val mutableSavingsGoals = MutableStateFlow<List<SavingsGoalProgress>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoalProgress>> = mutableSavingsGoals.asStateFlow()

    private val mutableShoppingLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val shoppingLists: StateFlow<List<ShoppingList>> = mutableShoppingLists.asStateFlow()

    private val mutableShoppingOverviews = MutableStateFlow<List<ShoppingListOverview>>(emptyList())
    val shoppingOverviews: StateFlow<List<ShoppingListOverview>> = mutableShoppingOverviews.asStateFlow()

    private val mutableFortnightPlans = MutableStateFlow<List<FortnightPlanSummary>>(emptyList())
    val fortnightPlans: StateFlow<List<FortnightPlanSummary>> = mutableFortnightPlans.asStateFlow()

    private val mutableFortnightTemplates = MutableStateFlow<List<FortnightTemplate>>(emptyList())
    val fortnightTemplates: StateFlow<List<FortnightTemplate>> = mutableFortnightTemplates.asStateFlow()

    private val mutableActiveFortnightTemplates = MutableStateFlow<List<FortnightTemplate>>(emptyList())
    val activeFortnightTemplates: StateFlow<List<FortnightTemplate>> = mutableActiveFortnightTemplates.asStateFlow()

    init {
        cache(SOURCE_TRANSACTIONS, transactionsRepository.observeAll(), mutableTransactions)
        cache(SOURCE_CATEGORIES, categoryRepository.observeAll(), mutableCategories)
        cache(SOURCE_BUDGET, budgetRepository.observe(), mutableBudget)
        cache(SOURCE_BUDGET_HISTORY, budgetRepository.observeHistory(), mutableBudgetHistory)
        cache(SOURCE_FIXED, fixedEntryRepository.observeAll(), mutableFixedEntries)
        cache(SOURCE_PENDING, pendingEntryRepository.observeAll(), mutablePendingEntries)
        cache(SOURCE_SAVINGS, savingsRepository.observeGoals(), mutableSavingsGoals)
        cache(SOURCE_SHOPPING_LISTS, shoppingListRepository.observeLists(), mutableShoppingLists)
        cache(SOURCE_SHOPPING_OVERVIEWS, shoppingListRepository.observeListOverviews(), mutableShoppingOverviews)
        cache(SOURCE_FORTNIGHT_PLANS, fortnightRepository.observePlanSummaries(), mutableFortnightPlans)
        cache(SOURCE_FORTNIGHT_TEMPLATES, fortnightRepository.observeTemplates(), mutableFortnightTemplates)
        cache(SOURCE_ACTIVE_FORTNIGHT_TEMPLATES, fortnightRepository.observeActiveTemplates(), mutableActiveFortnightTemplates)
    }

    suspend fun awaitInitialLoad() {
        val status = combine(loadedSources, loadFailure) { loaded, error ->
            LoadStatus(complete = loaded.containsAll(REQUIRED_SOURCES), failure = error)
        }.first { it.complete || it.failure != null }
        status.failure?.let { throw it }
    }

    fun retryFailedLoads() {
        loadFailure.value = null
    }

    private fun <T> cache(key: String, source: Flow<T>, destination: MutableStateFlow<T>) {
        scope.launch {
            source
                .retryWhen { error, _ ->
                    loadFailure.value = error
                    delay(RETRY_DELAY_MILLIS)
                    true
                }
                .collect { value ->
                    destination.value = value
                    loadedSources.update { it + key }
                    if (loadedSources.value.containsAll(REQUIRED_SOURCES)) loadFailure.value = null
                }
        }
    }

    private companion object {
        const val RETRY_DELAY_MILLIS = 500L
        const val SOURCE_TRANSACTIONS = "transactions"
        const val SOURCE_CATEGORIES = "categories"
        const val SOURCE_BUDGET = "budget"
        const val SOURCE_BUDGET_HISTORY = "budget_history"
        const val SOURCE_FIXED = "fixed"
        const val SOURCE_PENDING = "pending"
        const val SOURCE_SAVINGS = "savings"
        const val SOURCE_SHOPPING_LISTS = "shopping_lists"
        const val SOURCE_SHOPPING_OVERVIEWS = "shopping_overviews"
        const val SOURCE_FORTNIGHT_PLANS = "fortnight_plans"
        const val SOURCE_FORTNIGHT_TEMPLATES = "fortnight_templates"
        const val SOURCE_ACTIVE_FORTNIGHT_TEMPLATES = "active_fortnight_templates"
        val REQUIRED_SOURCES = setOf(
            SOURCE_TRANSACTIONS,
            SOURCE_CATEGORIES,
            SOURCE_BUDGET,
            SOURCE_BUDGET_HISTORY,
            SOURCE_FIXED,
            SOURCE_PENDING,
            SOURCE_SAVINGS,
            SOURCE_SHOPPING_LISTS,
            SOURCE_SHOPPING_OVERVIEWS,
            SOURCE_FORTNIGHT_PLANS,
            SOURCE_FORTNIGHT_TEMPLATES,
            SOURCE_ACTIVE_FORTNIGHT_TEMPLATES,
        )
    }
}
