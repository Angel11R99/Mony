package com.angel.mony.data.repository

import androidx.room.withTransaction
import com.angel.mony.core.FullBackupExporter
import com.angel.mony.core.FullBackupSnapshot
import com.angel.mony.core.ParsedBackup
import com.angel.mony.data.local.dao.BudgetConfigDao
import com.angel.mony.data.local.dao.BudgetCycleDao
import com.angel.mony.data.local.dao.CategoryDao
import com.angel.mony.data.local.dao.FixedEntryDao
import com.angel.mony.data.local.dao.FortnightPaymentDao
import com.angel.mony.data.local.dao.FortnightPlanDao
import com.angel.mony.data.local.dao.FortnightTemplateDao
import com.angel.mony.data.local.dao.PendingEntryDao
import com.angel.mony.data.local.dao.SavingsGoalDao
import com.angel.mony.data.local.dao.ShoppingListDao
import com.angel.mony.data.local.dao.TransactionDao
import com.angel.mony.data.local.database.FinanceDatabase
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import com.angel.mony.data.local.entity.FortnightPlanEntity
import com.angel.mony.data.local.entity.FortnightPlanItemEntity
import com.angel.mony.data.local.entity.FortnightTemplateEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.entity.ShoppingAdjustmentEntity
import com.angel.mony.data.local.entity.ShoppingListEntity
import com.angel.mony.data.local.entity.ShoppingListItemEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.repository.BackupPreview
import com.angel.mony.domain.repository.BackupRepository
import com.angel.mony.domain.repository.BackupRestoreResult
import javax.inject.Inject
import java.time.LocalDate

