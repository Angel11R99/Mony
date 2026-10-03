package com.angel.mony.domain.usecase

import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.ExpenseFundingEvaluation
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.repository.ExpenseFundingRepository
import com.angel.mony.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EvaluateExpenseFunding @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val expenseFundingRepository: ExpenseFundingRepository,
) {
    suspend operator fun invoke(
        expenseTransaction: FinanceTransaction,
        budget: BudgetConfig?,
        existingTransactionId: Long? = null,
    ): ExpenseFundingEvaluation {
        val period = activeBudgetPeriod(budget, expenseTransaction.date)
        return evaluate(expenseTransaction, budget, period, existingTransactionId)
    }

    suspend operator fun invoke(
        expenseTransaction: FinanceTransaction,
        budget: BudgetConfig?,
        period: DateRange,
        existingTransactionId: Long? = null,
    ): ExpenseFundingEvaluation = evaluate(expenseTransaction, budget, period, existingTransactionId)

    private suspend fun evaluate(
        expenseTransaction: FinanceTransaction,
        budget: BudgetConfig?,
        period: DateRange,
        existingTransactionId: Long?,
    ): ExpenseFundingEvaluation {
        val transactions: List<FinanceTransaction> = transactionRepository.observeByPeriod(period).first()

        // Filter transactions in the period
        val periodTransactions = transactions.filter { it.date in period.start..period.endInclusive }

        // Calculate income in the period
        val incomeInCents = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FinanceTransaction::amountInCents)

        // Calculate previous funding in the period
        val previousFundingInCents = expenseFundingRepository.sumByPeriod(
            period.start.toEpochDays().toLong(),
            period.endInclusive.toEpochDays().toLong(),
        )

        // Calculate expenses in the period (excluding the existing transaction if editing)
        val expensesInCents = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .filterNot { existingTransactionId != null && it.id == existingTransactionId }
            .sumOf(FinanceTransaction::amountInCents)

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents

        return ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = availableBeforeExpense,
            expenseAmountInCents = expenseTransaction.amountInCents,
            previousFundingInCents = previousFundingInCents,
        )
    }
}
