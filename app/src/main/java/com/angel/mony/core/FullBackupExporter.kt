package com.angel.mony.core

import com.angel.mony.core.backup.FullBackupJson
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.ExpenseFundingEntity
import com.angel.mony.data.local.entity.FixedEntryEntity
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import com.angel.mony.data.local.entity.FortnightPlanEntity
import com.angel.mony.data.local.entity.FortnightPlanItemEntity
import com.angel.mony.data.local.entity.FortnightTemplateEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.ProductRecognitionAliasEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.domain.model.BackupBudgetConfig
import com.angel.mony.domain.model.BackupBudgetCycle
import com.angel.mony.domain.model.BackupCategory
import com.angel.mony.domain.model.BackupExpenseFunding
import com.angel.mony.domain.model.BackupFixedEntry
import com.angel.mony.domain.model.BackupFortnightItem
import com.angel.mony.domain.model.BackupFortnightPayment
import com.angel.mony.domain.model.BackupFortnightPlan
import com.angel.mony.domain.model.BackupFortnightTemplate
import com.angel.mony.domain.model.BackupKnownProduct
import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.BackupPendingEntry
import com.angel.mony.domain.model.BackupProductAlias
import com.angel.mony.domain.model.BackupSavingsGoal
import com.angel.mony.domain.model.BackupShoppingAdjustment
import com.angel.mony.domain.model.BackupShoppingItem
import com.angel.mony.domain.model.BackupShoppingList
import com.angel.mony.domain.model.BackupTransaction
import com.angel.mony.domain.model.FullBackupSnapshot as PortableBackupSnapshot

data class FullBackupSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val fixedEntries: List<FixedEntryEntity>,
    val pendingEntries: List<PendingEntryEntity>,
    val budgetConfig: BudgetConfigEntity?,
    val budgetCycles: List<BudgetCycleEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val shoppingLists: List<ShoppingListEntity>,
    val shoppingItems: List<ShoppingListItemEntity>,
    val shoppingAdjustments: List<ShoppingAdjustmentEntity>,
    val knownProducts: List<KnownProductEntity> = emptyList(),
    val productAliases: List<ProductRecognitionAliasEntity> = emptyList(),
    val fortnightTemplates: List<FortnightTemplateEntity> = emptyList(),
    val fortnightPlans: List<FortnightPlanEntity> = emptyList(),
    val fortnightItems: List<FortnightPlanItemEntity> = emptyList(),
    val fortnightPayments: List<FortnightPaymentEntity> = emptyList(),
    val expenseFunding: List<ExpenseFundingEntity> = emptyList(),
)

sealed class ParsedBackup {
    data class Full(val snapshot: FullBackupSnapshot) : ParsedBackup()
    data class LegacyCsv(val movements: List<BackupMovement>) : ParsedBackup()
}

object FullBackupExporter {
    const val CURRENT_VERSION = FullBackupJson.CURRENT_VERSION

    fun isJsonBackup(content: String): Boolean = FullBackupJson.isJsonBackup(content)

    fun buildFullBackupJson(snapshot: FullBackupSnapshot): String =
        FullBackupJson.encode(snapshot.toPortable())

    fun parseBackup(content: String): ParsedBackup {
        val stripped = content.removePrefix(CsvExporter.UTF8_BOM).trim()
        if (stripped.isEmpty()) error("El archivo está vacío")
        return if (isJsonBackup(stripped)) {
            ParsedBackup.Full(parseJsonBackup(stripped))
        } else {
            ParsedBackup.LegacyCsv(CsvExporter.parseBackup(stripped))
        }
    }

    fun parseJsonBackup(content: String): FullBackupSnapshot =
        FullBackupJson.decode(content).toRoomSnapshot()
}

private fun FullBackupSnapshot.toPortable() = PortableBackupSnapshot(
    categories = categories.map(CategoryEntity::toBackup),
    transactions = transactions.map(TransactionEntity::toBackup),
    fixedEntries = fixedEntries.map(FixedEntryEntity::toBackup),
    pendingEntries = pendingEntries.map(PendingEntryEntity::toBackup),
    budgetConfig = budgetConfig?.toBackup(),
    budgetCycles = budgetCycles.map(BudgetCycleEntity::toBackup),
    savingsGoals = savingsGoals.map(SavingsGoalEntity::toBackup),
    shoppingLists = shoppingLists.map(ShoppingListEntity::toBackup),
    shoppingItems = shoppingItems.map(ShoppingListItemEntity::toBackup),
    shoppingAdjustments = shoppingAdjustments.map(ShoppingAdjustmentEntity::toBackup),
    knownProducts = knownProducts.map(KnownProductEntity::toBackup),
    productAliases = productAliases.map(ProductRecognitionAliasEntity::toBackup),
    fortnightTemplates = fortnightTemplates.map(FortnightTemplateEntity::toBackup),
    fortnightPlans = fortnightPlans.map(FortnightPlanEntity::toBackup),
    fortnightItems = fortnightItems.map(FortnightPlanItemEntity::toBackup),
    fortnightPayments = fortnightPayments.map(FortnightPaymentEntity::toBackup),
    expenseFunding = expenseFunding.map(ExpenseFundingEntity::toBackup),
)

