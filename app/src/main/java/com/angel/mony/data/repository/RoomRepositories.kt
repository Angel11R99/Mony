package com.angel.mony.data.repository

import com.angel.mony.data.local.dao.CategoryDao
import com.angel.mony.data.local.dao.BudgetConfigDao
import com.angel.mony.data.local.dao.BudgetCycleDao
import com.angel.mony.data.local.dao.ExpenseFundingDao
import com.angel.mony.data.local.dao.FortnightPaymentDao
import com.angel.mony.data.local.dao.FortnightPlanDao
import com.angel.mony.data.local.dao.FortnightTemplateDao
import com.angel.mony.data.local.dao.TransactionDao
import com.angel.mony.data.local.dao.FixedEntryDao
import com.angel.mony.data.local.dao.PendingEntryDao
import com.angel.mony.data.local.dao.SavingsGoalDao
import com.angel.mony.data.local.dao.ShoppingListDao
import com.angel.mony.data.local.entity.BudgetConfigEntity
import com.angel.mony.data.local.entity.BudgetCycleEntity
import com.angel.mony.data.local.entity.CategoryEntity
import com.angel.mony.data.local.entity.ExpenseFundingEntity
import com.angel.mony.data.local.entity.FortnightPaymentEntity
import com.angel.mony.data.local.entity.TransactionEntity
import com.angel.mony.data.local.entity.SavingsGoalEntity
import com.angel.mony.data.local.entity.KnownProductEntity
import com.angel.mony.data.local.entity.PendingEntryEntity
import com.angel.mony.data.local.entity.ProductRecognitionAliasEntity
import com.angel.mony.data.local.database.FinanceDatabase
import com.angel.mony.data.mapper.toDomain
import com.angel.mony.data.mapper.toEntity
import com.angel.mony.core.time.toKotlinInstant
import com.angel.mony.core.time.toKotlinLocalDate
import com.angel.mony.core.time.toJavaInstant
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.ExpenseFunding
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.defaultCycleSchedules
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FortnightPaymentCheck
import com.angel.mony.domain.model.FortnightPlan
import com.angel.mony.domain.model.FortnightPlanDetails
import com.angel.mony.domain.model.FortnightPlanItem
import com.angel.mony.domain.model.FortnightPlanStatus
import com.angel.mony.domain.model.FortnightPlanSummary
import com.angel.mony.domain.model.FortnightTemplate
import com.angel.mony.domain.model.evaluateFortnightPayment
import com.angel.mony.domain.model.fortnightPlanSummaries
import com.angel.mony.domain.model.toPaymentTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.ShoppingAdjustment
import com.angel.mony.domain.model.ShoppingList
import com.angel.mony.domain.model.ShoppingListDetails
import com.angel.mony.domain.model.ShoppingListItem
import com.angel.mony.domain.model.ShoppingListOverview
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.ShoppingPaymentMethod
import com.angel.mony.domain.model.PendingType
import com.angel.mony.domain.model.normalizeProductName
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.ExpenseFundingRepository
import com.angel.mony.domain.repository.TransactionRepository
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.domain.repository.PendingEntryRepository
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.repository.FinalizePurchaseResult
import com.angel.mony.domain.repository.FortnightMutationResult
import com.angel.mony.domain.repository.FortnightPaymentResult
import com.angel.mony.domain.repository.FortnightRepository
import com.angel.mony.domain.repository.ShoppingListRepository
import com.angel.mony.domain.repository.ShoppingMutationResult
import com.angel.mony.domain.repository.TicketProductUpdate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.datetime.LocalDate as KotlinLocalDate

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val fixedEntryDao: FixedEntryDao,
    private val categoryDao: CategoryDao,
    private val database: FinanceDatabase,
    private val shoppingListDao: ShoppingListDao,
    private val pendingEntryDao: PendingEntryDao,
    private val fortnightPaymentDao: FortnightPaymentDao,
    private val expenseFundingDao: ExpenseFundingDao,
) : TransactionRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeByPeriod(period: DateRange) = dao.observeByPeriod(
        period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()
    ).map { items -> items.map { it.toDomain() } }
    override fun observeBySavingsGoal(goalId: Long) =
        dao.observeBySavingsGoal(goalId).map { items -> items.map { it.toDomain() } }
    override suspend fun getRecent(limit: Int) = dao.getRecent(limit).map { it.toDomain() }
    override suspend fun get(id: Long) = dao.get(id)?.toDomain()
    override suspend fun create(transaction: FinanceTransaction) = dao.insert(transaction.toEntity())
    override suspend fun update(transaction: FinanceTransaction) {
        check(shoppingListDao.findListIdByExpenseTransaction(transaction.id) == null) {
            "Este gasto pertenece a una lista finalizada y no se puede editar."
        }
        check(pendingEntryDao.findByTransactionId(transaction.id)?.sourceShoppingListId == null) {
            "Este gasto pertenece a una compra a crédito y no se puede editar."
        }
        check(fortnightPaymentDao.findItemIdByTransaction(transaction.id) == null) {
            "Este gasto pertenece a un ciclo y no se puede editar."
        }
        dao.update(transaction.toEntity())
    }
    override suspend fun delete(id: Long) = database.withTransaction {
        check(shoppingListDao.findListIdByExpenseTransaction(id) == null) {
            "Este gasto pertenece a una lista finalizada y no se puede eliminar."
        }
        check(pendingEntryDao.findByTransactionId(id)?.sourceShoppingListId == null) {
            "Este gasto pertenece a una compra a crédito y no se puede eliminar."
        }
        check(fortnightPaymentDao.findItemIdByTransaction(id) == null) {
            "Este gasto pertenece a un ciclo y no se puede eliminar."
        }
        val deleted = dao.get(id)
        dao.delete(id)
        val fixedEntryId = deleted?.let {
            it.fixedEntryId ?: fixedEntryDao.findIdByLastAddedAt(it.createdAtEpochMillis)
        }
        fixedEntryId?.let {
            val latest = dao.latestForFixedEntry(it)
            fixedEntryDao.updateLastAdded(
                id = it,
                addedAt = latest?.createdAtEpochMillis,
                date = latest?.dateEpochDay,
            )
        }
        Unit
    }

    override suspend fun duplicate(id: Long): Long? {
        val original = dao.get(id) ?: return null
        val now = Instant.now()
        return dao.insert(
            original.copy(
                id = 0,
                dateEpochDay = LocalDate.now().toEpochDay(),
                createdAtEpochMillis = now.toEpochMilli(),
                updatedAtEpochMillis = now.toEpochMilli(),
                fixedEntryId = null,
                savingsGoalId = null,
            )
        )
    }

    override suspend fun createWithFunding(
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult = database.withTransaction {
        require(transaction.amountInCents > 0) { "El monto debe ser mayor que cero" }
        val normalized = transaction.copy(description = transaction.description?.trim()?.takeIf(String::isNotEmpty))

        // If not an expense, just create normally
        if (normalized.type != TransactionType.EXPENSE) {
            val id = dao.insert(normalized.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(id, null)
        }

        // For expenses, we need to evaluate funding
        // Get budget config to determine period
        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, normalized.date)

        // Calculate available before this expense
        val transactions = dao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountInCents }

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val remainingAfterExpense = availableBeforeExpense - normalized.amountInCents

        if (remainingAfterExpense >= 0) {
            val id = dao.insert(normalized.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(id, null)
        }

        val overflowInCents = -remainingAfterExpense

        if (fundingSourceDescription == null || fundingSourceDescription.trim().isEmpty()) {
            return@withTransaction ExpenseCreationResult.RequiresFundingSource(
                overflowInCents = overflowInCents,
                availableBeforeExpenseInCents = availableBeforeExpense,
                expenseAmountInCents = normalized.amountInCents,
                transaction = normalized,
            )
        }

        // Create both transaction and funding atomically
        val id = dao.insert(normalized.toEntity())
        val funding = ExpenseFunding(
            transactionId = id,
            amountInCents = overflowInCents,
            sourceDescription = fundingSourceDescription.trim(),
            createdAt = Instant.now().toKotlinInstant(),
            updatedAt = Instant.now().toKotlinInstant(),
        )
        expenseFundingDao.insert(funding.toEntity(normalized.date.toEpochDays().toLong()))
        ExpenseCreationResult.Saved(id, funding)
    }

    override suspend fun updateWithFunding(
        transaction: FinanceTransaction,
        existingTransactionId: Long,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult = database.withTransaction {
        require(transaction.amountInCents > 0) { "El monto debe ser mayor que cero" }
        val normalized = transaction.copy(description = transaction.description?.trim()?.takeIf(String::isNotEmpty))

        // If not an expense, just update normally
        if (normalized.type != TransactionType.EXPENSE) {
            dao.update(normalized.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(normalized.id, null)
        }

        // Get the existing transaction
        val existing = dao.get(existingTransactionId)
            ?: throw IllegalArgumentException("Transacción no encontrada: $existingTransactionId")

        // Get budget config to determine period
        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, normalized.date)

        // Calculate available before this expense (excluding the existing transaction)
        val transactions = dao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .filterNot { it.id == existingTransactionId }
            .sumOf { it.amountInCents }

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val remainingAfterExpense = availableBeforeExpense - normalized.amountInCents

        // Check existing funding
        val existingFunding = expenseFundingDao.getByTransaction(existingTransactionId)?.toDomain()

        if (remainingAfterExpense >= 0) {
            // No overflow needed, delete existing funding if any
            if (existingFunding != null) {
                expenseFundingDao.deleteByTransaction(existingTransactionId)
            }
            dao.update(normalized.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(normalized.id, null)
        }

        val overflowInCents = -remainingAfterExpense

        if (fundingSourceDescription == null || fundingSourceDescription.trim().isEmpty()) {
            return@withTransaction ExpenseCreationResult.RequiresFundingSource(
                overflowInCents = overflowInCents,
                availableBeforeExpenseInCents = availableBeforeExpense,
                expenseAmountInCents = normalized.amountInCents,
                transaction = normalized,
            )
        }

        // Update both transaction and funding atomically
        dao.update(normalized.toEntity())
        val funding = ExpenseFunding(
            id = existingFunding?.id ?: 0,
            transactionId = existingTransactionId,
            amountInCents = overflowInCents,
            sourceDescription = fundingSourceDescription.trim(),
            createdAt = existingFunding?.createdAt ?: Instant.now().toKotlinInstant(),
            updatedAt = Instant.now().toKotlinInstant(),
        )
        if (existingFunding != null) {
            expenseFundingDao.update(funding.toEntity(normalized.date.toEpochDays().toLong()))
        } else {
            expenseFundingDao.insert(funding.toEntity(normalized.date.toEpochDays().toLong()))
        }
        ExpenseCreationResult.Saved(existingTransactionId, funding)
    }

    override suspend fun restoreBackup(movements: List<BackupMovement>): Int = database.withTransaction {
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
                        icon = DEFAULT_RESTORED_CATEGORY_ICON,
                        isActive = true,
                        createdAtEpochMillis = System.currentTimeMillis(),
                    )
                )
            }

        val existingKeys = dao.getAll().mapTo(mutableSetOf()) { it.deduplicationKey() }
        var inserted = 0
        movements.forEach { movement ->
            val categoryId = resolveCategoryId(movement.categoryName, movement.type)
            val entity = TransactionEntity(
                amountInCents = movement.amountInCents,
                type = movement.type.name,
                categoryId = categoryId,
                description = movement.description,
                dateEpochDay = movement.date.toEpochDays().toLong(),
                createdAtEpochMillis = System.currentTimeMillis(),
                updatedAtEpochMillis = System.currentTimeMillis(),
                fixedEntryId = null,
                savingsGoalId = null,
            )
            val key = entity.deduplicationKey()
            if (existingKeys.add(key)) {
                dao.insert(entity)
                inserted++
            }
        }
        inserted
    }

    private fun TransactionEntity.deduplicationKey(): String =
        listOf(
            dateEpochDay.toString(),
            amountInCents.toString(),
            type,
            categoryId.toString(),
            description?.trim()?.lowercase().orEmpty(),
        ).joinToString("|")

    private fun categoryKey(name: String, type: String): String =
        "${type.lowercase()}|${name.trim().lowercase()}"

    private companion object {
        const val DEFAULT_RESTORED_CATEGORY_ICON = "label"
    }
}

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeActive(type: TransactionType): Flow<List<Category>> =
        dao.observeActive(type.name).map { items -> items.map { it.toDomain() } }
    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeUsedCategoryIds(): Flow<Set<Long>> =
        dao.observeUsedCategoryIds().map { ids -> ids.toSet() }

    override suspend fun create(name: String, type: TransactionType, budgetLimitInCents: Long?) {
        dao.insert(
            CategoryEntity(
                name = name.trim(),
                type = type.name,
                icon = DEFAULT_CATEGORY_ICON,
                createdAtEpochMillis = System.currentTimeMillis(),
                budgetLimitInCents = budgetLimitInCents,
            )
        )
    }

    override suspend fun update(id: Long, name: String, budgetLimitInCents: Long?) =
        dao.update(id = id, name = name.trim(), budgetLimitInCents = budgetLimitInCents)

    override suspend fun setActive(id: Long, isActive: Boolean) = dao.setActive(id, isActive)

    override suspend fun deleteIfUnused(id: Long): Boolean {
        val usedIds = dao.observeUsedCategoryIds().first()
        if (id in usedIds) return false
        dao.deleteById(id)
        return true
    }

    private companion object {
        const val DEFAULT_CATEGORY_ICON = "label"
    }
}

