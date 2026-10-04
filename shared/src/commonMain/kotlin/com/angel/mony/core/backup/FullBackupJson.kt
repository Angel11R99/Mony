package com.angel.mony.core.backup

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
import com.angel.mony.domain.model.BackupPendingEntry
import com.angel.mony.domain.model.BackupProductAlias
import com.angel.mony.domain.model.BackupSavingsGoal
import com.angel.mony.domain.model.BackupShoppingAdjustment
import com.angel.mony.domain.model.BackupShoppingItem
import com.angel.mony.domain.model.BackupShoppingList
import com.angel.mony.domain.model.BackupTransaction
import com.angel.mony.domain.model.FullBackupSnapshot
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object FullBackupJson {
    const val CURRENT_VERSION = 3
    const val UTF8_BOM = "\uFEFF"

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun isJsonBackup(content: String): Boolean {
        val trimmed = content.removePrefix(UTF8_BOM).trim()
        return trimmed.startsWith("{") && trimmed.contains("\"version\"")
    }

    fun encode(
        snapshot: FullBackupSnapshot,
        exportedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): String = json.encodeToString(
        FullBackupDocument.serializer(),
        FullBackupDocument.from(snapshot, exportedAtEpochMillis),
    )

    fun decode(content: String): FullBackupSnapshot {
        val document = json.decodeFromString(
            FullBackupDocument.serializer(),
            content.removePrefix(UTF8_BOM).trim(),
        )
        return document.toSnapshot()
    }
}

@Serializable
private data class FullBackupDocument(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val categories: List<BackupCategory> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList(),
    val fixedEntries: List<BackupFixedEntry> = emptyList(),
    val pendingEntries: List<BackupPendingEntry> = emptyList(),
    val budgetConfig: BackupBudgetConfig? = null,
    val budgetCycles: List<BackupBudgetCycle> = emptyList(),
    val savingsGoals: List<BackupSavingsGoal> = emptyList(),
    val shoppingLists: List<BackupShoppingList> = emptyList(),
    val shoppingItems: List<BackupShoppingItem> = emptyList(),
    val shoppingAdjustments: List<BackupShoppingAdjustment> = emptyList(),
    val knownProducts: List<BackupKnownProduct> = emptyList(),
    val productAliases: List<BackupProductAlias> = emptyList(),
    val fortnightTemplates: List<BackupFortnightTemplate> = emptyList(),
    val fortnightPlans: List<BackupFortnightPlan> = emptyList(),
    val fortnightItems: List<BackupFortnightItem> = emptyList(),
    val fortnightPayments: List<BackupFortnightPayment> = emptyList(),
    val expenseFunding: List<BackupExpenseFunding> = emptyList(),
) {
    fun toSnapshot() = FullBackupSnapshot(
        categories = categories,
        transactions = transactions,
        fixedEntries = fixedEntries,
        pendingEntries = pendingEntries,
        budgetConfig = budgetConfig,
        budgetCycles = budgetCycles,
        savingsGoals = savingsGoals,
        shoppingLists = shoppingLists,
        shoppingItems = shoppingItems,
        shoppingAdjustments = shoppingAdjustments,
        knownProducts = knownProducts,
        productAliases = productAliases,
        fortnightTemplates = fortnightTemplates,
        fortnightPlans = fortnightPlans,
        fortnightItems = fortnightItems,
        fortnightPayments = fortnightPayments,
        expenseFunding = expenseFunding,
    )

    companion object {
        fun from(snapshot: FullBackupSnapshot, exportedAt: Long) = FullBackupDocument(
            version = FullBackupJson.CURRENT_VERSION,
            exportedAt = exportedAt,
            categories = snapshot.categories,
            transactions = snapshot.transactions,
            fixedEntries = snapshot.fixedEntries,
            pendingEntries = snapshot.pendingEntries,
            budgetConfig = snapshot.budgetConfig,
            budgetCycles = snapshot.budgetCycles,
            savingsGoals = snapshot.savingsGoals,
            shoppingLists = snapshot.shoppingLists,
            shoppingItems = snapshot.shoppingItems,
            shoppingAdjustments = snapshot.shoppingAdjustments,
            knownProducts = snapshot.knownProducts,
            productAliases = snapshot.productAliases,
            fortnightTemplates = snapshot.fortnightTemplates,
            fortnightPlans = snapshot.fortnightPlans,
            fortnightItems = snapshot.fortnightItems,
            fortnightPayments = snapshot.fortnightPayments,
            expenseFunding = snapshot.expenseFunding,
        )
    }
}
