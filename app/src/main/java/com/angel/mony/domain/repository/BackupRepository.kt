package com.angel.mony.domain.repository

data class BackupRestoreResult(
    val insertedTransactions: Int = 0,
    val insertedCategories: Int = 0,
    val insertedFixedEntries: Int = 0,
    val insertedPendingEntries: Int = 0,
    val insertedSavingsGoals: Int = 0,
    val insertedShoppingLists: Int = 0,
    val insertedBudgetCycles: Int = 0,
    val skippedTransactions: Int = 0,
    val isLegacyCsv: Boolean = false,
) {
    val totalInserted: Int get() = insertedTransactions + insertedCategories + insertedFixedEntries + insertedPendingEntries + insertedSavingsGoals + insertedShoppingLists + insertedBudgetCycles
}

interface BackupRepository {
    suspend fun buildFullBackupJson(): String
    suspend fun restoreBackup(content: String): BackupRestoreResult
    suspend fun parsePreview(content: String): BackupPreview
}

data class BackupPreview(
    val version: Int,
    val isLegacyCsv: Boolean,
    val transactionsCount: Int,
    val categoriesCount: Int,
    val fixedEntriesCount: Int,
    val pendingEntriesCount: Int,
    val savingsGoalsCount: Int,
    val shoppingListsCount: Int,
    val budgetCyclesCount: Int,
    val firstDate: java.time.LocalDate?,
    val lastDate: java.time.LocalDate?,
)