class RoomFixedEntryRepository @Inject constructor(
    private val dao: FixedEntryDao,
    private val transactionDao: TransactionDao,
    private val expenseFundingDao: ExpenseFundingDao,
    private val database: FinanceDatabase,
) : FixedEntryRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override suspend fun save(entry: com.angel.mony.domain.model.FixedEntry) =
        dao.upsert(entry.toEntity())
    override suspend fun post(
        entry: com.angel.mony.domain.model.FixedEntry,
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult = database.withTransaction {
        require(transaction.amountInCents > 0) { "El monto debe ser mayor que cero" }
        val normalized = transaction.copy(description = transaction.description?.trim()?.takeIf(String::isNotEmpty))

        if (normalized.type != TransactionType.EXPENSE) {
            val id = transactionDao.insert(normalized.toEntity())
            dao.upsert(entry.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(id, null)
        }

        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, normalized.date)

        val transactions = transactionDao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountInCents }

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val remainingAfterExpense = availableBeforeExpense - normalized.amountInCents

        if (remainingAfterExpense >= 0) {
            val id = transactionDao.insert(normalized.toEntity())
            dao.upsert(entry.toEntity())
            return@withTransaction ExpenseCreationResult.Saved(id, null)
        }

        val overflowInCents = -remainingAfterExpense

        if (fundingSourceDescription == null || fundingSourceDescription.trim().isEmpty()) {
            return@withTransaction ExpenseCreationResult.RequiresFundingSource(
                overflowInCents = overflowInCents,
                availableBeforeExpenseInCents = availableBeforeExpense,
                expenseAmountInCents = normalized.amountInCents,
                transaction = normalized,
            )
        }

        val id = transactionDao.insert(normalized.toEntity())
        dao.upsert(entry.toEntity())
        val funding = ExpenseFunding(
            transactionId = id,
            amountInCents = overflowInCents,
            sourceDescription = fundingSourceDescription.trim(),
            createdAt = Instant.now().toKotlinInstant(),
            updatedAt = Instant.now().toKotlinInstant(),
        )
        expenseFundingDao.insert(funding.toEntity(normalized.date.toEpochDays().toLong()))
        ExpenseCreationResult.Saved(id, funding)
    }
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomPendingEntryRepository @Inject constructor(
    private val dao: PendingEntryDao,
    private val transactionDao: TransactionDao,
    private val expenseFundingDao: ExpenseFundingDao,
    private val database: FinanceDatabase,
) : PendingEntryRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override suspend fun get(id: Long) = dao.get(id)?.toDomain()
    override suspend fun save(entry: PendingEntry): Long {
        check(entry.sourceShoppingListId == null) { "Esta obligación debe editarse desde la compra vinculada." }
        return dao.upsert(entry.toEntity())
    }
    override suspend fun complete(
        entry: PendingEntry,
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult = database.withTransaction {
        require(transaction.amountInCents > 0) { "El monto debe ser mayor que cero" }
        val normalized = transaction.copy(description = transaction.description?.trim()?.takeIf(String::isNotEmpty))

        if (normalized.type != TransactionType.EXPENSE) {
            val transactionId = transactionDao.insert(normalized.toEntity())
            dao.upsert(
                entry.copy(
                    isDone = true,
                    doneAt = transaction.createdAt,
                    transactionId = transactionId,
                ).toEntity()
            )
            return@withTransaction ExpenseCreationResult.Saved(transactionId, null)
        }

        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, normalized.date)

        val transactions = transactionDao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountInCents }

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val remainingAfterExpense = availableBeforeExpense - normalized.amountInCents

        if (remainingAfterExpense >= 0) {
            val transactionId = transactionDao.insert(normalized.toEntity())
            dao.upsert(
                entry.copy(
                    isDone = true,
                    doneAt = transaction.createdAt,
                    transactionId = transactionId,
                ).toEntity()
            )
            return@withTransaction ExpenseCreationResult.Saved(transactionId, null)
        }

        val overflowInCents = -remainingAfterExpense

        if (fundingSourceDescription == null || fundingSourceDescription.trim().isEmpty()) {
            return@withTransaction ExpenseCreationResult.RequiresFundingSource(
                overflowInCents = overflowInCents,
                availableBeforeExpenseInCents = availableBeforeExpense,
                expenseAmountInCents = normalized.amountInCents,
                transaction = normalized,
            )
        }

        val transactionId = transactionDao.insert(normalized.toEntity())
        dao.upsert(
            entry.copy(
                isDone = true,
                doneAt = transaction.createdAt,
                transactionId = transactionId,
            ).toEntity()
        )
        val funding = ExpenseFunding(
            transactionId = transactionId,
            amountInCents = overflowInCents,
            sourceDescription = fundingSourceDescription.trim(),
            createdAt = Instant.now().toKotlinInstant(),
            updatedAt = Instant.now().toKotlinInstant(),
        )
        expenseFundingDao.insert(funding.toEntity(normalized.date.toEpochDays().toLong()))
        ExpenseCreationResult.Saved(transactionId, funding)
    }
    override suspend fun reopen(entry: PendingEntry) {
        database.withTransaction {
            entry.transactionId?.let { transactionDao.delete(it) }
            entry.transactionId?.let { expenseFundingDao.deleteByTransaction(it) }
            dao.upsert(entry.copy(isDone = false, doneAt = null, transactionId = null).toEntity())
        }
    }
    override suspend fun delete(id: Long) {
        check(dao.get(id)?.sourceShoppingListId == null) { "Esta obligación debe eliminarse desde la compra vinculada." }
        dao.delete(id)
    }
}

