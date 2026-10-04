package com.angel.mony.domain.usecase

import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.repository.TransactionRepository

class SaveExpenseWithFunding(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult {
        return transactionRepository.createWithFunding(transaction, fundingSourceDescription)
    }

    suspend operator fun invoke(
        transaction: FinanceTransaction,
        existingTransactionId: Long,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult {
        return transactionRepository.updateWithFunding(transaction, existingTransactionId, fundingSourceDescription)
    }
}
