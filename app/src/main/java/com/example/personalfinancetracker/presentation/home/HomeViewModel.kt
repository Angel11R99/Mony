package com.example.personalfinancetracker.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.BudgetPeriodView
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.activeBudgetPeriod
import com.example.personalfinancetracker.domain.model.belongsToActiveBudgetCycle
import com.example.personalfinancetracker.domain.model.budgetPeriodForView
import com.example.personalfinancetracker.domain.model.defaultCycleSchedules
import com.example.personalfinancetracker.domain.model.nextBudgetPeriod
import com.example.personalfinancetracker.domain.model.budgetPeriodToClose
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.core.CyclePreferences
import com.example.personalfinancetracker.core.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.Duration
import java.time.ZonedDateTime
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets

data class CategorySpending(val category: Category, val amountInCents: Long)

data class HomeUiState(
    val periodIncomeInCents: Long = 0,
    val periodExpenseInCents: Long = 0,
    val recent: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val spending: List<CategorySpending> = emptyList(),
    val period: DateRange = DateRange.currentFortnight(),
    val currentPeriod: DateRange = DateRange.currentFortnight(),
    val nextPeriod: DateRange = DateRange.currentFortnight(),
    val selectedPeriodView: BudgetPeriodView = BudgetPeriodView.CURRENT,
    val pinnedPeriodView: BudgetPeriodView = BudgetPeriodView.CURRENT,
    val budget: BudgetConfig? = null,
    val cycleHistory: List<BudgetCycle> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val closingCycle = MutableStateFlow(false)
    private val budgetIncomeMutex = Mutex()
    private val cyclePreferences = CyclePreferences(context)
    private val selectedPeriodView = MutableStateFlow(cyclePreferences.pinnedBudgetView.value)
    private val currentDate = flow {
        while (true) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate())
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000))
        }
    }.distinctUntilChanged()
    private val periodViewState = combine(
        cyclePreferences.pinnedBudgetView,
        selectedPeriodView,
        currentDate,
    ) { pinned, selected, today -> Triple(pinned, selected, today) }

    val state = combine(
        transactions.observeAll(),
        categories.observeAll(),
        budgetRepository.observe(),
        budgetRepository.observeHistory(),
        periodViewState,
    ) { all, categoryList, budget, history, (pinnedView, selectedView, today) ->
        val currentPeriod = budgetPeriodForView(budget, BudgetPeriodView.CURRENT, today)
        val nextPeriod = budgetPeriodForView(budget, BudgetPeriodView.NEXT, today)
        val period = if (selectedView == BudgetPeriodView.CURRENT) currentPeriod else nextPeriod
        val periodTransactions = all.filter { it.belongsToActiveBudgetCycle(budget, period) }
        val byId = categoryList.associateBy(Category::id)
        val income = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountInCents }
        val expense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountInCents }
        HomeUiState(
            periodIncomeInCents = income,
            periodExpenseInCents = expense,
            recent = periodTransactions.take(5),
            categories = byId,
            spending = periodTransactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapNotNull { (id, items) -> byId[id]?.let { CategorySpending(it, items.sumOf(FinanceTransaction::amountInCents)) } }
                .sortedByDescending(CategorySpending::amountInCents),
            period = period,
            currentPeriod = currentPeriod,
            nextPeriod = nextPeriod,
            selectedPeriodView = selectedView,
            pinnedPeriodView = pinnedView,
            budget = budget,
            cycleHistory = history,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectPeriodView(view: BudgetPeriodView) {
        selectedPeriodView.value = view
    }

    fun pinPeriodView(view: BudgetPeriodView) {
        selectedPeriodView.value = view
        cyclePreferences.setPinnedBudgetView(view)
        context.showToast("Vista fijada correctamente")
        viewModelScope.launch { updateAllFinanceWidgets(context) }
    }

    init {
        viewModelScope.launch {
            budgetRepository.observe().first { it != null }
            budgetIncomeMutex.withLock {
                val budget = budgetRepository.observe().first() ?: return@withLock
                val linkedIncome = budget.incomeTransactionId?.let { transactions.get(it) }
                if (linkedIncome == null) {
                    val categoryId = incomeCategoryId() ?: return@withLock
                    val transactionId = upsertBudgetIncome(
                        amountInCents = budget.amountInCents,
                        period = budget.period,
                        categoryId = categoryId,
                        existingId = null,
                        date = budget.cycleStart ?: activeBudgetPeriod(budget).start,
                        now = Instant.now(),
                    )
                    budgetRepository.save(budget.copy(incomeTransactionId = transactionId))
                    updateAllFinanceWidgets(context)
                }
            }
        }
    }

    fun saveBudget(
        amount: String,
        period: BudgetPeriod,
        onSaved: () -> Unit,
    ) {
        val amountInCents = MoneyFormatter.parseToCents(amount)
        if (amountInCents == null || amountInCents <= 0) return
        viewModelScope.launch {
            budgetIncomeMutex.withLock {
                val existing = budgetRepository.observe().first()
                val categoryId = incomeCategoryId() ?: return@withLock
                val now = Instant.now()
                val incomeTransactionId = upsertBudgetIncome(
                    amountInCents = amountInCents,
                    period = period,
                    categoryId = categoryId,
                    existingId = existing?.incomeTransactionId,
                    date = LocalDate.now(),
                    now = now,
                )
                budgetRepository.save(
                    BudgetConfig(
                        amountInCents = amountInCents,
                        period = period,
                        cycleStart = existing?.cycleStart,
                        cycleStartedAt = existing?.cycleStartedAt,
                        incomeTransactionId = incomeTransactionId,
                        cycleSchedules = existing?.cycleSchedules ?: defaultCycleSchedules(period),
                    )
                )
            }
            context.showToast("Presupuesto guardado correctamente")
            onSaved()
            viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
        }
    }

    fun closeCurrentCycle(onClosed: () -> Unit) {
        val budget = state.value.budget ?: return
        if (closingCycle.value) return
        val today = LocalDate.now()
        val periodToClose = budgetPeriodToClose(budget, today)
        if (periodToClose.endInclusive != today) {
            context.showToast("Este ciclo todavía no ha llegado a su día de cierre")
            return
        }
        viewModelScope.launch {
            closingCycle.value = true
            val now = Instant.now()
            val nextStart = nextBudgetPeriod(budget, today).start
            val cycleTransactions = transactions.observeAll().first().filter {
                it.belongsToActiveBudgetCycle(budget, periodToClose)
            }
            val closedCycle = BudgetCycle(
                period = budget.period,
                budgetAmountInCents = budget.amountInCents,
                incomeInCents = cycleTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(FinanceTransaction::amountInCents),
                expenseInCents = cycleTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(FinanceTransaction::amountInCents),
                startDate = periodToClose.start,
                endDate = periodToClose.endInclusive,
                closedAt = now,
            )
            runCatching {
                budgetIncomeMutex.withLock {
                    val categoryId = incomeCategoryId() ?: error("No hay categoría de ingreso disponible")
                    val nextIncomeId = upsertBudgetIncome(
                        amountInCents = budget.amountInCents,
                        period = budget.period,
                        categoryId = categoryId,
                        existingId = null,
                        date = nextStart,
                        now = now,
                    )
                    val nextConfig = budget.copy(
                        cycleStart = nextStart,
                        cycleStartedAt = now,
                        incomeTransactionId = nextIncomeId,
                    )
                    budgetRepository.closeCycle(closedCycle, nextConfig)
                }
            }
                .onSuccess {
                    context.showToast("Ciclo cerrado correctamente")
                    onClosed()
                    viewModelScope.launch { runCatching { updateAllFinanceWidgets(context) } }
                }
                .onFailure {
                    context.showToast("No se pudo cerrar el ciclo")
                }
            closingCycle.value = false
        }
    }

    private suspend fun incomeCategoryId(): Long? {
        val incomeCategories = categories.observeActive(TransactionType.INCOME).first()
        return incomeCategories.firstOrNull { it.name.equals("Salario", ignoreCase = true) }?.id
            ?: incomeCategories.firstOrNull()?.id
    }

    private suspend fun upsertBudgetIncome(
        amountInCents: Long,
        period: BudgetPeriod,
        categoryId: Long,
        existingId: Long?,
        date: LocalDate,
        now: Instant,
    ): Long {
        val existing = existingId?.let { transactions.get(it) }
        val transaction = FinanceTransaction(
            id = existing?.id ?: 0,
            amountInCents = amountInCents,
            type = TransactionType.INCOME,
            categoryId = existing?.categoryId ?: categoryId,
            description = budgetIncomeDescription(period),
            date = existing?.date ?: date,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return if (existing == null) transactions.create(transaction) else {
            transactions.update(transaction)
            transaction.id
        }
    }
}

internal fun budgetIncomeDescription(period: BudgetPeriod): String =
    if (period == BudgetPeriod.MONTHLY) "Ingreso mensual" else "Ingreso quincenal"
