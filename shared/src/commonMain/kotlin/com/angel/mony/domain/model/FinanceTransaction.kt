package com.angel.mony.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class FinanceTransaction(
    val id: Long = 0,
    val amountInCents: Long,
    val type: TransactionType,
    val categoryId: Long,
    val description: String?,
    val date: LocalDate,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val fixedEntryId: Long? = null,
    val savingsGoalId: Long? = null,
)