private fun PortableBackupSnapshot.toRoomSnapshot() = FullBackupSnapshot(
    categories = categories.map(BackupCategory::toEntity),
    transactions = transactions.map(BackupTransaction::toEntity),
    fixedEntries = fixedEntries.map(BackupFixedEntry::toEntity),
    pendingEntries = pendingEntries.map(BackupPendingEntry::toEntity),
    budgetConfig = budgetConfig?.toEntity(),
    budgetCycles = budgetCycles.map(BackupBudgetCycle::toEntity),
    savingsGoals = savingsGoals.map(BackupSavingsGoal::toEntity),
    shoppingLists = shoppingLists.map(BackupShoppingList::toEntity),
    shoppingItems = shoppingItems.map(BackupShoppingItem::toEntity),
    shoppingAdjustments = shoppingAdjustments.map(BackupShoppingAdjustment::toEntity),
    knownProducts = knownProducts.map(BackupKnownProduct::toEntity),
    productAliases = productAliases.map(BackupProductAlias::toEntity),
    fortnightTemplates = fortnightTemplates.map(BackupFortnightTemplate::toEntity),
    fortnightPlans = fortnightPlans.map(BackupFortnightPlan::toEntity),
    fortnightItems = fortnightItems.map(BackupFortnightItem::toEntity),
    fortnightPayments = fortnightPayments.map(BackupFortnightPayment::toEntity),
    expenseFunding = expenseFunding.map(BackupExpenseFunding::toEntity),
)

private fun CategoryEntity.toBackup() = BackupCategory(
    id, name, type, icon, isActive, createdAtEpochMillis, budgetLimitInCents,
)

private fun BackupCategory.toEntity() = CategoryEntity(
    id, name, type, icon, isActive, createdAtEpochMillis, budgetLimitInCents,
)

private fun TransactionEntity.toBackup() = BackupTransaction(
    id, amountInCents, type, categoryId, description, dateEpochDay, createdAtEpochMillis,
    updatedAtEpochMillis, fixedEntryId, savingsGoalId,
)

private fun BackupTransaction.toEntity() = TransactionEntity(
    id, amountInCents, type, categoryId, description, dateEpochDay, createdAtEpochMillis,
    updatedAtEpochMillis, fixedEntryId, savingsGoalId,
)

private fun FixedEntryEntity.toBackup() = BackupFixedEntry(
    id, type, description, amountInCents, categoryId, comment, isActive, manualDateMode,
    manualSpecificDateEpochDay, scheduleMode, scheduleHour, scheduleSpecificDateEpochDay,
    nextRunAtEpochMillis, lastAddedAtEpochMillis, lastAddedDateEpochDay,
)

private fun BackupFixedEntry.toEntity() = FixedEntryEntity(
    id, type, description, amountInCents, categoryId, comment, isActive, manualDateMode,
    manualSpecificDateEpochDay, scheduleMode, scheduleHour, scheduleSpecificDateEpochDay,
    nextRunAtEpochMillis, lastAddedAtEpochMillis, lastAddedDateEpochDay,
)

private fun PendingEntryEntity.toBackup() = BackupPendingEntry(
    id, type, description, amountInCents, categoryId, dateEpochDay, reminderMinutesOfDay,
    comment, isDone, doneAtEpochMillis, transactionId, createdAtEpochMillis,
    updatedAtEpochMillis, sourceShoppingListId,
)

private fun BackupPendingEntry.toEntity() = PendingEntryEntity(
    id, type, description, amountInCents, categoryId, dateEpochDay, reminderMinutesOfDay,
    comment, isDone, doneAtEpochMillis, transactionId, createdAtEpochMillis,
    updatedAtEpochMillis, sourceShoppingListId,
)

private fun BudgetConfigEntity.toBackup() = BackupBudgetConfig(
    id, amountInCents, period, cycleStartEpochDay, cycleStartedAtEpochMillis,
    incomeTransactionId, closingDays,
)

private fun BackupBudgetConfig.toEntity() = BudgetConfigEntity(
    id, amountInCents, period, cycleStartEpochDay, cycleStartedAtEpochMillis,
    incomeTransactionId, closingDays,
)

private fun BudgetCycleEntity.toBackup() = BackupBudgetCycle(
    id, period, budgetAmountInCents, incomeInCents, expenseInCents, startDateEpochDay,
    endDateEpochDay, closedAtEpochMillis,
)

