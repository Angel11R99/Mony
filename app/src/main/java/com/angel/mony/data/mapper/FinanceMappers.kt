package com.angel.mony.data.mapper

import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.ExpenseFundingEntity
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import com.angel.mony.data.local.entity.FortnightPlanEntity
import com.angel.mony.data.local.entity.FortnightPlanItemEntity
import com.angel.mony.data.local.entity.FortnightTemplateEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.data.local.entity.FixedEntryEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.dao.SavingsGoalWithSaved
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FortnightItemType
import com.angel.mony.domain.model.FortnightPayment
import com.angel.mony.domain.model.FortnightPlan
import com.angel.mony.domain.model.FortnightPlanItem
import com.angel.mony.domain.model.FortnightPlanStatus
import com.angel.mony.domain.model.FortnightSlot
import com.angel.mony.domain.model.FortnightTemplate
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
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.core.time.toKotlinInstant
import com.angel.mony.core.time.toKotlinLocalDate
import com.angel.mony.domain.model.BudgetPeriod
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.Instant as KotlinInstant
import kotlinx.datetime.LocalTime as KotlinLocalTime
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
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis).toKotlinInstant(),
        completedAt = completedAtEpochMillis?.let(Instant::ofEpochMilli)?.toKotlinInstant(),
    ),
    savedInCents = savedInCents,
)

fun TransactionEntity.toDomain() = FinanceTransaction(
    id = id,
    amountInCents = amountInCents,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    description = description,
    date = KotlinLocalDate.fromEpochDays(dateEpochDay.toInt()),
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
    fixedEntryId = fixedEntryId,
    savingsGoalId = savingsGoalId,
)

fun FinanceTransaction.toEntity() = TransactionEntity(
    id = id,
    amountInCents = amountInCents,
    type = type.name,
    categoryId = categoryId,
    description = description,
    dateEpochDay = date.toEpochDays().toLong(),
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
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
    manualSpecificDate = manualSpecificDateEpochDay?.let { KotlinLocalDate.fromEpochDays(it.toInt()) },
    scheduleMode = FixedScheduleMode.valueOf(scheduleMode),
    scheduleHour = scheduleHour,
    scheduleSpecificDate = scheduleSpecificDateEpochDay?.let { KotlinLocalDate.fromEpochDays(it.toInt()) },
    nextRunAt = nextRunAtEpochMillis?.let(KotlinInstant::fromEpochMilliseconds),
    lastAddedAt = lastAddedAtEpochMillis?.let(KotlinInstant::fromEpochMilliseconds),
    lastAddedDate = lastAddedDateEpochDay?.let { KotlinLocalDate.fromEpochDays(it.toInt()) },
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
    manualSpecificDateEpochDay = manualSpecificDate?.toEpochDays()?.toLong(),
    scheduleMode = scheduleMode.name,
    scheduleHour = scheduleHour,
    scheduleSpecificDateEpochDay = scheduleSpecificDate?.toEpochDays()?.toLong(),
    nextRunAtEpochMillis = nextRunAt?.toEpochMilliseconds(),
    lastAddedAtEpochMillis = lastAddedAt?.toEpochMilliseconds(),
    lastAddedDateEpochDay = lastAddedDate?.toEpochDays()?.toLong(),
)

fun PendingEntryEntity.toDomain() = PendingEntry(
    id = id,
    type = PendingType.valueOf(type),
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    date = KotlinLocalDate.fromEpochDays(dateEpochDay.toInt()),
    reminderTime = reminderMinutesOfDay?.let { KotlinLocalTime(it / 60, it % 60) },
    comment = comment,
    isDone = isDone,
    doneAt = doneAtEpochMillis?.let(KotlinInstant::fromEpochMilliseconds),
    transactionId = transactionId,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
    sourceShoppingListId = sourceShoppingListId,
)

