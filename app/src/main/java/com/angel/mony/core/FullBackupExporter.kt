package com.angel.mony.core

import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.FixedEntryEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.ProductRecognitionAliasEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.domain.model.BackupMovement
import org.json.JSONArray
import org.json.JSONObject

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
)

sealed class ParsedBackup {
    data class Full(val snapshot: FullBackupSnapshot) : ParsedBackup()
    data class LegacyCsv(val movements: List<BackupMovement>) : ParsedBackup()
}

object FullBackupExporter {
    const val CURRENT_VERSION = 2

    fun isJsonBackup(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") && trimmed.contains("\"version\"")
    }

    fun buildFullBackupJson(snapshot: FullBackupSnapshot): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("categories", JSONArray().apply {
            snapshot.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("type", c.type)
                    put("icon", c.icon)
                    put("isActive", c.isActive)
                    put("createdAtEpochMillis", c.createdAtEpochMillis)
                    if (c.budgetLimitInCents != null) put("budgetLimitInCents", c.budgetLimitInCents) else put("budgetLimitInCents", JSONObject.NULL)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            snapshot.transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("amountInCents", t.amountInCents)
                    put("type", t.type)
                    put("categoryId", t.categoryId)
                    if (t.description != null) put("description", t.description) else put("description", JSONObject.NULL)
                    put("dateEpochDay", t.dateEpochDay)
                    put("createdAtEpochMillis", t.createdAtEpochMillis)
                    put("updatedAtEpochMillis", t.updatedAtEpochMillis)
                    if (t.fixedEntryId != null) put("fixedEntryId", t.fixedEntryId) else put("fixedEntryId", JSONObject.NULL)
                    if (t.savingsGoalId != null) put("savingsGoalId", t.savingsGoalId) else put("savingsGoalId", JSONObject.NULL)
                })
            }
        })

        root.put("fixedEntries", JSONArray().apply {
            snapshot.fixedEntries.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("type", e.type)
                    put("description", e.description)
                    put("amountInCents", e.amountInCents)
                    put("categoryId", e.categoryId)
                    if (e.comment != null) put("comment", e.comment) else put("comment", JSONObject.NULL)
                    put("isActive", e.isActive)
                    put("manualDateMode", e.manualDateMode)
                    if (e.manualSpecificDateEpochDay != null) put("manualSpecificDateEpochDay", e.manualSpecificDateEpochDay) else put("manualSpecificDateEpochDay", JSONObject.NULL)
                    put("scheduleMode", e.scheduleMode)
                    put("scheduleHour", e.scheduleHour)
                    if (e.scheduleSpecificDateEpochDay != null) put("scheduleSpecificDateEpochDay", e.scheduleSpecificDateEpochDay) else put("scheduleSpecificDateEpochDay", JSONObject.NULL)
                    if (e.nextRunAtEpochMillis != null) put("nextRunAtEpochMillis", e.nextRunAtEpochMillis) else put("nextRunAtEpochMillis", JSONObject.NULL)
                    if (e.lastAddedAtEpochMillis != null) put("lastAddedAtEpochMillis", e.lastAddedAtEpochMillis) else put("lastAddedAtEpochMillis", JSONObject.NULL)
                    if (e.lastAddedDateEpochDay != null) put("lastAddedDateEpochDay", e.lastAddedDateEpochDay) else put("lastAddedDateEpochDay", JSONObject.NULL)
                })
            }
        })

        root.put("pendingEntries", JSONArray().apply {
            snapshot.pendingEntries.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("type", p.type)
                    put("description", p.description)
                    put("amountInCents", p.amountInCents)
                    put("categoryId", p.categoryId)
                    put("dateEpochDay", p.dateEpochDay)
                    if (p.reminderMinutesOfDay != null) put("reminderMinutesOfDay", p.reminderMinutesOfDay) else put("reminderMinutesOfDay", JSONObject.NULL)
                    if (p.comment != null) put("comment", p.comment) else put("comment", JSONObject.NULL)
                    put("isDone", p.isDone)
                    if (p.doneAtEpochMillis != null) put("doneAtEpochMillis", p.doneAtEpochMillis) else put("doneAtEpochMillis", JSONObject.NULL)
                    if (p.transactionId != null) put("transactionId", p.transactionId) else put("transactionId", JSONObject.NULL)
                    put("createdAtEpochMillis", p.createdAtEpochMillis)
                    put("updatedAtEpochMillis", p.updatedAtEpochMillis)
                    if (p.sourceShoppingListId != null) put("sourceShoppingListId", p.sourceShoppingListId) else put("sourceShoppingListId", JSONObject.NULL)
                })
            }
        })

        root.put("budgetConfig", snapshot.budgetConfig?.let { b ->
            JSONObject().apply {
                put("id", b.id)
                put("amountInCents", b.amountInCents)
                put("period", b.period)
                if (b.cycleStartEpochDay != null) put("cycleStartEpochDay", b.cycleStartEpochDay) else put("cycleStartEpochDay", JSONObject.NULL)
                if (b.cycleStartedAtEpochMillis != null) put("cycleStartedAtEpochMillis", b.cycleStartedAtEpochMillis) else put("cycleStartedAtEpochMillis", JSONObject.NULL)
                if (b.incomeTransactionId != null) put("incomeTransactionId", b.incomeTransactionId) else put("incomeTransactionId", JSONObject.NULL)
                put("closingDays", b.closingDays)
            }
        } ?: JSONObject.NULL)

        root.put("budgetCycles", JSONArray().apply {
            snapshot.budgetCycles.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("period", c.period)
                    put("budgetAmountInCents", c.budgetAmountInCents)
                    put("incomeInCents", c.incomeInCents)
                    put("expenseInCents", c.expenseInCents)
                    put("startDateEpochDay", c.startDateEpochDay)
                    put("endDateEpochDay", c.endDateEpochDay)
                    put("closedAtEpochMillis", c.closedAtEpochMillis)
                })
            }
        })

        root.put("savingsGoals", JSONArray().apply {
            snapshot.savingsGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmountInCents", g.targetAmountInCents)
                    put("createdAtEpochMillis", g.createdAtEpochMillis)
                    if (g.completedAtEpochMillis != null) put("completedAtEpochMillis", g.completedAtEpochMillis) else put("completedAtEpochMillis", JSONObject.NULL)
                })
            }
        })

        root.put("shoppingLists", JSONArray().apply {
            snapshot.shoppingLists.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id)
                    put("name", l.name)
                    put("status", l.status)
                    if (l.budgetInCents != null) put("budgetInCents", l.budgetInCents) else put("budgetInCents", JSONObject.NULL)
                    if (l.expenseTransactionId != null) put("expenseTransactionId", l.expenseTransactionId) else put("expenseTransactionId", JSONObject.NULL)
                    if (l.payableId != null) put("payableId", l.payableId) else put("payableId", JSONObject.NULL)
                    if (l.purchaseDateEpochDay != null) put("purchaseDateEpochDay", l.purchaseDateEpochDay) else put("purchaseDateEpochDay", JSONObject.NULL)
                    if (l.paymentMethod != null) put("paymentMethod", l.paymentMethod) else put("paymentMethod", JSONObject.NULL)
                    if (l.expenseCategoryId != null) put("expenseCategoryId", l.expenseCategoryId) else put("expenseCategoryId", JSONObject.NULL)
                    put("createdAtEpochMillis", l.createdAtEpochMillis)
                    put("updatedAtEpochMillis", l.updatedAtEpochMillis)
                    if (l.completedAtEpochMillis != null) put("completedAtEpochMillis", l.completedAtEpochMillis) else put("completedAtEpochMillis", JSONObject.NULL)
                })
            }
        })

        root.put("shoppingItems", JSONArray().apply {
            snapshot.shoppingItems.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id)
                    put("shoppingListId", i.shoppingListId)
                    put("name", i.name)
                    put("quantity", i.quantity)
                    if (i.estimatedUnitPriceInCents != null) put("estimatedUnitPriceInCents", i.estimatedUnitPriceInCents) else put("estimatedUnitPriceInCents", JSONObject.NULL)
                    if (i.actualUnitPriceInCents != null) put("actualUnitPriceInCents", i.actualUnitPriceInCents) else put("actualUnitPriceInCents", JSONObject.NULL)
                    if (i.barcode != null) put("barcode", i.barcode) else put("barcode", JSONObject.NULL)
                    put("isPurchased", i.isPurchased)
                    put("isIdentified", i.isIdentified)
                    if (i.notes != null) put("notes", i.notes) else put("notes", JSONObject.NULL)
                    put("createdAtEpochMillis", i.createdAtEpochMillis)
                    put("updatedAtEpochMillis", i.updatedAtEpochMillis)
                })
            }
        })

        root.put("shoppingAdjustments", JSONArray().apply {
            snapshot.shoppingAdjustments.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("shoppingListId", a.shoppingListId)
                    put("name", a.name)
                    put("isPositive", a.isPositive)
                    put("amountInCents", a.amountInCents)
                    put("createdAtEpochMillis", a.createdAtEpochMillis)
                })
            }
        })

        root.put("knownProducts", JSONArray().apply {
            snapshot.knownProducts.forEach { k ->
                put(JSONObject().apply {
                    put("barcode", k.barcode)
                    put("name", k.name)
                    if (k.lastPriceInCents != null) put("lastPriceInCents", k.lastPriceInCents) else put("lastPriceInCents", JSONObject.NULL)
                    put("lastUsedAtEpochMillis", k.lastUsedAtEpochMillis)
                })
            }
        })

        root.put("productAliases", JSONArray().apply {
            snapshot.productAliases.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("detectedText", p.detectedText)
                    put("normalizedAlias", p.normalizedAlias)
                    put("displayName", p.displayName)
                    if (p.barcode != null) put("barcode", p.barcode) else put("barcode", JSONObject.NULL)
                    put("confirmationCount", p.confirmationCount)
                    put("lastUsedAtEpochMillis", p.lastUsedAtEpochMillis)
                })
            }
        })

        return root.toString(2)
    }

    fun parseBackup(content: String): ParsedBackup {
        val stripped = content.removePrefix(CsvExporter.UTF8_BOM).trim()
        if (stripped.isEmpty()) error("El archivo está vacío")
        if (isJsonBackup(stripped)) {
            return ParsedBackup.Full(parseJsonBackup(stripped))
        }
        // Intentar como CSV legacy
        val movements = CsvExporter.parseBackup(stripped)
        return ParsedBackup.LegacyCsv(movements)
    }

    fun parseJsonBackup(content: String): FullBackupSnapshot {
        val root = JSONObject(content.removePrefix(CsvExporter.UTF8_BOM))
        val version = root.optInt("version", 1)
        if (version > CURRENT_VERSION) {
            // Permite versiones futuras menores, pero avisa si es mayor
        }

        fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
        fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

        val categories = mutableListOf<CategoryEntity>()
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.optLong("id"),
                        name = o.getString("name"),
                        type = o.getString("type"),
                        icon = o.optString("icon", "label"),
                        isActive = o.optBoolean("isActive", true),
                        createdAtEpochMillis = o.optLong("createdAtEpochMillis"),
                        budgetLimitInCents = o.optLongOrNull("budgetLimitInCents"),
                    )
                )
            }
        }

        val transactions = mutableListOf<TransactionEntity>()
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = o.optLong("id"),
                        amountInCents = o.getLong("amountInCents"),
                        type = o.getString("type"),
                        categoryId = o.getLong("categoryId"),
                        description = o.optStringOrNull("description"),
                        dateEpochDay = o.getLong("dateEpochDay"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                        fixedEntryId = o.optLongOrNull("fixedEntryId"),
                        savingsGoalId = o.optLongOrNull("savingsGoalId"),
                    )
                )
            }
        }

        val fixedEntries = mutableListOf<FixedEntryEntity>()
        root.optJSONArray("fixedEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                fixedEntries.add(
                    FixedEntryEntity(
                        id = o.optLong("id"),
                        type = o.getString("type"),
                        description = o.getString("description"),
                        amountInCents = o.getLong("amountInCents"),
                        categoryId = o.getLong("categoryId"),
                        comment = o.optStringOrNull("comment"),
                        isActive = o.optBoolean("isActive", true),
                        manualDateMode = o.optString("manualDateMode", "TODAY"),
                        manualSpecificDateEpochDay = o.optLongOrNull("manualSpecificDateEpochDay"),
                        scheduleMode = o.optString("scheduleMode", "MANUAL"),
                        scheduleHour = o.optInt("scheduleHour", 9),
                        scheduleSpecificDateEpochDay = o.optLongOrNull("scheduleSpecificDateEpochDay"),
                        nextRunAtEpochMillis = o.optLongOrNull("nextRunAtEpochMillis"),
                        lastAddedAtEpochMillis = o.optLongOrNull("lastAddedAtEpochMillis"),
                        lastAddedDateEpochDay = o.optLongOrNull("lastAddedDateEpochDay"),
                    )
                )
            }
        }

        val pendingEntries = mutableListOf<PendingEntryEntity>()
        root.optJSONArray("pendingEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                pendingEntries.add(
                    PendingEntryEntity(
                        id = o.optLong("id"),
                        type = o.getString("type"),
                        description = o.getString("description"),
                        amountInCents = o.getLong("amountInCents"),
                        categoryId = o.getLong("categoryId"),
                        dateEpochDay = o.getLong("dateEpochDay"),
                        reminderMinutesOfDay = if (o.isNull("reminderMinutesOfDay")) null else o.optInt("reminderMinutesOfDay"),
                        comment = o.optStringOrNull("comment"),
                        isDone = o.optBoolean("isDone", false),
                        doneAtEpochMillis = o.optLongOrNull("doneAtEpochMillis"),
                        transactionId = o.optLongOrNull("transactionId"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                        sourceShoppingListId = o.optLongOrNull("sourceShoppingListId"),
                    )
                )
            }
        }

        val budgetConfig = if (root.isNull("budgetConfig")) null else {
            val o = root.getJSONObject("budgetConfig")
            BudgetConfigEntity(
                id = o.optInt("id", 1),
                amountInCents = o.getLong("amountInCents"),
                period = o.getString("period"),
                cycleStartEpochDay = o.optLongOrNull("cycleStartEpochDay"),
                cycleStartedAtEpochMillis = o.optLongOrNull("cycleStartedAtEpochMillis"),
                incomeTransactionId = o.optLongOrNull("incomeTransactionId"),
                closingDays = o.optString("closingDays", "15"),
            )
        }

        val budgetCycles = mutableListOf<BudgetCycleEntity>()
        root.optJSONArray("budgetCycles")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                budgetCycles.add(
                    BudgetCycleEntity(
                        id = o.optLong("id"),
                        period = o.getString("period"),
                        budgetAmountInCents = o.getLong("budgetAmountInCents"),
                        incomeInCents = o.getLong("incomeInCents"),
                        expenseInCents = o.getLong("expenseInCents"),
                        startDateEpochDay = o.getLong("startDateEpochDay"),
                        endDateEpochDay = o.getLong("endDateEpochDay"),
                        closedAtEpochMillis = o.getLong("closedAtEpochMillis"),
                    )
                )
            }
        }

        val savingsGoals = mutableListOf<SavingsGoalEntity>()
        root.optJSONArray("savingsGoals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                savingsGoals.add(
                    SavingsGoalEntity(
                        id = o.optLong("id"),
                        name = o.getString("name"),
                        targetAmountInCents = o.getLong("targetAmountInCents"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        completedAtEpochMillis = o.optLongOrNull("completedAtEpochMillis"),
                    )
                )
            }
        }

        val shoppingLists = mutableListOf<ShoppingListEntity>()
        root.optJSONArray("shoppingLists")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                shoppingLists.add(
                    ShoppingListEntity(
                        id = o.optLong("id"),
                        name = o.getString("name"),
                        status = o.getString("status"),
                        budgetInCents = o.optLongOrNull("budgetInCents"),
                        expenseTransactionId = o.optLongOrNull("expenseTransactionId"),
                        payableId = o.optLongOrNull("payableId"),
                        purchaseDateEpochDay = o.optLongOrNull("purchaseDateEpochDay"),
                        paymentMethod = o.optStringOrNull("paymentMethod"),
                        expenseCategoryId = o.optLongOrNull("expenseCategoryId"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                        completedAtEpochMillis = o.optLongOrNull("completedAtEpochMillis"),
                    )
                )
            }
        }

        val shoppingItems = mutableListOf<ShoppingListItemEntity>()
        root.optJSONArray("shoppingItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                shoppingItems.add(
                    ShoppingListItemEntity(
                        id = o.optLong("id"),
                        shoppingListId = o.getLong("shoppingListId"),
                        name = o.getString("name"),
                        quantity = o.getInt("quantity"),
                        estimatedUnitPriceInCents = o.optLongOrNull("estimatedUnitPriceInCents"),
                        actualUnitPriceInCents = o.optLongOrNull("actualUnitPriceInCents"),
                        barcode = o.optStringOrNull("barcode"),
                        isPurchased = o.optBoolean("isPurchased", false),
                        isIdentified = o.optBoolean("isIdentified", false),
                        notes = o.optStringOrNull("notes"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                        updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                    )
                )
            }
        }

        val shoppingAdjustments = mutableListOf<ShoppingAdjustmentEntity>()
        root.optJSONArray("shoppingAdjustments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                shoppingAdjustments.add(
                    ShoppingAdjustmentEntity(
                        id = o.optLong("id"),
                        shoppingListId = o.getLong("shoppingListId"),
                        name = o.getString("name"),
                        isPositive = o.getBoolean("isPositive"),
                        amountInCents = o.getLong("amountInCents"),
                        createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                    )
                )
            }
        }

        val knownProducts = mutableListOf<KnownProductEntity>()
        root.optJSONArray("knownProducts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                knownProducts.add(
                    KnownProductEntity(
                        barcode = o.getString("barcode"),
                        name = o.getString("name"),
                        lastPriceInCents = o.optLongOrNull("lastPriceInCents"),
                        lastUsedAtEpochMillis = o.getLong("lastUsedAtEpochMillis"),
                    )
                )
            }
        }

        val productAliases = mutableListOf<ProductRecognitionAliasEntity>()
        root.optJSONArray("productAliases")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                productAliases.add(
                    ProductRecognitionAliasEntity(
                        id = o.optLong("id"),
                        detectedText = o.getString("detectedText"),
                        normalizedAlias = o.getString("normalizedAlias"),
                        displayName = o.getString("displayName"),
                        barcode = o.optStringOrNull("barcode"),
                        confirmationCount = o.getInt("confirmationCount"),
                        lastUsedAtEpochMillis = o.getLong("lastUsedAtEpochMillis"),
                    )
                )
            }
        }

        return FullBackupSnapshot(
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
        )
    }
}