class RoomSavingsRepository @Inject constructor(
    private val dao: SavingsGoalDao,
    private val database: FinanceDatabase,
) : SavingsRepository {
    override fun observeGoals(): Flow<List<SavingsGoalProgress>> =
        dao.observeAllWithSaved().map { rows -> rows.map { it.toDomain() } }

    override suspend fun create(name: String, targetAmountInCents: Long): Long =
        dao.insert(
            SavingsGoalEntity(
                name = name.trim(),
                targetAmountInCents = targetAmountInCents,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        )

    override suspend fun update(id: Long, name: String, targetAmountInCents: Long) =
        dao.update(id, name.trim(), targetAmountInCents)

    override suspend fun complete(id: Long) =
        dao.complete(id, System.currentTimeMillis())

    override suspend fun reopen(id: Long) =
        dao.reopen(id)

    override suspend fun delete(id: Long) {
        database.withTransaction {
            dao.unlinkTransactions(id)
            dao.deleteById(id)
        }
    }
}

class RoomShoppingListRepository @Inject constructor(
    private val dao: ShoppingListDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val pendingEntryDao: PendingEntryDao,
    private val database: FinanceDatabase,
) : ShoppingListRepository {
    override fun observeLists(): Flow<List<ShoppingList>> =
        dao.observeLists().map { lists -> lists.map { it.toDomain() } }

    override fun observeListOverviews(): Flow<List<ShoppingListOverview>> =
        dao.observeListOverviews().map { rows ->
            rows.map { ShoppingListOverview(it.list.toDomain(), it.itemCount, it.totalInCents) }
        }

    override fun observeDetails(listId: Long): Flow<ShoppingListDetails?> = combine(
        dao.observeList(listId),
        dao.observeItems(listId),
        dao.observeAdjustments(listId),
    ) { list, items, adjustments ->
        list?.let {
            ShoppingListDetails(
                list = it.toDomain(),
                items = items.map { item -> item.toDomain() },
                adjustments = adjustments.map { adjustment -> adjustment.toDomain() },
            )
        }
    }

    override suspend fun getDetails(listId: Long): ShoppingListDetails? {
        val list = dao.getList(listId) ?: return null
        return ShoppingListDetails(
            list = list.toDomain(),
            items = dao.getItems(listId).map { it.toDomain() },
            adjustments = dao.getAdjustments(listId).map { it.toDomain() },
        )
    }

    override suspend fun create(list: ShoppingList): Long {
        require(list.status != ShoppingListStatus.COMPLETED) { "A completed list must be finalized" }
        return dao.insertList(
            list.copy(
                id = 0,
                name = list.name.trim(),
                expenseTransactionId = null,
                completedAt = null,
            ).toEntity()
        )
    }

    override suspend fun update(list: ShoppingList): ShoppingMutationResult = database.withTransaction {
        val current = dao.getList(list.id) ?: return@withTransaction ShoppingMutationResult.NotFound
        dao.updateList(
            list.copy(
                name = list.name.trim(),
                status = if (current.status == ShoppingListStatus.COMPLETED.name) {
                    ShoppingListStatus.COMPLETED
                } else {
                    list.status
                },
                expenseTransactionId = current.expenseTransactionId,
                payableId = current.payableId,
                purchaseDate = current.purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
                paymentMethod = current.paymentMethod?.let(ShoppingPaymentMethod::valueOf),
                expenseCategoryId = current.expenseCategoryId,
                createdAt = current.toDomain().createdAt,
                completedAt = current.completedAtEpochMillis?.let(Instant::ofEpochMilli),
            ).toEntity()
        )
        if (current.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(list.id)
        ShoppingMutationResult.Success(list.id)
    }

    override suspend fun delete(listId: Long): ShoppingMutationResult = database.withTransaction {
        val list = dao.getList(listId) ?: return@withTransaction ShoppingMutationResult.NotFound
        if (list.status == ShoppingListStatus.COMPLETED.name) {
            return@withTransaction ShoppingMutationResult.CompletedList
        }
        dao.deleteList(listId)
        ShoppingMutationResult.Success(listId)
    }

    override suspend fun saveItem(item: ShoppingListItem): ShoppingMutationResult = database.withTransaction {
        val list = dao.getList(item.shoppingListId) ?: return@withTransaction ShoppingMutationResult.NotFound
        if (item.id == 0L) {
            val id = dao.insertItem(item.copy(name = item.name.trim()).toEntity())
            dao.touchList(item.shoppingListId, Instant.now().toEpochMilli())
            if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(item.shoppingListId)
            ShoppingMutationResult.Success(id)
        } else {
            val current = dao.getItem(item.id)
            if (current == null || current.shoppingListId != item.shoppingListId) {
                return@withTransaction ShoppingMutationResult.NotFound
            }
            dao.updateItem(item.copy(name = item.name.trim()).toEntity())
            dao.touchList(item.shoppingListId, Instant.now().toEpochMilli())
            if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(item.shoppingListId)
            ShoppingMutationResult.Success(item.id)
        }
    }

    override suspend fun deleteItem(itemId: Long): ShoppingMutationResult = database.withTransaction {
        val item = dao.getItem(itemId) ?: return@withTransaction ShoppingMutationResult.NotFound
        val list = dao.getList(item.shoppingListId) ?: return@withTransaction ShoppingMutationResult.NotFound
        dao.deleteItem(itemId)
        dao.touchList(item.shoppingListId, Instant.now().toEpochMilli())
        if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(item.shoppingListId)
        ShoppingMutationResult.Success(itemId)
    }

    override suspend fun saveAdjustment(adjustment: ShoppingAdjustment): ShoppingMutationResult =
        database.withTransaction {
            val list = dao.getList(adjustment.shoppingListId)
                ?: return@withTransaction ShoppingMutationResult.NotFound
            val result = if (adjustment.id == 0L) {
                val id = dao.insertAdjustment(adjustment.copy(name = adjustment.name.trim()).toEntity())
                dao.touchList(adjustment.shoppingListId, Instant.now().toEpochMilli())
                ShoppingMutationResult.Success(id)
            } else {
                val current = dao.getAdjustment(adjustment.id)
                if (current == null || current.shoppingListId != adjustment.shoppingListId) {
                    return@withTransaction ShoppingMutationResult.NotFound
                }
                dao.updateAdjustment(adjustment.copy(name = adjustment.name.trim()).toEntity())
                dao.touchList(adjustment.shoppingListId, Instant.now().toEpochMilli())
                ShoppingMutationResult.Success(adjustment.id)
            }
            if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(adjustment.shoppingListId)
            result
        }

    override suspend fun saveAdjustments(adjustments: List<ShoppingAdjustment>): ShoppingMutationResult =
        database.withTransaction {
            val listId = adjustments.firstOrNull()?.shoppingListId
                ?: return@withTransaction ShoppingMutationResult.NotFound
            if (adjustments.any { it.shoppingListId != listId }) {
                return@withTransaction ShoppingMutationResult.NotFound
            }
            val list = dao.getList(listId) ?: return@withTransaction ShoppingMutationResult.NotFound
            adjustments.forEach { adjustment ->
                if (adjustment.id == 0L) {
                    dao.insertAdjustment(adjustment.copy(name = adjustment.name.trim()).toEntity())
                } else {
                    val current = dao.getAdjustment(adjustment.id)
                    if (current == null || current.shoppingListId != listId) {
                        return@withTransaction ShoppingMutationResult.NotFound
                    }
                    dao.updateAdjustment(adjustment.copy(name = adjustment.name.trim()).toEntity())
                }
            }
            dao.touchList(listId, Instant.now().toEpochMilli())
            if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(listId)
            ShoppingMutationResult.Success(listId)
        }

    override suspend fun deleteAdjustment(adjustmentId: Long): ShoppingMutationResult =
        database.withTransaction {
            val adjustment = dao.getAdjustment(adjustmentId)
                ?: return@withTransaction ShoppingMutationResult.NotFound
            val list = dao.getList(adjustment.shoppingListId)
                ?: return@withTransaction ShoppingMutationResult.NotFound
            dao.deleteAdjustment(adjustmentId)
            dao.touchList(adjustment.shoppingListId, Instant.now().toEpochMilli())
            if (list.status == ShoppingListStatus.COMPLETED.name) syncCompletedPurchase(adjustment.shoppingListId)
            ShoppingMutationResult.Success(adjustmentId)
        }

    override suspend fun findKnownProduct(barcode: String) =
        dao.findKnownProduct(barcode.trim())?.toDomain()

    override suspend fun findLearnedNames(detectedText: String): List<String> =
        dao.findAliases(normalizeProductName(detectedText)).map { it.displayName }.distinct()

    override suspend fun applyTicketReview(
        listId: Long,
        products: List<TicketProductUpdate>,
        adjustments: List<ShoppingAdjustment>,
    ): ShoppingMutationResult = database.withTransaction {
        val list = dao.getList(listId) ?: return@withTransaction ShoppingMutationResult.NotFound
        if (list.status == ShoppingListStatus.COMPLETED.name) return@withTransaction ShoppingMutationResult.CompletedList
        require(products.mapNotNull { it.itemId }.let { it.size == it.distinct().size })
        val now = Instant.now()
        products.forEach { draft ->
            require(draft.displayName.isNotBlank() && draft.quantity >= 1 && draft.unitPriceInCents >= 0)
            val existing = draft.itemId?.let { dao.getItem(it) }
            val item = if (existing == null) {
                com.angel.mony.data.local.entity.ShoppingListItemEntity(
                    shoppingListId = listId,
                    name = draft.displayName.trim(),
                    quantity = draft.quantity,
                    estimatedUnitPriceInCents = null,
                    actualUnitPriceInCents = draft.unitPriceInCents,
                    barcode = null,
                    isPurchased = true,
                    isIdentified = false,
                    notes = null,
                    createdAtEpochMillis = now.toEpochMilli(),
                    updatedAtEpochMillis = now.toEpochMilli(),
                )
            } else existing.copy(
                name = draft.displayName.trim(),
                quantity = draft.quantity,
                actualUnitPriceInCents = draft.unitPriceInCents,
                isPurchased = true,
                updatedAtEpochMillis = now.toEpochMilli(),
            )
            if (existing == null) dao.insertItem(item) else dao.updateItem(item)
            learnAlias(draft.detectedText, draft.displayName, item.barcode, now)
        }
        adjustments.forEach { dao.insertAdjustment(it.copy(id = 0, shoppingListId = listId).toEntity()) }
        dao.touchList(listId, now.toEpochMilli())
        ShoppingMutationResult.Success(listId)
    }

    override suspend fun duplicate(listId: Long): Long? = database.withTransaction {
        val source = dao.getList(listId) ?: return@withTransaction null
        val sourceItems = dao.getItems(listId)
        val sourceAdjustments = dao.getAdjustments(listId)
        val now = Instant.now()
        val newListId = dao.insertList(
            source.copy(
                id = 0,
                status = ShoppingListStatus.PENDING.name,
                expenseTransactionId = null,
                payableId = null,
                purchaseDateEpochDay = null,
                paymentMethod = null,
                expenseCategoryId = null,
                createdAtEpochMillis = now.toEpochMilli(),
                updatedAtEpochMillis = now.toEpochMilli(),
                completedAtEpochMillis = null,
            )
        )
        sourceItems.forEach { item ->
            dao.insertItem(
                item.copy(
                    id = 0,
                    shoppingListId = newListId,
                    actualUnitPriceInCents = null,
                    isPurchased = false,
                    createdAtEpochMillis = now.toEpochMilli(),
                    updatedAtEpochMillis = now.toEpochMilli(),
                )
            )
        }
        sourceAdjustments.forEach { adjustment ->
            dao.insertAdjustment(
                adjustment.copy(
                    id = 0,
                    shoppingListId = newListId,
                    createdAtEpochMillis = now.toEpochMilli(),
                )
            )
        }
        newListId
    }

    override suspend fun finalizePurchase(
        listId: Long,
        categoryId: Long,
        date: LocalDate,
        paymentMethod: ShoppingPaymentMethod,
        allowMissingPrices: Boolean,
    ): FinalizePurchaseResult = database.withTransaction {
        val list = dao.getList(listId) ?: return@withTransaction FinalizePurchaseResult.ListNotFound
        if (list.status == ShoppingListStatus.COMPLETED.name) {
            val transactionId = dao.getExpenseTransaction(listId)?.id ?: list.expenseTransactionId
            return@withTransaction FinalizePurchaseResult.AlreadyCompleted(transactionId)
        }
        val category = categoryDao.get(categoryId)
        if (category == null || category.type != TransactionType.EXPENSE.name || !category.isActive) {
            return@withTransaction FinalizePurchaseResult.InvalidExpenseCategory
        }

        val purchaseItems = dao.getItems(listId)
        val missingPrices = purchaseItems.filter { it.actualUnitPriceInCents == null }.map { it.id }
        if (missingPrices.isNotEmpty() && !allowMissingPrices) {
            return@withTransaction FinalizePurchaseResult.MissingActualPrices(missingPrices)
        }
        val details = ShoppingListDetails(
            list = list.toDomain(),
            items = purchaseItems.map { it.toDomain() },
            adjustments = dao.getAdjustments(listId).map { it.toDomain() },
        )
        val total = try {
            details.finalizableTotalInCents
        } catch (_: ArithmeticException) {
            return@withTransaction FinalizePurchaseResult.CalculationOverflow
        }
        if (total <= 0) return@withTransaction FinalizePurchaseResult.TotalNotPositive

        val now = Instant.now()
        if (dao.markCompleted(listId, now.toEpochMilli()) == 0) {
            val transactionId = dao.getExpenseTransaction(listId)?.id
            return@withTransaction FinalizePurchaseResult.AlreadyCompleted(transactionId)
        }
        dao.markAllItemsPurchased(listId, now.toEpochMilli())
        dao.updateList(list.copy(
            status = ShoppingListStatus.COMPLETED.name,
            purchaseDateEpochDay = date.toEpochDay(),
            paymentMethod = paymentMethod.name,
            expenseCategoryId = categoryId,
            completedAtEpochMillis = now.toEpochMilli(),
            updatedAtEpochMillis = now.toEpochMilli(),
        ))
        val financialId = syncCompletedPurchase(listId)
        purchaseItems.forEach { item ->
            val barcode = item.barcode?.trim().orEmpty()
            if (barcode.isNotEmpty()) {
                dao.upsertKnownProduct(
                    KnownProductEntity(
                        barcode = barcode,
                        name = item.name,
                        lastPriceInCents = item.actualUnitPriceInCents,
                        lastUsedAtEpochMillis = now.toEpochMilli(),
                    )
                )
            }
        }
        FinalizePurchaseResult.Completed(financialId, total)
    }

    override suspend fun updatePurchaseSettings(
        listId: Long,
        categoryId: Long,
        date: LocalDate,
        paymentMethod: ShoppingPaymentMethod,
    ): ShoppingMutationResult = database.withTransaction {
        val list = dao.getList(listId) ?: return@withTransaction ShoppingMutationResult.NotFound
        if (list.status != ShoppingListStatus.COMPLETED.name) return@withTransaction ShoppingMutationResult.NotFound
        val category = categoryDao.get(categoryId)
        if (category == null || category.type != TransactionType.EXPENSE.name || !category.isActive) {
            return@withTransaction ShoppingMutationResult.NotFound
        }
        dao.updateList(list.copy(
            purchaseDateEpochDay = date.toEpochDay(),
            paymentMethod = paymentMethod.name,
            expenseCategoryId = categoryId,
            updatedAtEpochMillis = Instant.now().toEpochMilli(),
        ))
        syncCompletedPurchase(listId)
        ShoppingMutationResult.Success(listId)
    }

    override suspend fun reopen(listId: Long): ShoppingMutationResult = database.withTransaction {
        val list = dao.getList(listId) ?: return@withTransaction ShoppingMutationResult.NotFound
        if (list.status != ShoppingListStatus.COMPLETED.name) {
            return@withTransaction ShoppingMutationResult.NotFound
        }
        list.expenseTransactionId?.let { transactionDao.delete(it) }
        list.payableId?.let { payableId ->
            pendingEntryDao.get(payableId)?.transactionId?.let { transactionDao.delete(it) }
            pendingEntryDao.delete(payableId)
        }
        val now = Instant.now().toEpochMilli()
        dao.reopen(listId, ShoppingListStatus.SHOPPING.name, now)
        ShoppingMutationResult.Success(listId)
    }

    private suspend fun syncCompletedPurchase(listId: Long): Long {
        val list = checkNotNull(dao.getList(listId))
        check(list.status == ShoppingListStatus.COMPLETED.name)
        val categoryId = checkNotNull(list.expenseCategoryId)
        val date = LocalDate.ofEpochDay(checkNotNull(list.purchaseDateEpochDay))
        val method = ShoppingPaymentMethod.valueOf(checkNotNull(list.paymentMethod))
        val details = ShoppingListDetails(
            list.toDomain(),
            dao.getItems(listId).map { it.toDomain() },
            dao.getAdjustments(listId).map { it.toDomain() },
        )
        val total = details.actualTotalInCents
        check(total > 0) { "Completed purchase total must be positive" }
        val now = Instant.now()
        return if (method == ShoppingPaymentMethod.CREDIT) {
            list.expenseTransactionId?.let { transactionDao.delete(it) }
            val currentPayable = list.payableId?.let { pendingEntryDao.get(it) }
                ?: pendingEntryDao.findByShoppingListId(listId)
            var payable = (currentPayable ?: PendingEntryEntity(
                type = PendingType.PAYMENT.name,
                description = list.name,
                amountInCents = total,
                categoryId = categoryId,
                dateEpochDay = date.toEpochDay(),
                reminderMinutesOfDay = null,
                comment = "Compra creada desde Lista",
                isDone = false,
                doneAtEpochMillis = null,
                transactionId = null,
                createdAtEpochMillis = now.toEpochMilli(),
                updatedAtEpochMillis = now.toEpochMilli(),
                sourceShoppingListId = listId,
            )).copy(
                description = list.name,
                amountInCents = total,
                categoryId = categoryId,
                dateEpochDay = date.toEpochDay(),
                updatedAtEpochMillis = now.toEpochMilli(),
                sourceShoppingListId = listId,
            )
            val payableId = if (currentPayable == null) pendingEntryDao.upsert(payable) else {
                pendingEntryDao.upsert(payable)
                payable.id
            }
            if (payable.isDone) {
                val linkedTransaction = payable.transactionId?.let { transactionDao.get(it) }
                if (linkedTransaction == null) {
                    val transactionId = transactionDao.insert(TransactionEntity(
                        amountInCents = total,
                        type = TransactionType.EXPENSE.name,
                        categoryId = categoryId,
                        description = list.name,
                        dateEpochDay = date.toEpochDay(),
                        createdAtEpochMillis = now.toEpochMilli(),
                        updatedAtEpochMillis = now.toEpochMilli(),
                        fixedEntryId = null,
                        savingsGoalId = null,
                    ))
                    payable = payable.copy(transactionId = transactionId)
                    pendingEntryDao.upsert(payable)
                } else {
                    transactionDao.update(linkedTransaction.copy(
                        amountInCents = total,
                        categoryId = categoryId,
                        description = list.name,
                        dateEpochDay = date.toEpochDay(),
                        updatedAtEpochMillis = now.toEpochMilli(),
                    ))
                }
            }
            dao.updateList(list.copy(expenseTransactionId = null, payableId = payableId, updatedAtEpochMillis = now.toEpochMilli()))
            payableId
        } else {
            list.payableId?.let { payableId ->
                pendingEntryDao.get(payableId)?.transactionId?.let { transactionDao.delete(it) }
                pendingEntryDao.delete(payableId)
            }
            val existing = list.expenseTransactionId?.let { transactionDao.get(it) }
            val transaction = (existing ?: TransactionEntity(
                amountInCents = total,
                type = TransactionType.EXPENSE.name,
                categoryId = categoryId,
                description = list.name,
                dateEpochDay = date.toEpochDay(),
                createdAtEpochMillis = now.toEpochMilli(),
                updatedAtEpochMillis = now.toEpochMilli(),
                fixedEntryId = null,
                savingsGoalId = null,
            )).copy(
                amountInCents = total,
                categoryId = categoryId,
                description = list.name,
                dateEpochDay = date.toEpochDay(),
                updatedAtEpochMillis = now.toEpochMilli(),
            )
            val transactionId = if (existing == null) transactionDao.insert(transaction) else {
                transactionDao.update(transaction)
                transaction.id
            }
            dao.updateList(list.copy(expenseTransactionId = transactionId, payableId = null, updatedAtEpochMillis = now.toEpochMilli()))
            transactionId
        }
    }

    private suspend fun learnAlias(detectedText: String, displayName: String, barcode: String?, now: Instant) {
        val normalized = normalizeProductName(detectedText)
        if (normalized.isBlank()) return
        val existing = dao.findAlias(normalized, displayName.trim(), barcode)
        if (existing == null) {
            dao.insertAlias(ProductRecognitionAliasEntity(
                detectedText = detectedText.trim(),
                normalizedAlias = normalized,
                displayName = displayName.trim(),
                barcode = barcode,
                confirmationCount = 1,
                lastUsedAtEpochMillis = now.toEpochMilli(),
            ))
        } else {
            dao.updateAlias(existing.copy(
                detectedText = detectedText.trim(),
                confirmationCount = existing.confirmationCount + 1,
                lastUsedAtEpochMillis = now.toEpochMilli(),
            ))
        }
    }
}

class RoomFortnightRepository @Inject constructor(
    private val templateDao: FortnightTemplateDao,
    private val planDao: FortnightPlanDao,
    private val paymentDao: FortnightPaymentDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val expenseFundingDao: ExpenseFundingDao,
    private val database: FinanceDatabase,
) : FortnightRepository {

    override fun observeTemplates() =
        templateDao.observeTemplates().map { items -> items.map { it.toDomain() } }

    override fun observeActiveTemplates() =
        templateDao.observeActiveTemplates().map { items -> items.map { it.toDomain() } }

    override suspend fun saveTemplate(template: FortnightTemplate): FortnightMutationResult {
        val category = categoryDao.get(template.categoryId)
            ?: return FortnightMutationResult.NotFound
        check(category.isActive) { "La categoría seleccionada ya no está disponible." }
        return database.withTransaction {
            if (template.id == 0L) {
                val id = templateDao.insertTemplate(template.toEntity().copy(id = 0))
                FortnightMutationResult.Success(id)
            } else {
                val existing = templateDao.getTemplate(template.id)
                    ?: return@withTransaction FortnightMutationResult.NotFound
                templateDao.updateTemplate(template.toEntity().copy(id = existing.id))
                FortnightMutationResult.Success(existing.id)
            }
        }
    }

    override suspend fun setTemplateActive(id: Long, isActive: Boolean): FortnightMutationResult =
        database.withTransaction {
            val template = templateDao.getTemplate(id)
                ?: return@withTransaction FortnightMutationResult.NotFound
            templateDao.setTemplateActive(id, isActive, Instant.now().toEpochMilli())
            FortnightMutationResult.Success(template.id)
        }

    override suspend fun deleteTemplate(id: Long): FortnightMutationResult = database.withTransaction {
        val template = templateDao.getTemplate(id)
            ?: return@withTransaction FortnightMutationResult.NotFound
        if (templateDao.countItemsUsingTemplate(id) > 0) {
            return@withTransaction FortnightMutationResult.TemplateInUse
        }
        templateDao.deleteTemplate(id)
        FortnightMutationResult.Success(id)
    }

    override fun observePlanSummaries(): Flow<List<FortnightPlanSummary>> = combine(
        planDao.observePlans(),
        planDao.observeAllItems(),
        paymentDao.observeAllPayments(),
    ) { plans, items, payments ->
        fortnightPlanSummaries(
            plans = plans.map { it.toDomain() },
            items = items.map { it.toDomain() },
            payments = payments.map { it.toDomain() },
        )
    }

    override fun observeDetails(planId: Long): Flow<FortnightPlanDetails?> = combine(
        planDao.observePlan(planId),
        planDao.observeItems(planId),
        paymentDao.observePayments(planId),
    ) { plan, items, payments ->
        val planEntity = plan ?: return@combine null
        FortnightPlanDetails(
            plan = planEntity.toDomain(),
            items = items.map { it.toDomain() },
            payments = payments.map { it.toDomain() },
        )
    }

    override suspend fun getDetails(planId: Long): FortnightPlanDetails? = database.withTransaction {
        val planEntity = planDao.getPlan(planId) ?: return@withTransaction null
        FortnightPlanDetails(
            plan = planEntity.toDomain(),
            items = planDao.getItems(planId).map { it.toDomain() },
            payments = paymentDao.getPayments(planId).map { it.toDomain() },
        )
    }

    override suspend fun findPlanForPeriod(start: KotlinLocalDate, endInclusive: KotlinLocalDate): FortnightPlan? =
        planDao.getPlanForPeriod(start.toEpochDays().toLong(), endInclusive.toEpochDays().toLong())?.toDomain()

    override suspend fun createPlan(plan: FortnightPlan, items: List<FortnightPlanItem>): Long =
        database.withTransaction {
            val now = Instant.now().toKotlinInstant()
            val planId = planDao.insertPlan(
                plan.copy(id = 0, createdAt = now, status = FortnightPlanStatus.OPEN, closedAt = null)
                    .toEntity(),
            )
            items.forEachIndexed { index, item ->
                planDao.insertItem(
                    item.copy(id = 0, planId = planId, position = index, createdAt = now, updatedAt = now).toEntity(),
                )
            }
            planId
        }

    override suspend fun saveItem(item: FortnightPlanItem): FortnightMutationResult =
        database.withTransaction {
            val plan = planDao.getPlan(item.planId)
                ?: return@withTransaction FortnightMutationResult.NotFound
            if (plan.status == FortnightPlanStatus.CLOSED.name) {
                return@withTransaction FortnightMutationResult.ClosedPlan
            }
            val category = categoryDao.get(item.categoryId)
                ?: return@withTransaction FortnightMutationResult.NotFound
            check(category.isActive) { "La categoría seleccionada ya no está disponible." }
            val now = Instant.now().toKotlinInstant()
            if (item.id == 0L) {
                val id = planDao.insertItem(item.copy(id = 0, createdAt = now, updatedAt = now).toEntity())
                FortnightMutationResult.Success(id)
            } else {
                planDao.updateItem(item.copy(updatedAt = now).toEntity())
                FortnightMutationResult.Success(item.id)
            }
        }

    override suspend fun deleteItem(itemId: Long): FortnightMutationResult = database.withTransaction {
        val item = planDao.getItem(itemId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        val plan = planDao.getPlan(item.planId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        if (plan.status == FortnightPlanStatus.CLOSED.name) {
            return@withTransaction FortnightMutationResult.ClosedPlan
        }
        if (paymentDao.countPaymentsForItem(itemId) > 0) {
            return@withTransaction FortnightMutationResult.HasPayments
        }
        planDao.deleteItem(itemId)
        FortnightMutationResult.Success(itemId)
    }

    override suspend fun registerPayment(
        itemId: Long,
        amountInCents: Long,
        date: KotlinLocalDate,
        allowOverpayment: Boolean,
    ): FortnightPaymentResult = database.withTransaction {
        val itemEntity = planDao.getItem(itemId)
            ?: return@withTransaction FortnightPaymentResult.NotFound
        val item = itemEntity.toDomain()
        val planEntity = planDao.getPlan(item.planId)
            ?: return@withTransaction FortnightPaymentResult.NotFound
        if (planEntity.status == FortnightPlanStatus.CLOSED.name) {
            return@withTransaction FortnightPaymentResult.ClosedPlan
        }
        val alreadyPaid = paymentDao.getPaymentsForItem(itemId)
            .sumOf { it.amountInCents }
        when (val check = evaluateFortnightPayment(item.plannedAmountInCents, alreadyPaid, amountInCents)) {
            is FortnightPaymentCheck.InvalidAmount -> return@withTransaction FortnightPaymentResult.InvalidAmount
            is FortnightPaymentCheck.Overpayment -> {
                if (!allowOverpayment) {
                    return@withTransaction FortnightPaymentResult.Overpayment(
                        pendingInCents = check.pendingInCents,
                        excessInCents = check.excessInCents,
                    )
                }
            }
            is FortnightPaymentCheck.Valid -> Unit
        }
        val category = categoryDao.get(item.categoryId)
        if (category == null || !category.isActive || category.type != TransactionType.EXPENSE.name) {
            return@withTransaction FortnightPaymentResult.InvalidCategory
        }

        // Check for expense funding overflow
        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, date)

        val transactions = transactionDao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountInCents }

        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val remainingAfterExpense = availableBeforeExpense - amountInCents

        if (remainingAfterExpense < 0) {
            val overflowInCents = -remainingAfterExpense
            val transaction = item.toPaymentTransaction(amountInCents, date, Instant.now().toKotlinInstant())
            return@withTransaction FortnightPaymentResult.RequiresFundingSource(
                overflowInCents = overflowInCents,
                availableBeforeExpenseInCents = availableBeforeExpense,
                expenseAmountInCents = amountInCents,
                transaction = transaction,
                itemId = itemId,
                amountInCents = amountInCents,
                date = date,
                allowOverpayment = allowOverpayment,
            )
        }

        val now = Instant.now()
        val kotlinNow = now.toKotlinInstant()
        val transactionId = transactionDao.insert(
            item.toPaymentTransaction(amountInCents, date, kotlinNow).toEntity(),
        )
        val paymentId = paymentDao.insertPayment(
            FortnightPaymentEntity(
                itemId = itemId,
                amountInCents = amountInCents,
                dateEpochDay = date.toEpochDays().toLong(),
                transactionId = transactionId,
                createdAtEpochMillis = now.toEpochMilli(),
            ),
        )
        FortnightPaymentResult.Registered(paymentId, transactionId, getDetails(item.planId)!!)
    }

    override suspend fun registerPaymentWithFunding(
        itemId: Long,
        amountInCents: Long,
        date: KotlinLocalDate,
        allowOverpayment: Boolean,
        fundingSourceDescription: String,
    ): FortnightPaymentResult = database.withTransaction {
        val itemEntity = planDao.getItem(itemId)
            ?: return@withTransaction FortnightPaymentResult.NotFound
        val item = itemEntity.toDomain()
        val planEntity = planDao.getPlan(item.planId)
            ?: return@withTransaction FortnightPaymentResult.NotFound
        if (planEntity.status == FortnightPlanStatus.CLOSED.name) {
            return@withTransaction FortnightPaymentResult.ClosedPlan
        }
        val alreadyPaid = paymentDao.getPaymentsForItem(itemId)
            .sumOf { it.amountInCents }
        when (val check = evaluateFortnightPayment(item.plannedAmountInCents, alreadyPaid, amountInCents)) {
            is FortnightPaymentCheck.InvalidAmount -> return@withTransaction FortnightPaymentResult.InvalidAmount
            is FortnightPaymentCheck.Overpayment -> {
                if (!allowOverpayment) {
                    return@withTransaction FortnightPaymentResult.Overpayment(
                        pendingInCents = check.pendingInCents,
                        excessInCents = check.excessInCents,
                    )
                }
            }
            is FortnightPaymentCheck.Valid -> Unit
        }
        val category = categoryDao.get(item.categoryId)
        if (category == null || !category.isActive || category.type != TransactionType.EXPENSE.name) {
            return@withTransaction FortnightPaymentResult.InvalidCategory
        }

        if (fundingSourceDescription.trim().isEmpty()) {
            return@withTransaction FortnightPaymentResult.Error("La fuente de financiación es obligatoria")
        }

        val now = Instant.now()
        val kotlinNow = now.toKotlinInstant()
        val transactionId = transactionDao.insert(
            item.toPaymentTransaction(amountInCents, date, kotlinNow).toEntity(),
        )
        val paymentId = paymentDao.insertPayment(
            FortnightPaymentEntity(
                itemId = itemId,
                amountInCents = amountInCents,
                dateEpochDay = date.toEpochDays().toLong(),
                transactionId = transactionId,
                createdAtEpochMillis = now.toEpochMilli(),
            ),
        )

        // Create the funding record
        val budgetConfig = database.budgetConfigDao().get()?.toDomain()
        val period = activeBudgetPeriod(budgetConfig, date)
        val transactions = transactionDao.getByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong())
        val incomeInCents = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amountInCents }
        val previousFundingInCents = expenseFundingDao.sumByPeriod(period.start.toEpochDays().toLong(), period.endInclusive.toEpochDays().toLong()) ?: 0L
        val expensesInCents = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amountInCents }
        val availableBeforeExpense = incomeInCents + previousFundingInCents - expensesInCents
        val overflowInCents = if (availableBeforeExpense - amountInCents < 0) -(availableBeforeExpense - amountInCents) else 0L

        if (overflowInCents > 0) {
            val funding = ExpenseFunding(
                transactionId = transactionId,
                amountInCents = overflowInCents,
                sourceDescription = fundingSourceDescription.trim(),
                createdAt = now.toKotlinInstant(),
                updatedAt = now.toKotlinInstant(),
            )
            expenseFundingDao.insert(funding.toEntity(date.toEpochDays().toLong()))
        }

        FortnightPaymentResult.Registered(paymentId, transactionId, getDetails(item.planId)!!)
    }

    override suspend fun deletePayment(paymentId: Long): FortnightMutationResult = database.withTransaction {
        val payment = paymentDao.getPayment(paymentId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        val item = planDao.getItem(payment.itemId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        val plan = planDao.getPlan(item.planId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        if (plan.status == FortnightPlanStatus.CLOSED.name) {
            return@withTransaction FortnightMutationResult.ClosedPlan
        }
        payment.transactionId?.let { transactionId ->
            transactionDao.delete(transactionId)
            expenseFundingDao.deleteByTransaction(transactionId)
        }
        paymentDao.deletePayment(paymentId)
        FortnightMutationResult.Success(paymentId)
    }

    override suspend fun closePlan(planId: Long): FortnightMutationResult = database.withTransaction {
        val plan = planDao.getPlan(planId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        planDao.closePlan(planId, Instant.now().toEpochMilli())
        FortnightMutationResult.Success(planId)
    }

    override suspend fun reopenPlan(planId: Long): FortnightMutationResult = database.withTransaction {
        val plan = planDao.getPlan(planId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        planDao.reopenPlan(planId)
        FortnightMutationResult.Success(planId)
    }

    override suspend fun deletePlan(planId: Long): FortnightMutationResult = database.withTransaction {
        val plan = planDao.getPlan(planId)
            ?: return@withTransaction FortnightMutationResult.NotFound
        if (plan.status == FortnightPlanStatus.CLOSED.name) {
            return@withTransaction FortnightMutationResult.ClosedPlan
        }
        if (paymentDao.countPaymentsForPlan(planId) > 0) {
            return@withTransaction FortnightMutationResult.HasPayments
        }
        planDao.deletePlan(planId)
        FortnightMutationResult.Success(planId)
    }
}

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetConfigDao,
    private val cycleDao: BudgetCycleDao,
    private val database: FinanceDatabase,
) : BudgetRepository {
    override fun observe(): Flow<BudgetConfig?> = dao.observe().map { entity ->
        entity?.let {
            BudgetConfig(
                amountInCents = it.amountInCents,
                period = BudgetPeriod.valueOf(it.period),
                cycleStart = it.cycleStartEpochDay?.let(LocalDate::ofEpochDay)?.toKotlinLocalDate(),
                cycleStartedAt = it.cycleStartedAtEpochMillis?.let(Instant::ofEpochMilli)?.toKotlinInstant(),
                incomeTransactionId = it.incomeTransactionId,
                cycleSchedules = parseCycleSchedules(it.closingDays, BudgetPeriod.valueOf(it.period)),
            )
        }
    }

    override fun observeHistory(): Flow<List<BudgetCycle>> = cycleDao.observeAll().map { cycles ->
        cycles.map { entity ->
            BudgetCycle(
                id = entity.id,
                period = BudgetPeriod.valueOf(entity.period),
                budgetAmountInCents = entity.budgetAmountInCents,
                incomeInCents = entity.incomeInCents,
                expenseInCents = entity.expenseInCents,
                startDate = LocalDate.ofEpochDay(entity.startDateEpochDay).toKotlinLocalDate(),
                endDate = LocalDate.ofEpochDay(entity.endDateEpochDay).toKotlinLocalDate(),
                closedAt = Instant.ofEpochMilli(entity.closedAtEpochMillis).toKotlinInstant(),
            )
        }
    }

    override suspend fun save(config: BudgetConfig) {
        dao.upsert(config.toEntity())
    }

    override suspend fun closeCycle(cycle: BudgetCycle, nextConfig: BudgetConfig) {
        database.withTransaction {
            cycleDao.insert(cycle.toEntity())
            dao.upsert(nextConfig.toEntity())
        }
    }
}

private fun BudgetConfig.toEntity() = BudgetConfigEntity(
    amountInCents = amountInCents,
    period = period.name,
    cycleStartEpochDay = cycleStart?.toEpochDays()?.toLong(),
    cycleStartedAtEpochMillis = cycleStartedAt?.toEpochMilliseconds(),
    incomeTransactionId = incomeTransactionId,
    closingDays = cycleSchedules.toSerializedCycleSchedules(),
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
    if (legacyOpeningDays.size < 2) return defaultCycleSchedules(period)
    return legacyOpeningDays.mapIndexed { index, openingDay ->
        val nextOpeningDay = legacyOpeningDays[(index + 1) % legacyOpeningDays.size]
        BudgetCycleSchedule(
            openingDay = openingDay,
            closingDay = if (nextOpeningDay == 1) 31 else nextOpeningDay - 1,
        )
    }
}

private fun List<BudgetCycleSchedule>.toSerializedCycleSchedules(): String =
    distinct().joinToString(",") { "${it.openingDay}:${it.closingDay}" }

private fun BudgetCycle.toEntity() = BudgetCycleEntity(
    id = id,
    period = period.name,
    budgetAmountInCents = budgetAmountInCents,
    incomeInCents = incomeInCents,
    expenseInCents = expenseInCents,
    startDateEpochDay = startDate.toEpochDays().toLong(),
    endDateEpochDay = endDate.toEpochDays().toLong(),
    closedAtEpochMillis = closedAt.toEpochMilliseconds(),
)

class RoomExpenseFundingRepository @Inject constructor(
    private val dao: ExpenseFundingDao,
    private val transactionDao: TransactionDao,
) : ExpenseFundingRepository {
    override fun observeAll(): Flow<List<ExpenseFunding>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }

    override fun observeByTransaction(transactionId: Long): Flow<ExpenseFunding?> =
        dao.observeByTransaction(transactionId).map { it?.toDomain() }

    override fun observeByPeriod(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseFunding>> =
        dao.observeByPeriod(startEpochDay, endEpochDay).map { items -> items.map { it.toDomain() } }

    override suspend fun getByTransaction(transactionId: Long): ExpenseFunding? =
        dao.getByTransaction(transactionId)?.toDomain()

    override suspend fun create(funding: ExpenseFunding): Long {
        val transaction = transactionDao.get(funding.transactionId)
            ?: throw IllegalArgumentException("Transaction not found: ${funding.transactionId}")
        return dao.insert(funding.toEntity(transaction.dateEpochDay))
    }

    override suspend fun update(funding: ExpenseFunding) {
        val transaction = transactionDao.get(funding.transactionId)
            ?: throw IllegalArgumentException("Transaction not found: ${funding.transactionId}")
        dao.update(funding.toEntity(transaction.dateEpochDay))
    }

    override suspend fun deleteByTransaction(transactionId: Long) =
        dao.deleteByTransaction(transactionId)

    override suspend fun sumByPeriod(startEpochDay: Long, endEpochDay: Long): Long =
        dao.sumByPeriod(startEpochDay, endEpochDay) ?: 0L
}
