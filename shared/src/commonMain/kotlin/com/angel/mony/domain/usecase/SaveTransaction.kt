package com.angel.mony.domain.usecase

import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.repository.TransactionRepository

class SaveTransaction(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: FinanceTransaction): Long {
        require(transaction.amountInCents > 0) { "El monto debe ser mayor que cero" }
        val normalized = transaction.copy(description = transaction.description?.trim()?.takeIf(String::isNotEmpty))
        return if (normalized.id == 0L) repository.create(normalized) else {
            repository.update(normalized)
            normalized.id
        }
    }
}
