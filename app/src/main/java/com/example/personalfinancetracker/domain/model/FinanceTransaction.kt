package com.example.personalfinancetracker.domain.model

import java.time.LocalDate
import java.time.Instant

data class FinanceTransaction(
    val id: Long = 0,
    val amountInCents: Long,
    val type: TransactionType,
    val categoryId: Long,
    val description: String?,
    val date: LocalDate,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
