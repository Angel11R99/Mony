package com.angel.mony.data.mapper

import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.data.local.entity.FixedEntryEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.dao.SavingsGoalWithSaved
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.SavingsGoal
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.FixedDateMode
import com.angel.mony.domain.model.FixedScheduleMode
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.PendingType
import com.angel.mony.domain.model.KnownProduct
import com.angel.mony.domain.model.ShoppingAdjustment
import com.angel.mony.domain.model.ShoppingList
import com.angel.mony.domain.model.ShoppingListItem
import com.angel.mony.domain.model.ShoppingListStatus
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
    sourceShoppingListId = sourceShoppingListId,
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
    sourceShoppingListId = sourceShoppingListId,
)

fun ShoppingListEntity.toDomain() = ShoppingList(
    id = id,
    name = name,
    status = ShoppingListStatus.valueOf(status),
    budgetInCents = budgetInCents,
    expenseTransactionId = expenseTransactionId,
    payableId = payableId,
    purchaseDate = purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
    paymentMethod = paymentMethod?.let(com.angel.mony.domain.model.ShoppingPaymentMethod::valueOf),
    expenseCategoryId = expenseCategoryId,
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
    payableId = payableId,
    purchaseDateEpochDay = purchaseDate?.toEpochDay(),
    paymentMethod = paymentMethod?.name,
    expenseCategoryId = expenseCategoryId,
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