fun PendingEntry.toEntity() = PendingEntryEntity(
    id = id,
    type = type.name,
    description = description,
    amountInCents = amountInCents,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDays().toLong(),
    reminderMinutesOfDay = reminderTime?.let { it.hour * 60 + it.minute },
    comment = comment,
    isDone = isDone,
    doneAtEpochMillis = doneAt?.toEpochMilliseconds(),
    transactionId = transactionId,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
    sourceShoppingListId = sourceShoppingListId,
)

fun ShoppingListEntity.toDomain() = ShoppingList(
    id = id,
    name = name,
    status = ShoppingListStatus.valueOf(status),
    budgetInCents = budgetInCents,
    expenseTransactionId = expenseTransactionId,
    payableId = payableId,
    purchaseDate = purchaseDateEpochDay?.let { KotlinLocalDate.fromEpochDays(it.toInt()) },
    paymentMethod = paymentMethod?.let(com.angel.mony.domain.model.ShoppingPaymentMethod::valueOf),
    expenseCategoryId = expenseCategoryId,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
    completedAt = completedAtEpochMillis?.let(KotlinInstant::fromEpochMilliseconds),
)

fun ShoppingList.toEntity() = ShoppingListEntity(
    id = id,
    name = name,
    status = status.name,
    budgetInCents = budgetInCents,
    expenseTransactionId = expenseTransactionId,
    payableId = payableId,
    purchaseDateEpochDay = purchaseDate?.toEpochDays()?.toLong(),
    paymentMethod = paymentMethod?.name,
    expenseCategoryId = expenseCategoryId,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
    completedAtEpochMillis = completedAt?.toEpochMilliseconds(),
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
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
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
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

fun ShoppingAdjustmentEntity.toDomain() = ShoppingAdjustment(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    isPositive = isPositive,
    amountInCents = amountInCents,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
)

fun ShoppingAdjustment.toEntity() = ShoppingAdjustmentEntity(
    id = id,
    shoppingListId = shoppingListId,
    name = name,
    isPositive = isPositive,
    amountInCents = amountInCents,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

fun KnownProductEntity.toDomain() = KnownProduct(
    barcode = barcode,
    name = name,
    lastPriceInCents = lastPriceInCents,
    lastUsedAt = KotlinInstant.fromEpochMilliseconds(lastUsedAtEpochMillis),
)

fun KnownProduct.toEntity() = KnownProductEntity(
    barcode = barcode,
    name = name,
    lastPriceInCents = lastPriceInCents,
    lastUsedAtEpochMillis = lastUsedAt.toEpochMilliseconds(),
)

fun FortnightTemplateEntity.toDomain() = FortnightTemplate(
    id = id,
    description = description,
    firstFortnightAmountInCents = firstFortnightAmountInCents,
    secondFortnightAmountInCents = secondFortnightAmountInCents,
    categoryId = categoryId,
    type = FortnightItemType.valueOf(type),
    note = note,
    isActive = isActive,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
)

fun FortnightTemplate.toEntity() = FortnightTemplateEntity(
    id = id,
    description = description,
    firstFortnightAmountInCents = firstFortnightAmountInCents,
    secondFortnightAmountInCents = secondFortnightAmountInCents,
    categoryId = categoryId,
    type = type.name,
    note = note,
    isActive = isActive,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

fun FortnightPlanEntity.toDomain() = FortnightPlan(
    id = id,
    period = DateRange(
        start = KotlinLocalDate.fromEpochDays(startDateEpochDay.toInt()),
        endInclusive = KotlinLocalDate.fromEpochDays(endDateEpochDay.toInt()),
    ),
    slot = FortnightSlot.valueOf(slot),
    budgetInCents = budgetInCents,
    status = FortnightPlanStatus.valueOf(status),
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    closedAt = closedAtEpochMillis?.let(KotlinInstant::fromEpochMilliseconds),
)

fun FortnightPlan.toEntity() = FortnightPlanEntity(
    id = id,
    startDateEpochDay = period.start.toEpochDays().toLong(),
    endDateEpochDay = period.endInclusive.toEpochDays().toLong(),
    slot = slot.name,
    budgetInCents = budgetInCents,
    status = status.name,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    closedAtEpochMillis = closedAt?.toEpochMilliseconds(),
)

fun FortnightPlanItemEntity.toDomain() = FortnightPlanItem(
    id = id,
    planId = planId,
    templateId = templateId,
    description = description,
    plannedAmountInCents = plannedAmountInCents,
    categoryId = categoryId,
    type = FortnightItemType.valueOf(type),
    savingsGoalId = savingsGoalId,
    note = note,
    position = position,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
)

fun FortnightPlanItem.toEntity() = FortnightPlanItemEntity(
    id = id,
    planId = planId,
    templateId = templateId,
    description = description,
    plannedAmountInCents = plannedAmountInCents,
    categoryId = categoryId,
    type = type.name,
    savingsGoalId = savingsGoalId,
    note = note,
    position = position,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

fun FortnightPaymentEntity.toDomain() = FortnightPayment(
    id = id,
    itemId = itemId,
    amountInCents = amountInCents,
    date = KotlinLocalDate.fromEpochDays(dateEpochDay.toInt()),
    transactionId = transactionId,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
)

fun FortnightPayment.toEntity() = FortnightPaymentEntity(
    id = id,
    itemId = itemId,
    amountInCents = amountInCents,
    dateEpochDay = date.toEpochDays().toLong(),
    transactionId = transactionId,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

fun ExpenseFundingEntity.toDomain() = com.angel.mony.domain.model.ExpenseFunding(
    id = id,
    transactionId = transactionId,
    amountInCents = amountInCents,
    sourceDescription = sourceDescription,
    createdAt = KotlinInstant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = KotlinInstant.fromEpochMilliseconds(updatedAtEpochMillis),
)

fun com.angel.mony.domain.model.ExpenseFunding.toEntity(dateEpochDay: Long) = ExpenseFundingEntity(
    id = id,
    transactionId = transactionId,
    amountInCents = amountInCents,
    sourceDescription = sourceDescription,
    dateEpochDay = dateEpochDay,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

fun BudgetConfigEntity.toDomain() = BudgetConfig(
    amountInCents = amountInCents,
    period = BudgetPeriod.valueOf(period),
    cycleStart = cycleStartEpochDay?.let(LocalDate::ofEpochDay)?.toKotlinLocalDate(),
    cycleStartedAt = cycleStartedAtEpochMillis?.let(Instant::ofEpochMilli)?.toKotlinInstant(),
    incomeTransactionId = incomeTransactionId,
    cycleSchedules = parseCycleSchedules(closingDays, BudgetPeriod.valueOf(period)),
)

private fun parseCycleSchedules(raw: String, period: BudgetPeriod): List<BudgetCycleSchedule> {
    val schedules = raw.split(',').mapNotNull { value ->
        val parts = value.split(':')
        if (parts.size != 2) return@mapNotNull null
        val openingDay = parts[0].toIntOrNull() ?: return@mapNotNull null
        val closingDay = parts[1].toIntOrNull() ?: return@mapNotNull null
        if (openingDay !in 1..31 || closingDay !in 1..31) return@mapNotNull null
        BudgetCycleSchedule(openingDay, closingDay)
    }.distinct()
    if (schedules.isNotEmpty()) return schedules

    val legacyOpeningDays = raw.split(',').mapNotNull(String::toIntOrNull)
        .filter { it in 1..31 }
        .distinct()
        .sorted()
    if (legacyOpeningDays.size < 2) return com.angel.mony.domain.model.defaultCycleSchedules(period)
    return legacyOpeningDays.mapIndexed { index, openingDay ->
        val nextOpeningDay = legacyOpeningDays[(index + 1) % legacyOpeningDays.size]
        BudgetCycleSchedule(
            openingDay = openingDay,
            closingDay = if (nextOpeningDay == 1) 31 else nextOpeningDay - 1,
        )
    }
}
