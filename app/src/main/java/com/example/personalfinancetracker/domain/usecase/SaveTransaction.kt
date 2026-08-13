package com.example.personalfinancetracker.domain.usecase

import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class SaveTransaction @Inject constructor(
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
