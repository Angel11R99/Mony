package com.angel.mony.domain.usecase

import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.ExpenseFunding
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.ExpenseFundingRepository
import com.angel.mony.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DomainUseCasesTest {
    @Test
    fun saveTransactionNormalizesDescriptionBeforeCreating() = runBlocking {
        val transactions = FakeTransactionRepository(createId = 41L)
        val transaction = transaction(amount = 500L, description = "  Mercado  ")

        val id = SaveTransaction(transactions)(transaction)

        assertEquals(41L, id)
        assertEquals("Mercado", transactions.created?.description)
    }

    @Test
    fun saveTransactionRejectsNonPositiveAmounts() = runBlocking {
        val transactions = FakeTransactionRepository()

        assertFailsWith<IllegalArgumentException> {
            SaveTransaction(transactions)(transaction(amount = 0L))
        }
        assertNull(transactions.created)
    }

    @Test
    fun evaluateExpenseFundingExcludesEditedExpense() = runBlocking {
        val period = DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
        val transactions = FakeTransactionRepository(
            periodTransactions = listOf(
                transaction(id = 1L, amount = 1_000L, type = TransactionType.INCOME),
                transaction(id = 2L, amount = 300L),
                transaction(id = 3L, amount = 200L),
                transaction(id = 4L, amount = 9_000L, date = LocalDate(2026, 9, 30)),
            )
        )
        val funding = FakeExpenseFundingRepository(periodSum = 50L)

        val result = EvaluateExpenseFunding(transactions, funding)(
            expenseTransaction = transaction(id = 3L, amount = 400L),
            budget = null,
            period = period,
            existingTransactionId = 3L,
        )

        assertEquals(750L, result.availableBeforeExpenseInCents)
        assertEquals(400L, result.expenseAmountInCents)
        assertEquals(50L, result.previousFundingInCents)
    }

    @Test
    fun saveExpenseWithFundingDelegatesCreateAndUpdate() = runBlocking {
        val saved = ExpenseCreationResult.Saved(transactionId = 8L, funding = null)
        val transactions = FakeTransactionRepository(fundingResult = saved)
        val useCase = SaveExpenseWithFunding(transactions)
        val expense = transaction(amount = 600L)

        assertEquals(saved, useCase(expense, "Ahorros"))
        assertEquals(saved, useCase(expense.copy(id = 8L), 8L, "Ahorros"))
        assertEquals("Ahorros", transactions.createFundingDescription)
        assertEquals(8L, transactions.updatedFundingId)
    }

    @Test
    fun saveBudgetCreatesIncomeAndInitializesCycle() = runBlocking {
        val budgets = FakeBudgetRepository()
        val categories = FakeCategoryRepository(
            listOf(Category(7L, "Salario", TransactionType.INCOME, "payments", true))
        )
        val transactions = FakeTransactionRepository(createId = 42L)
        val now = Instant.parse("2026-10-04T15:30:00Z")

        SaveBudget(
            budgets = budgets,
            categories = categories,
            transactions = transactions,
            currentInstant = { now },
            currentTimeZone = { TimeZone.UTC },
        )(250_000L, BudgetPeriod.MONTHLY)

        val income = requireNotNull(transactions.created)
        val budget = requireNotNull(budgets.saved)
        assertEquals(250_000L, income.amountInCents)
        assertEquals(7L, income.categoryId)
        assertEquals("Ingreso mensual", income.description)
        assertEquals(now, income.createdAt)
        assertEquals(budget.cycleStart, income.date)
        assertEquals(now, budget.cycleStartedAt)
        assertEquals(42L, budget.incomeTransactionId)
    }

    private fun transaction(
        id: Long = 0L,
        amount: Long,
        type: TransactionType = TransactionType.EXPENSE,
        description: String? = null,
        date: LocalDate = LocalDate(2026, 10, 4),
    ) = FinanceTransaction(
        id = id,
        amountInCents = amount,
        type = type,
        categoryId = 1L,
        description = description,
        date = date,
    )
}