private fun BackupBudgetCycle.toEntity() = BudgetCycleEntity(
    id, period, budgetAmountInCents, incomeInCents, expenseInCents, startDateEpochDay,
    endDateEpochDay, closedAtEpochMillis,
)

private fun SavingsGoalEntity.toBackup() = BackupSavingsGoal(
    id, name, targetAmountInCents, createdAtEpochMillis, completedAtEpochMillis,
)

private fun BackupSavingsGoal.toEntity() = SavingsGoalEntity(
    id, name, targetAmountInCents, createdAtEpochMillis, completedAtEpochMillis,
)

private fun ShoppingListEntity.toBackup() = BackupShoppingList(
    id, name, status, budgetInCents, expenseTransactionId, payableId, purchaseDateEpochDay,
    paymentMethod, expenseCategoryId, createdAtEpochMillis, updatedAtEpochMillis,
    completedAtEpochMillis,
)

private fun BackupShoppingList.toEntity() = ShoppingListEntity(
    id, name, status, budgetInCents, expenseTransactionId, payableId, purchaseDateEpochDay,
    paymentMethod, expenseCategoryId, createdAtEpochMillis, updatedAtEpochMillis,
    completedAtEpochMillis,
)

private fun ShoppingListItemEntity.toBackup() = BackupShoppingItem(
    id, shoppingListId, name, quantity, estimatedUnitPriceInCents, actualUnitPriceInCents,
    barcode, isPurchased, isIdentified, notes, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun BackupShoppingItem.toEntity() = ShoppingListItemEntity(
    id, shoppingListId, name, quantity, estimatedUnitPriceInCents, actualUnitPriceInCents,
    barcode, isPurchased, isIdentified, notes, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun ShoppingAdjustmentEntity.toBackup() = BackupShoppingAdjustment(
    id, shoppingListId, name, isPositive, amountInCents, createdAtEpochMillis,
)

private fun BackupShoppingAdjustment.toEntity() = ShoppingAdjustmentEntity(
    id, shoppingListId, name, isPositive, amountInCents, createdAtEpochMillis,
)

private fun KnownProductEntity.toBackup() = BackupKnownProduct(
    barcode, name, lastPriceInCents, lastUsedAtEpochMillis,
)

private fun BackupKnownProduct.toEntity() = KnownProductEntity(
    barcode, name, lastPriceInCents, lastUsedAtEpochMillis,
)

private fun ProductRecognitionAliasEntity.toBackup() = BackupProductAlias(
    id, detectedText, normalizedAlias, displayName, barcode, confirmationCount,
    lastUsedAtEpochMillis,
)

private fun BackupProductAlias.toEntity() = ProductRecognitionAliasEntity(
    id, detectedText, normalizedAlias, displayName, barcode, confirmationCount,
    lastUsedAtEpochMillis,
)

private fun FortnightTemplateEntity.toBackup() = BackupFortnightTemplate(
    id, description, firstFortnightAmountInCents, secondFortnightAmountInCents, categoryId,
    type, note, isActive, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun BackupFortnightTemplate.toEntity() = FortnightTemplateEntity(
    id, description, firstFortnightAmountInCents, secondFortnightAmountInCents, categoryId,
    type, note, isActive, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun FortnightPlanEntity.toBackup() = BackupFortnightPlan(
    id, startDateEpochDay, endDateEpochDay, slot, budgetInCents, status,
    createdAtEpochMillis, closedAtEpochMillis,
)

private fun BackupFortnightPlan.toEntity() = FortnightPlanEntity(
    id, startDateEpochDay, endDateEpochDay, slot, budgetInCents, status,
    createdAtEpochMillis, closedAtEpochMillis,
)

private fun FortnightPlanItemEntity.toBackup() = BackupFortnightItem(
    id, planId, templateId, description, plannedAmountInCents, categoryId, type,
    savingsGoalId, note, position, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun BackupFortnightItem.toEntity() = FortnightPlanItemEntity(
    id, planId, templateId, description, plannedAmountInCents, categoryId, type,
    savingsGoalId, note, position, createdAtEpochMillis, updatedAtEpochMillis,
)

private fun FortnightPaymentEntity.toBackup() = BackupFortnightPayment(
    id, itemId, amountInCents, dateEpochDay, transactionId, createdAtEpochMillis,
)

private fun BackupFortnightPayment.toEntity() = FortnightPaymentEntity(
    id, itemId, amountInCents, dateEpochDay, transactionId, createdAtEpochMillis,
)

private fun ExpenseFundingEntity.toBackup() = BackupExpenseFunding(
    id, transactionId, amountInCents, sourceDescription, dateEpochDay,
    createdAtEpochMillis, updatedAtEpochMillis,
)

private fun BackupExpenseFunding.toEntity() = ExpenseFundingEntity(
    id, transactionId, amountInCents, sourceDescription, dateEpochDay,
    createdAtEpochMillis, updatedAtEpochMillis,
)
