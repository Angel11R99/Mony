package com.example.personalfinancetracker.domain.model

import java.time.Instant
import java.time.LocalDate

enum class PendingType {
    PAYMENT,
    COLLECTION,
}

fun PendingType.toTransactionType(): TransactionType = when (this) {
    PendingType.PAYMENT -> TransactionType.EXPENSE
    PendingType.COLLECTION -> TransactionType.INCOME
}

fun PendingType.label(): String = when (this) {
    PendingType.PAYMENT -> "Pago"
    PendingType.COLLECTION -> "Cobro"
}

enum class PendingPeriodFilter {
    FORTNIGHT,
    MONTH,
    ALL,
}

data class PendingEntry(
    val id: Long = 0,
    val type: PendingType,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val date: LocalDate,
    val comment: String? = null,
    val isDone: Boolean = false,
    val doneAt: Instant? = null,
    val transactionId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)