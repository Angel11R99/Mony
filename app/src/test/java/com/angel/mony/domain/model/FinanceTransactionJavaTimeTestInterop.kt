package com.angel.mony.domain.model

import com.angel.mony.core.time.toKotlinInstant
import com.angel.mony.core.time.toKotlinLocalDate
import java.time.Instant
import java.time.LocalDate

@Suppress("FunctionName")
fun FinanceTransaction(
    id: Long = 0,
    amountInCents: Long,
    type: TransactionType,
    categoryId: Long,
    description: String?,
    date: LocalDate,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    fixedEntryId: Long? = null,
    savingsGoalId: Long? = null,
): FinanceTransaction = FinanceTransaction(
    id = id,
    amountInCents = amountInCents,
    type = type,
    categoryId = categoryId,
    description = description,
    date = date.toKotlinLocalDate(),
    createdAt = createdAt.toKotlinInstant(),
    updatedAt = updatedAt.toKotlinInstant(),
    fixedEntryId = fixedEntryId,
    savingsGoalId = savingsGoalId,
)
