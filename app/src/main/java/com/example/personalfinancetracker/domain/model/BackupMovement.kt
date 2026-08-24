package com.example.personalfinancetracker.domain.model

import java.time.LocalDate

data class BackupMovement(
    val date: LocalDate,
    val type: TransactionType,
    val categoryName: String,
    val amountInCents: Long,
    val description: String?,
)
