package com.angel.mony.domain.model

import java.time.Instant

data class ExpenseFunding(
    val id: Long = 0,
    val transactionId: Long,
    val amountInCents: Long,
    val sourceDescription: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

sealed interface ExpenseCreationResult {
    data class Saved(
        val transactionId: Long,
        val funding: ExpenseFunding?,
    ) : ExpenseCreationResult

    data class RequiresFundingSource(
        val overflowInCents: Long,
        val availableBeforeExpenseInCents: Long,
        val expenseAmountInCents: Long,
        val transaction: FinanceTransaction,
    ) : ExpenseCreationResult

    data class Error(
        val message: String,
    ) : ExpenseCreationResult
}

data class ExpenseFundingEvaluation(
    val availableBeforeExpenseInCents: Long,
    val expenseAmountInCents: Long,
    val previousFundingInCents: Long,
) {
    val remainingAfterExpense: Long
        get() = availableBeforeExpenseInCents - expenseAmountInCents

    val overflowInCents: Long
        get() = if (remainingAfterExpense < 0) -remainingAfterExpense else 0L

    val requiresFundingSource: Boolean
        get() = overflowInCents > 0
}