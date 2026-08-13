package com.example.personalfinancetracker.data.mapper

import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

fun CategoryEntity.toDomain() = Category(id, name, TransactionType.valueOf(type), icon, isActive)

fun TransactionEntity.toDomain() = FinanceTransaction(
    id = id,
    amountInCents = amountInCents,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    description = description,
    date = LocalDate.ofEpochDay(dateEpochDay),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

fun FinanceTransaction.toEntity() = TransactionEntity(
    id = id,
    amountInCents = amountInCents,
    type = type.name,
    categoryId = categoryId,
    description = description,
    dateEpochDay = date.toEpochDay(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)
