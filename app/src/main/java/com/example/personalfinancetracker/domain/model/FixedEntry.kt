package com.example.personalfinancetracker.domain.model

data class FixedEntry(
    val id: Long = 0,
    val type: TransactionType,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val comment: String?,
    val isActive: Boolean = true,
)
