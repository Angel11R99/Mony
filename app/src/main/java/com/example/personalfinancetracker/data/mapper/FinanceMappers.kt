package com.example.personalfinancetracker.data.mapper

import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.data.local.entity.FixedEntryEntity
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.FixedDateMode
import com.example.personalfinancetracker.domain.model.FixedScheduleMode
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

fun FixedEntryEntity.toDomain() = FixedEntry(
    id = id,
    type = TransactionType.valueOf(type),
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    comment = comment,
    isActive = isActive,
    manualDateMode = FixedDateMode.valueOf(manualDateMode),
    manualSpecificDate = manualSpecificDateEpochDay?.let(LocalDate::ofEpochDay),
    scheduleMode = FixedScheduleMode.valueOf(scheduleMode),
    scheduleHour = scheduleHour,
    scheduleSpecificDate = scheduleSpecificDateEpochDay?.let(LocalDate::ofEpochDay),
    nextRunAt = nextRunAtEpochMillis?.let(Instant::ofEpochMilli),
    lastAddedAt = lastAddedAtEpochMillis?.let(Instant::ofEpochMilli),
    lastAddedDate = lastAddedDateEpochDay?.let(LocalDate::ofEpochDay),
)

fun FixedEntry.toEntity() = FixedEntryEntity(
    id = id,
    type = type.name,
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    comment = comment,
    isActive = isActive,
    manualDateMode = manualDateMode.name,
    manualSpecificDateEpochDay = manualSpecificDate?.toEpochDay(),
    scheduleMode = scheduleMode.name,
    scheduleHour = scheduleHour,
    scheduleSpecificDateEpochDay = scheduleSpecificDate?.toEpochDay(),
    nextRunAtEpochMillis = nextRunAt?.toEpochMilli(),
    lastAddedAtEpochMillis = lastAddedAt?.toEpochMilli(),
    lastAddedDateEpochDay = lastAddedDate?.toEpochDay(),
)
