package com.example.personalfinancetracker.data.mapper

import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.data.local.entity.FixedEntryEntity
import com.example.personalfinancetracker.data.local.entity.PendingEntryEntity
import com.example.personalfinancetracker.data.local.entity.KnownProductEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingAdjustmentEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingListEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingListItemEntity
import com.example.personalfinancetracker.data.local.dao.SavingsGoalWithSaved
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.SavingsGoal
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.FixedEntry
import com.example.personalfinancetracker.domain.model.FixedDateMode
import com.example.personalfinancetracker.domain.model.FixedScheduleMode
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.model.PendingType
import com.example.personalfinancetracker.domain.model.KnownProduct
import com.example.personalfinancetracker.domain.model.ShoppingAdjustment
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListItem
import com.example.personalfinancetracker.domain.model.ShoppingListStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun CategoryEntity.toDomain() =
    Category(id, name, TransactionType.valueOf(type), icon, isActive, budgetLimitInCents)

fun SavingsGoalWithSaved.toDomain() = SavingsGoalProgress(
    goal = SavingsGoal(
        id = id,
        name = name,
        targetAmountInCents = targetAmountInCents,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        completedAt = completedAtEpochMillis?.let(Instant::ofEpochMilli),
    ),
    savedInCents = savedInCents,
)

fun TransactionEntity.toDomain() = FinanceTransaction(
    id = id,
    amountInCents = amountInCents,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    description = description,
    date = LocalDate.ofEpochDay(dateEpochDay),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    fixedEntryId = fixedEntryId,
    savingsGoalId = savingsGoalId,
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
    fixedEntryId = fixedEntryId,
    savingsGoalId = savingsGoalId,
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

fun PendingEntryEntity.toDomain() = PendingEntry(
    id = id,
    type = PendingType.valueOf(type),
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    reminderTime = reminderMinutesOfDay?.let { LocalTime.of(it / 60, it % 60) },
    comment = comment,
    isDone = isDone,
    doneAt = doneAtEpochMillis?.let(Instant::ofEpochMilli),
    transactionId = transactionId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

fun PendingEntry.toEntity() = PendingEntryEntity(
    id = id,
    type = type.name,
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    reminderMinutesOfDay = reminderTime?.let { it.hour * 60 + it.minute },
    comment = comment,
    isDone = isDone,
    doneAtEpochMillis = doneAt?.toEpochMilli(),
    transactionId = transactionId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

fun ShoppingListEntity.toDomain() = ShoppingList(
    id = id,
    name = name,
    status = ShoppingListStatus.valueOf(status),
    budgetInCents = budgetInCents,
    expenseTransactionId = expenseTransactionId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    completedAt = completedAtEpochMillis?.let(Instant::ofEpochMilli),
)

fun ShoppingList.toEntity() = ShoppingListEntity(
    id = id,
    name = name,
    status = status.name,
    budgetInCents = budgetInCents,
    expenseTransactionId = expenseTransactionId,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    completedAtEpochMillis = completedAt?.toEpochMilli(),
)

fun ShoppingListItemEntity.toDomain() = ShoppingListItem(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    quantity = quantity,
    estimatedUnitPriceInCents = estimatedUnitPriceInCents,
    actualUnitPriceInCents = actualUnitPriceInCents,
    barcode = barcode,
    isPurchased = isPurchased,
    isIdentified = isIdentified,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

fun ShoppingListItem.toEntity() = ShoppingListItemEntity(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    quantity = quantity,
    estimatedUnitPriceInCents = estimatedUnitPriceInCents,
    actualUnitPriceInCents = actualUnitPriceInCents,
    barcode = barcode,
    isPurchased = isPurchased,
    isIdentified = isIdentified,
    notes = notes,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

fun ShoppingAdjustmentEntity.toDomain() = ShoppingAdjustment(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    isPositive = isPositive,
    amountInCents = amountInCents,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

fun ShoppingAdjustment.toEntity() = ShoppingAdjustmentEntity(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    isPositive = isPositive,
    amountInCents = amountInCents,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

fun KnownProductEntity.toDomain() = KnownProduct(
    barcode = barcode,
    name = name,
    lastPriceInCents = lastPriceInCents,
    lastUsedAt = Instant.ofEpochMilli(lastUsedAtEpochMillis),
)

fun KnownProduct.toEntity() = KnownProductEntity(
    barcode = barcode,
    name = name,
    lastPriceInCents = lastPriceInCents,
    lastUsedAtEpochMillis = lastUsedAt.toEpochMilli(),
)
