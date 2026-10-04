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

class EvaluateExpenseFunding(
    private val transactionRepository: TransactionRepository,
    private val expenseFundingRepository: ExpenseFundingRepository,
) {
    suspend operator fun invoke(
        expenseTransaction: FinanceTransaction,
        budget: BudgetConfig?,
        existingTransactionId: Long? = null,
    ): ExpenseFundingEvaluation {
        val period = activeBudgetPeriod(budget, expenseTransaction.date)
        return evaluate(expenseTransaction, period, existingTransactionId)
    }

    suspend operator fun invoke(
        expenseTransaction: FinanceTransaction,
        budget: BudgetConfig?,
        period: DateRange,
        existingTransactionId: Long? = null,
    ): ExpenseFundingEvaluation = evaluate(expenseTransaction, period, existingTransactionId)

    private suspend fun evaluate(
        expenseTransaction: FinanceTransaction,
        period: DateRange,
        existingTransactionId: Long?,
    ): ExpenseFundingEvaluation {
        val transactions = transactionRepository.observeByPeriod(period).first()
        val periodTransactions = transactions.filter { it.date in period.start..period.endInclusive }
        val incomeInCents = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FinanceTransaction::amountInCents)
        val previousFundingInCents = expenseFundingRepository.sumByPeriod(
            period.start.toEpochDays().toLong(),
            period.endInclusive.toEpochDays().toLong(),
        )
        val expensesInCents = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .filterNot { existingTransactionId != null && it.id == existingTransactionId }
            .sumOf(FinanceTransaction::amountInCents)

        return ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = incomeInCents + previousFundingInCents - expensesInCents,
            expenseAmountInCents = expenseTransaction.amountInCents,
            previousFundingInCents = previousFundingInCents,
        )
    }
}
