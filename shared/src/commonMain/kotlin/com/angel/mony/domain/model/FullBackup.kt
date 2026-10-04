package com.angel.mony.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FullBackupSnapshot(
    val categories: List<BackupCategory>,
    val transactions: List<BackupTransaction>,
    val fixedEntries: List<BackupFixedEntry>,
    val pendingEntries: List<BackupPendingEntry>,
    val budgetConfig: BackupBudgetConfig?,
    val budgetCycles: List<BackupBudgetCycle>,
    val savingsGoals: List<BackupSavingsGoal>,
    val shoppingLists: List<BackupShoppingList>,
    val shoppingItems: List<BackupShoppingItem>,
    val shoppingAdjustments: List<BackupShoppingAdjustment>,
    val knownProducts: List<BackupKnownProduct> = emptyList(),
    val productAliases: List<BackupProductAlias> = emptyList(),
    val fortnightTemplates: List<BackupFortnightTemplate> = emptyList(),
    val fortnightPlans: List<BackupFortnightPlan> = emptyList(),
    val fortnightItems: List<BackupFortnightItem> = emptyList(),
    val fortnightPayments: List<BackupFortnightPayment> = emptyList(),
    val expenseFunding: List<BackupExpenseFunding> = emptyList(),
)

@Serializable
data class BackupCategory(
    val id: Long = 0,
    val name: String,
    val type: String,
    val icon: String = "label",
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long = 0,
    val budgetLimitInCents: Long? = null,
)

@Serializable
data class BackupTransaction(
    val id: Long = 0,
    val amountInCents: Long,
    val type: String,
    val categoryId: Long,
    val description: String? = null,
    val dateEpochDay: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val fixedEntryId: Long? = null,
    val savingsGoalId: Long? = null,
)

@Serializable
data class BackupFixedEntry(
    val id: Long = 0,
    val type: String,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val comment: String? = null,
    val isActive: Boolean = true,
    val manualDateMode: String = "TODAY",
    val manualSpecificDateEpochDay: Long? = null,
    val scheduleMode: String = "MANUAL",
    val scheduleHour: Int = 9,
    val scheduleSpecificDateEpochDay: Long? = null,
    val nextRunAtEpochMillis: Long? = null,
    val lastAddedAtEpochMillis: Long? = null,
    val lastAddedDateEpochDay: Long? = null,
)

@Serializable
data class BackupPendingEntry(
    val id: Long = 0,
    val type: String,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val dateEpochDay: Long,
    val reminderMinutesOfDay: Int? = null,
    val comment: String? = null,
    val isDone: Boolean = false,
    val doneAtEpochMillis: Long? = null,
    val transactionId: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val sourceShoppingListId: Long? = null,
)

@Serializable
data class BackupBudgetConfig(
    val id: Int = 1,
    val amountInCents: Long,
    val period: String,
    val cycleStartEpochDay: Long? = null,
    val cycleStartedAtEpochMillis: Long? = null,
    val incomeTransactionId: Long? = null,
    val closingDays: String = "15",
)

@Serializable
data class BackupBudgetCycle(
    val id: Long = 0,
    val period: String,
    val budgetAmountInCents: Long,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val closedAtEpochMillis: Long,
)

@Serializable
data class BackupSavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmountInCents: Long,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

@Serializable
data class BackupShoppingList(
    val id: Long = 0,
    val name: String,
    val status: String,
    val budgetInCents: Long? = null,
    val expenseTransactionId: Long? = null,
    val payableId: Long? = null,
    val purchaseDateEpochDay: Long? = null,
    val paymentMethod: String? = null,
    val expenseCategoryId: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

@Serializable
data class BackupShoppingItem(
    val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val quantity: Int,
    val estimatedUnitPriceInCents: Long? = null,
    val actualUnitPriceInCents: Long? = null,
    val barcode: String? = null,
    val isPurchased: Boolean = false,
    val isIdentified: Boolean = false,
    val notes: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class BackupShoppingAdjustment(
    val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val isPositive: Boolean,
    val amountInCents: Long,
    val createdAtEpochMillis: Long,
)

@Serializable
data class BackupKnownProduct(
    val barcode: String,
    val name: String,
    val lastPriceInCents: Long? = null,
    val lastUsedAtEpochMillis: Long,
)

@Serializable
data class BackupProductAlias(
    val id: Long = 0,
    val detectedText: String,
    val normalizedAlias: String,
    val displayName: String,
    val barcode: String? = null,
    val confirmationCount: Int,
    val lastUsedAtEpochMillis: Long,
)

@Serializable
data class BackupFortnightTemplate(
    val id: Long = 0,
    val description: String,
    val firstFortnightAmountInCents: Long? = null,
    val secondFortnightAmountInCents: Long? = null,
    val categoryId: Long,
    val type: String,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class BackupFortnightPlan(
    val id: Long = 0,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val slot: String = "FIRST",
    val budgetInCents: Long,
    val status: String = "OPEN",
    val createdAtEpochMillis: Long,
    val closedAtEpochMillis: Long? = null,
)

@Serializable
data class BackupFortnightItem(
    val id: Long = 0,
    val planId: Long,
    val templateId: Long? = null,
    val description: String,
    val plannedAmountInCents: Long,
    val categoryId: Long,
    val type: String,
    val savingsGoalId: Long? = null,
    val note: String? = null,
    val position: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class BackupFortnightPayment(
    val id: Long = 0,
    val itemId: Long,
    val amountInCents: Long,
    val dateEpochDay: Long,
    val transactionId: Long? = null,
    val createdAtEpochMillis: Long,
)

@Serializable
data class BackupExpenseFunding(
    val id: Long = 0,
    val transactionId: Long,
    val amountInCents: Long,
    val sourceDescription: String,
    val dateEpochDay: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
