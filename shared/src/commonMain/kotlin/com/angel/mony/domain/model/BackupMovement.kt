package com.angel.mony.domain.model

import kotlinx.datetime.LocalDate

data class BackupMovement(
    val date: LocalDate,
    val type: TransactionType,
    val categoryName: String,
    val amountInCents: Long,
    val description: String?,
)