private class FakeTransactionRepository(
    private val createId: Long = 1L,
    private val periodTransactions: List<FinanceTransaction> = emptyList(),
    private val fundingResult: ExpenseCreationResult = ExpenseCreationResult.Saved(1L, null),
) : TransactionRepository {
    var created: FinanceTransaction? = null
    var updated: FinanceTransaction? = null
    var createFundingDescription: String? = null
    var updatedFundingId: Long? = null

    override fun observeAll(): Flow<List<FinanceTransaction>> = flowOf(periodTransactions)
    override fun observeByPeriod(period: DateRange): Flow<List<FinanceTransaction>> = flowOf(periodTransactions)
    override fun observeBySavingsGoal(goalId: Long): Flow<List<FinanceTransaction>> = flowOf(emptyList())
    override suspend fun getRecent(limit: Int): List<FinanceTransaction> = periodTransactions.take(limit)
    override suspend fun get(id: Long): FinanceTransaction? = periodTransactions.firstOrNull { it.id == id }

    override suspend fun create(transaction: FinanceTransaction): Long {
        created = transaction
        return createId
    }

    override suspend fun update(transaction: FinanceTransaction) {
        updated = transaction
    }

    override suspend fun delete(id: Long) = Unit
    override suspend fun duplicate(id: Long): Long? = null

    override suspend fun createWithFunding(
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult {
        createFundingDescription = fundingSourceDescription
        return fundingResult
    }

    override suspend fun updateWithFunding(
        transaction: FinanceTransaction,
        existingTransactionId: Long,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult {
        updatedFundingId = existingTransactionId
        return fundingResult
    }

    override suspend fun restoreBackup(movements: List<BackupMovement>): Int = 0
}

private class FakeBudgetRepository : BudgetRepository {
    var current: BudgetConfig? = null
    var saved: BudgetConfig? = null

    override fun observe(): Flow<BudgetConfig?> = flowOf(current)
    override fun observeHistory(): Flow<List<BudgetCycle>> = flowOf(emptyList())

    override suspend fun save(config: BudgetConfig) {
        saved = config
    }

    override suspend fun closeCycle(cycle: BudgetCycle, nextConfig: BudgetConfig) = Unit
}

private class FakeCategoryRepository(
    private val categories: List<Category>,
) : CategoryRepository {
    override fun observeActive(type: TransactionType): Flow<List<Category>> =
        flowOf(categories.filter { it.type == type && it.isActive })

    override fun observeAll(): Flow<List<Category>> = flowOf(categories)
    override fun observeUsedCategoryIds(): Flow<Set<Long>> = flowOf(emptySet())
    override suspend fun create(name: String, type: TransactionType, budgetLimitInCents: Long?) = Unit
    override suspend fun update(id: Long, name: String, budgetLimitInCents: Long?) = Unit
    override suspend fun setActive(id: Long, isActive: Boolean) = Unit
    override suspend fun deleteIfUnused(id: Long): Boolean = true
}

private class FakeExpenseFundingRepository(
    private val periodSum: Long,
) : ExpenseFundingRepository {
    override fun observeAll(): Flow<List<ExpenseFunding>> = flowOf(emptyList())
    override fun observeByTransaction(transactionId: Long): Flow<ExpenseFunding?> = flowOf(null)
    override fun observeByPeriod(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseFunding>> = flowOf(emptyList())
    override suspend fun getByTransaction(transactionId: Long): ExpenseFunding? = null
    override suspend fun create(funding: ExpenseFunding): Long = 1L
    override suspend fun update(funding: ExpenseFunding) = Unit
    override suspend fun deleteByTransaction(transactionId: Long) = Unit
    override suspend fun sumByPeriod(startEpochDay: Long, endEpochDay: Long): Long = periodSum
}
