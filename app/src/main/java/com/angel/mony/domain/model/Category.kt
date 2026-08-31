package com.angel.mony.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val icon: String,
    val isActive: Boolean,
    val budgetLimitInCents: Long? = null,
)