class RoomBackupRepository @Inject constructor(
    private val database: FinanceDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val fixedEntryDao: FixedEntryDao,
    private val pendingEntryDao: PendingEntryDao,
    private val budgetConfigDao: BudgetConfigDao,
    private val budgetCycleDao: BudgetCycleDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val shoppingListDao: ShoppingListDao,
    private val fortnightTemplateDao: FortnightTemplateDao,
    private val fortnightPlanDao: FortnightPlanDao,
    private val fortnightPaymentDao: FortnightPaymentDao,
) : BackupRepository {

    override suspend fun buildFullBackupJson(): String {
        val snapshot = FullBackupSnapshot(
            categories = categoryDao.getAll(),
            transactions = transactionDao.getAll(),
            fixedEntries = fixedEntryDao.getAll(),
            pendingEntries = pendingEntryDao.getAll(),
            budgetConfig = budgetConfigDao.get(),
            budgetCycles = budgetCycleDao.getAll(),
            savingsGoals = savingsGoalDao.getAll(),
            shoppingLists = shoppingListDao.getAllLists(),
            shoppingItems = shoppingListDao.getAllItems(),
            shoppingAdjustments = shoppingListDao.getAllAdjustments(),
            knownProducts = shoppingListDao.getAllKnownProducts(),
            productAliases = shoppingListDao.getAllAliases(),
            fortnightTemplates = fortnightTemplateDao.getAllTemplates(),
            fortnightPlans = fortnightPlanDao.getAllPlans(),
            fortnightItems = fortnightPlanDao.getAllItems(),
            fortnightPayments = fortnightPaymentDao.getAllPayments(),
        )
        return FullBackupExporter.buildFullBackupJson(snapshot)
    }

    override suspend fun parsePreview(content: String): BackupPreview {
        val parsed = FullBackupExporter.parseBackup(content)
        return when (parsed) {
            is ParsedBackup.LegacyCsv -> {
                val movements = parsed.movements
                BackupPreview(
                    version = 1,
                    isLegacyCsv = true,
                    transactionsCount = movements.size,
                    categoriesCount = 0,
                    fixedEntriesCount = 0,
                    pendingEntriesCount = 0,
                    savingsGoalsCount = 0,
                    shoppingListsCount = 0,
                    budgetCyclesCount = 0,
                    firstDate = movements.minOfOrNull { it.date },
                    lastDate = movements.maxOfOrNull { it.date },
                )
            }
            is ParsedBackup.Full -> {
                val s = parsed.snapshot
                val allDates = mutableListOf<LocalDate>()
                s.transactions.forEach { allDates.add(LocalDate.ofEpochDay(it.dateEpochDay)) }
                s.pendingEntries.forEach { allDates.add(LocalDate.ofEpochDay(it.dateEpochDay)) }
                s.budgetCycles.forEach {
                    allDates.add(LocalDate.ofEpochDay(it.startDateEpochDay))
                    allDates.add(LocalDate.ofEpochDay(it.endDateEpochDay))
                }
                s.fortnightPayments.forEach { allDates.add(LocalDate.ofEpochDay(it.dateEpochDay)) }
                BackupPreview(
                    version = FullBackupExporter.CURRENT_VERSION,
                    isLegacyCsv = false,
                    transactionsCount = s.transactions.size,
                    categoriesCount = s.categories.size,
                    fixedEntriesCount = s.fixedEntries.size,
                    pendingEntriesCount = s.pendingEntries.size,
                    savingsGoalsCount = s.savingsGoals.size,
                    shoppingListsCount = s.shoppingLists.size,
                    budgetCyclesCount = s.budgetCycles.size,
                    fortnightPlansCount = s.fortnightPlans.size,
                    fortnightPaymentsCount = s.fortnightPayments.size,
                    firstDate = allDates.minOrNull(),
                    lastDate = allDates.maxOrNull(),
                )
            }
        }
    }

    override suspend fun restoreBackup(content: String): BackupRestoreResult = database.withTransaction {
        val parsed = FullBackupExporter.parseBackup(content)
        if (parsed is ParsedBackup.LegacyCsv) {
            return@withTransaction restoreLegacyCsv(parsed.movements)
        }
        val snapshot = (parsed as ParsedBackup.Full).snapshot
        var insertedCategories = 0
        var insertedTransactions = 0
        var insertedFixed = 0
        var insertedPending = 0
        var insertedSavings = 0
        var insertedShoppingLists = 0
        var insertedBudgetCycles = 0
        var insertedFortnightPlans = 0
        var insertedFortnightPayments = 0

        // 1. Categorías: map oldId -> newId
        val existingCategories = categoryDao.getAll()
        val categoryKeyToId = mutableMapOf<String, Long>()
        existingCategories.forEach { e ->
            categoryKeyToId[categoryKey(e.name, e.type)] = e.id
        }
        val categoryIdMap = mutableMapOf<Long, Long>()
        // También index por nombre normalizado para búsqueda rápida de categoría por nombre (legacy)
        for (cat in snapshot.categories) {
            val key = categoryKey(cat.name, cat.type)
            val existingId = categoryKeyToId[key]
            if (existingId != null) {
                categoryIdMap[cat.id] = existingId
            } else {
                val newId = categoryDao.insert(
                    CategoryEntity(
                        name = cat.name,
                        type = cat.type,
                        icon = cat.icon,
                        isActive = cat.isActive,
                        createdAtEpochMillis = cat.createdAtEpochMillis,
                        budgetLimitInCents = cat.budgetLimitInCents,
                    )
                )
                categoryKeyToId[key] = newId
                categoryIdMap[cat.id] = newId
                insertedCategories++
            }
        }
        // Para categorías referenciadas pero no incluidas en backup (transacciones legacy que crean categorías al vuelo),
        // el mapeo se completará on demand.

        suspend fun resolveCategoryId(oldCategoryId: Long, fallbackName: String? = null, fallbackType: String? = null): Long {
            categoryIdMap[oldCategoryId]?.let { return it }
            // Si no está en el backup, buscar por id existente (puede ser que la categoría ya exista con ese id)
            categoryDao.get(oldCategoryId)?.let {
                categoryIdMap[oldCategoryId] = it.id
                return it.id
            }
            // Último recurso: crear categoría por nombre si se provee
            if (fallbackName != null && fallbackType != null) {
                val key = categoryKey(fallbackName, fallbackType)
                categoryKeyToId[key]?.let {
                    categoryIdMap[oldCategoryId] = it
                    return it
                }
                val newId = categoryDao.insert(
                    CategoryEntity(
                        name = fallbackName,
                        type = fallbackType,
                        icon = "label",
                        isActive = true,
                        createdAtEpochMillis = System.currentTimeMillis(),
                    )
                )
                categoryKeyToId[key] = newId
                categoryIdMap[oldCategoryId] = newId
                insertedCategories++
                return newId
            }
            error("No se pudo resolver la categoría $oldCategoryId")
        }

        // 2. SavingsGoals deduplicados por nombre
        val existingSavings = savingsGoalDao.getAll()
        val savingsNameToId = mutableMapOf<String, Long>()
        existingSavings.forEach { savingsNameToId[it.name.trim().lowercase()] = it.id }
        val savingsIdMap = mutableMapOf<Long, Long>()
        for (goal in snapshot.savingsGoals) {
            val key = goal.name.trim().lowercase()
            val existingId = savingsNameToId[key]
            if (existingId != null) {
                savingsIdMap[goal.id] = existingId
            } else {
                val newId = savingsGoalDao.insert(
                    SavingsGoalEntity(
                        name = goal.name,
                        targetAmountInCents = goal.targetAmountInCents,
                        createdAtEpochMillis = goal.createdAtEpochMillis,
                        completedAtEpochMillis = goal.completedAtEpochMillis,
                    )
                )
                savingsNameToId[key] = newId
                savingsIdMap[goal.id] = newId
                insertedSavings++
            }
        }

        // 3. FixedEntries dedup por descripción+amount+type+category
        val existingFixed = fixedEntryDao.getAll().toMutableList()
        val fixedKeySet = existingFixed.mapTo(mutableSetOf()) { fixedDedupKey(it) }
        val fixedIdMap = mutableMapOf<Long, Long>()
        for (entry in snapshot.fixedEntries) {
            val newCategoryId = resolveCategoryId(entry.categoryId)
            val dedupKey = fixedDedupKey(entry, newCategoryId)
            if (fixedKeySet.contains(dedupKey)) {
                // Buscar el id existente con esa key para mapeo
                val existing = existingFixed.firstOrNull { fixedDedupKey(it) == dedupKey }
                if (existing != null) fixedIdMap[entry.id] = existing.id
                continue
            }
            val newId = fixedEntryDao.upsert(
                entry.copy(id = 0, categoryId = newCategoryId)
            )
            fixedKeySet.add(dedupKey)
            fixedIdMap[entry.id] = newId
            // Añadir a lista existente para futuras búsquedas
            existingFixed.add(entry.copy(id = newId, categoryId = newCategoryId))
            insertedFixed++
        }

        // 4. BudgetConfig upsert si no existe o si backup tiene datos
        snapshot.budgetConfig?.let { backupConfig ->
            val existing = budgetConfigDao.get()
            if (existing == null) {
                budgetConfigDao.upsert(backupConfig.copy(id = 1))
            } else {
                budgetConfigDao.upsert(backupConfig.copy(id = 1))
            }
        }

        // 4b. Plantillas de quincena: dedup por descripción+type+categoría
        val existingTemplates = fortnightTemplateDao.getAllTemplates()
        val templateKeyToId = existingTemplates.associate { templateDedupKey(it) to it.id }.toMutableMap()
        val templateIdMap = mutableMapOf<Long, Long>()
        for (template in snapshot.fortnightTemplates) {
            val newCategoryId = resolveCategoryId(template.categoryId)
            val key = templateDedupKey(template, newCategoryId)
            val existingId = templateKeyToId[key]
            if (existingId != null) {
                templateIdMap[template.id] = existingId
                continue
            }
            val newId = fortnightTemplateDao.insertTemplate(
                template.copy(
                    id = 0,
                    categoryId = newCategoryId,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                )
            )
            templateKeyToId[key] = newId
            templateIdMap[template.id] = newId
        }

        // 5. BudgetCycles dedup por start+end
        val existingCycles = budgetCycleDao.getAll()
        val cycleKeySet = existingCycles.mapTo(mutableSetOf()) { "${it.startDateEpochDay}|${it.endDateEpochDay}" }
        for (cycle in snapshot.budgetCycles) {
            val key = "${cycle.startDateEpochDay}|${cycle.endDateEpochDay}"
            if (cycleKeySet.contains(key)) continue
            budgetCycleDao.insert(cycle.copy(id = 0))
            cycleKeySet.add(key)
            insertedBudgetCycles++
        }

        // 5b. Planes de quincena dedup por período + items
        val existingPlans = fortnightPlanDao.getAllPlans()
        val planKeySet = existingPlans.mapTo(mutableSetOf()) { "${it.startDateEpochDay}|${it.endDateEpochDay}" }
        val planIdMap = mutableMapOf<Long, Long>()
        val itemIdMap = mutableMapOf<Long, Long>()
        val itemsByPlan = snapshot.fortnightItems.groupBy { it.planId }
        for (plan in snapshot.fortnightPlans) {
            val key = "${plan.startDateEpochDay}|${plan.endDateEpochDay}"
            if (planKeySet.contains(key)) {
                existingPlans.firstOrNull { "${it.startDateEpochDay}|${it.endDateEpochDay}" == key }
                    ?.let { planIdMap[plan.id] = it.id }
                continue
            }
            val newPlanId = fortnightPlanDao.insertPlan(plan.copy(id = 0))
            planKeySet.add(key)
            planIdMap[plan.id] = newPlanId
            insertedFortnightPlans++
            itemsByPlan[plan.id]?.forEach { item ->
                val newItemId = fortnightPlanDao.insertItem(
                    item.copy(
                        id = 0,
                        planId = newPlanId,
                        templateId = item.templateId?.let { templateIdMap[it] },
                        categoryId = resolveCategoryId(item.categoryId),
                        savingsGoalId = item.savingsGoalId?.let { savingsIdMap[it] },
                    )
                )
                itemIdMap[item.id] = newItemId
            }
        }

        // 6. ShoppingLists + items + adjustments (debe ir antes de pending para mapear sourceShoppingListId)
        val existingLists = shoppingListDao.getAllLists()
        val listKeySet = existingLists.mapTo(mutableSetOf()) { shoppingListDedupKey(it) }
        val shoppingListIdMap = mutableMapOf<Long, Long>()
        val itemsByList = snapshot.shoppingItems.groupBy { it.shoppingListId }
        val adjustmentsByList = snapshot.shoppingAdjustments.groupBy { it.shoppingListId }

        for (list in snapshot.shoppingLists) {
            val newExpenseCategoryId = list.expenseCategoryId?.let { resolveCategoryId(it) }
            val dedupKey = shoppingListDedupKey(list)
            val existing = existingLists.firstOrNull { shoppingListDedupKey(it) == dedupKey }
            if (existing != null) {
                shoppingListIdMap[list.id] = existing.id
                continue
            }
            val newId = shoppingListDao.insertList(
                list.copy(
                    id = 0,
                    expenseCategoryId = newExpenseCategoryId,
                    expenseTransactionId = null,
                    payableId = null,
                )
            )
            shoppingListIdMap[list.id] = newId
            insertedShoppingLists++

            itemsByList[list.id]?.forEach { item ->
                shoppingListDao.insertItem(
                    item.copy(id = 0, shoppingListId = newId)
                )
            }
            adjustmentsByList[list.id]?.forEach { adj ->
                shoppingListDao.insertAdjustment(
                    adj.copy(id = 0, shoppingListId = newId)
                )
            }
        }
        for (kp in snapshot.knownProducts) {
            val existing = shoppingListDao.findKnownProduct(kp.barcode)
            if (existing == null) {
                shoppingListDao.upsertKnownProduct(kp)
            }
        }
        for (alias in snapshot.productAliases) {
            val existing = shoppingListDao.findAlias(alias.normalizedAlias, alias.displayName, alias.barcode)
            if (existing == null) {
                shoppingListDao.insertAlias(alias.copy(id = 0))
            }
        }

        // 7. Transactions dedup (similar a restoreBackup original)
        val existingTransactions = transactionDao.getAll()
        val transactionIdByKey = existingTransactions.associate { it.deduplicationKey(categoryIdMap) to it.id }.toMutableMap()
        val transactionIdMap = mutableMapOf<Long, Long>()
        var skipped = 0
        for (t in snapshot.transactions) {
            val newCategoryId = resolveCategoryId(t.categoryId)
            val newFixedId = t.fixedEntryId?.let { fixedIdMap[it] }
            val newSavingsId = t.savingsGoalId?.let { savingsIdMap[it] }
            val entity = TransactionEntity(
                amountInCents = t.amountInCents,
                type = t.type,
                categoryId = newCategoryId,
                description = t.description,
                dateEpochDay = t.dateEpochDay,
                createdAtEpochMillis = t.createdAtEpochMillis,
                updatedAtEpochMillis = t.updatedAtEpochMillis,
                fixedEntryId = newFixedId,
                savingsGoalId = newSavingsId,
            )
            val key = entity.deduplicationKey(null)
            val existingId = transactionIdByKey[key]
            if (existingId != null) {
                transactionIdMap[t.id] = existingId
                skipped++
                continue
            }
            val newId = transactionDao.insert(entity)
            transactionIdByKey[key] = newId
            transactionIdMap[t.id] = newId
            insertedTransactions++
        }

        // 7b. Pagos quincenales (después de transactions para remapear transactionId)
        val existingPayments = fortnightPaymentDao.getAllPayments()
        val paymentKeySet = existingPayments.mapTo(mutableSetOf()) { paymentDedupKey(it) }
        for (payment in snapshot.fortnightPayments) {
            val newItemId = itemIdMap[payment.itemId] ?: continue
            val newTransactionId = payment.transactionId?.let { transactionIdMap[it] }
            val entity = FortnightPaymentEntity(
                id = 0,
                itemId = newItemId,
                amountInCents = payment.amountInCents,
                dateEpochDay = payment.dateEpochDay,
                transactionId = newTransactionId,
                createdAtEpochMillis = payment.createdAtEpochMillis,
            )
            if (paymentKeySet.add(paymentDedupKey(entity))) {
                fortnightPaymentDao.insertPayment(entity)
                insertedFortnightPayments++
            }
        }

        // 8. PendingEntries (después de shoppingLists y transactions para mapear ids)
        val existingPending = pendingEntryDao.getAll().toMutableList()
        val pendingKeySet = existingPending.mapTo(mutableSetOf()) { pendingDedupKey(it) }
        for (entry in snapshot.pendingEntries) {
            val newCategoryId = resolveCategoryId(entry.categoryId)
            val dedupKey = pendingDedupKey(entry, newCategoryId)
            if (pendingKeySet.contains(dedupKey)) continue
            val newSourceId = entry.sourceShoppingListId?.let { shoppingListIdMap[it] }
            val finalSource = when {
                entry.sourceShoppingListId == null -> null
                newSourceId != null -> newSourceId
                else -> {
                    // Si la lista no se insertó por ser duplicada, intentar resolver como existente
                    val allLists = shoppingListDao.getAllLists()
                    if (allLists.any { it.id == entry.sourceShoppingListId }) entry.sourceShoppingListId else null
                }
            }
            val newEntity = entry.copy(
                id = 0,
                categoryId = newCategoryId,
                transactionId = null,
                sourceShoppingListId = finalSource,
            )
            pendingEntryDao.upsert(newEntity)
            pendingKeySet.add(dedupKey)
            existingPending.add(newEntity)
            insertedPending++
        }

        BackupRestoreResult(
            insertedTransactions = insertedTransactions,
            insertedCategories = insertedCategories,
            insertedFixedEntries = insertedFixed,
            insertedPendingEntries = insertedPending,
            insertedSavingsGoals = insertedSavings,
            insertedShoppingLists = insertedShoppingLists,
            insertedBudgetCycles = insertedBudgetCycles,
            insertedFortnightPlans = insertedFortnightPlans,
            insertedFortnightPayments = insertedFortnightPayments,
            skippedTransactions = skipped,
            isLegacyCsv = false,
        )
    }

    private suspend fun restoreLegacyCsv(movements: List<com.angel.mony.domain.model.BackupMovement>): BackupRestoreResult {
        // Reusa lógica de RoomTransactionRepository.restoreBackup pero sin necesidad de inyectarlo: replicamos
        val categoryKeyToId = mutableMapOf<String, Long>()
        categoryDao.getAll().forEach { entity ->
            categoryKeyToId[categoryKey(entity.name, entity.type)] = entity.id
        }
        suspend fun resolveCategoryId(name: String, type: TransactionType): Long =
            categoryKeyToId.getOrPut(categoryKey(name, type.name)) {
                categoryDao.insert(
                    CategoryEntity(
                        name = name,
                        type = type.name,
                        icon = "label",
                        isActive = true,
                        createdAtEpochMillis = System.currentTimeMillis(),
                    )
                )
            }

        val existingKeys = transactionDao.getAll().mapTo(mutableSetOf()) { it.deduplicationKey(null) }
        var inserted = 0
        movements.forEach { movement ->
            val categoryId = resolveCategoryId(movement.categoryName, movement.type)
            val entity = TransactionEntity(
                amountInCents = movement.amountInCents,
                type = movement.type.name,
                categoryId = categoryId,
                description = movement.description,
                dateEpochDay = movement.date.toEpochDay(),
                createdAtEpochMillis = System.currentTimeMillis(),
                updatedAtEpochMillis = System.currentTimeMillis(),
                fixedEntryId = null,
                savingsGoalId = null,
            )
            val key = entity.deduplicationKey(null)
            if (existingKeys.add(key)) {
                transactionDao.insert(entity)
                inserted++
            }
        }
        return BackupRestoreResult(
            insertedTransactions = inserted,
            skippedTransactions = movements.size - inserted,
            isLegacyCsv = true,
        )
    }

    private fun categoryKey(name: String, type: String): String =
        "${type.lowercase()}|${name.trim().lowercase()}"

    private fun fixedDedupKey(e: com.angel.mony.data.local.entity.FixedEntryEntity, overCategoryId: Long? = null): String =
        listOf(
            e.type,
            e.description.trim().lowercase(),
            e.amountInCents.toString(),
            (overCategoryId ?: e.categoryId).toString(),
        ).joinToString("|")

    private fun pendingDedupKey(p: com.angel.mony.data.local.entity.PendingEntryEntity, overCategoryId: Long? = null): String =
        listOf(
            p.type,
            p.description.trim().lowercase(),
            p.amountInCents.toString(),
            p.dateEpochDay.toString(),
            (overCategoryId ?: p.categoryId).toString(),
        ).joinToString("|")

    private fun shoppingListDedupKey(l: ShoppingListEntity): String =
        listOf(
            l.name.trim().lowercase(),
            l.status,
            l.createdAtEpochMillis.toString(),
        ).joinToString("|")

    private fun templateDedupKey(
        t: com.angel.mony.data.local.entity.FortnightTemplateEntity,
        overCategoryId: Long? = null,
    ): String =
        listOf(
            t.description.trim().lowercase(),
            t.type,
            (overCategoryId ?: t.categoryId).toString(),
        ).joinToString("|")

    private fun paymentDedupKey(p: FortnightPaymentEntity): String =
        listOf(
            p.itemId.toString(),
            p.dateEpochDay.toString(),
            p.amountInCents.toString(),
        ).joinToString("|")

    private fun TransactionEntity.deduplicationKey(categoryMap: Map<Long, Long>?): String =
        listOf(
            dateEpochDay.toString(),
            amountInCents.toString(),
            type,
            (categoryMap?.get(categoryId) ?: categoryId).toString(),
            description?.trim()?.lowercase().orEmpty(),
        ).joinToString("|")
}
