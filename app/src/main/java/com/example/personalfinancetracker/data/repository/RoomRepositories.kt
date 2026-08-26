package com.example.personalfinancetracker.data.repository

import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.BudgetCycleDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.dao.FixedEntryDao
import com.example.personalfinancetracker.data.local.dao.PendingEntryDao
import com.example.personalfinancetracker.data.local.dao.SavingsGoalDao
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import com.example.personalfinancetracker.data.local.entity.BudgetCycleEntity
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.data.local.entity.SavingsGoalEntity
import com.example.personalfinancetracker.data.local.database.FinanceDatabase
import com.example.personalfinancetracker.data.mapper.toDomain
import com.example.personalfinancetracker.data.mapper.toEntity
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BackupMovement
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import com.example.personalfinancetracker.domain.model.defaultCycleSchedules
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.model.PendingEntry
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.domain.repository.FixedEntryRepository
import com.example.personalfinancetracker.domain.repository.PendingEntryRepository
import com.example.personalfinancetracker.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val fixedEntryDao: FixedEntryDao,
    private val categoryDao: CategoryDao,
    private val database: FinanceDatabase,
) : TransactionRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeByPeriod(period: DateRange) = dao.observeByPeriod(
        period.start.toEpochDay(), period.endInclusive.toEpochDay()
    ).map { items -> items.map { it.toDomain() } }
    override fun observeBySavingsGoal(goalId: Long) =
        dao.observeBySavingsGoal(goalId).map { items -> items.map { it.toDomain() } }
    override suspend fun get(id: Long) = dao.get(id)?.toDomain()
    override suspend fun create(transaction: FinanceTransaction) = dao.insert(transaction.toEntity())
    override suspend fun update(transaction: FinanceTransaction) = dao.update(transaction.toEntity())
    override suspend fun delete(id: Long) = database.withTransaction {
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
                dateEpochDay = movement.date.toEpochDay(),
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
    private val database: FinanceDatabase,
) : FixedEntryRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override suspend fun save(entry: com.example.personalfinancetracker.domain.model.FixedEntry) =
        dao.upsert(entry.toEntity())
    override suspend fun post(
        entry: com.example.personalfinancetracker.domain.model.FixedEntry,
        transaction: FinanceTransaction,
    ) = database.withTransaction {
        transactionDao.insert(transaction.toEntity())
        dao.upsert(entry.toEntity())
        Unit
    }
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomPendingEntryRepository @Inject constructor(
    private val dao: PendingEntryDao,
    private val transactionDao: TransactionDao,
    private val database: FinanceDatabase,
) : PendingEntryRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override suspend fun get(id: Long) = dao.get(id)?.toDomain()
    override suspend fun save(entry: PendingEntry) = dao.upsert(entry.toEntity())
    override suspend fun complete(entry: PendingEntry, transaction: FinanceTransaction) {
        database.withTransaction {
            val transactionId = transactionDao.insert(transaction.toEntity())
            dao.upsert(
                entry.copy(isDone = true, doneAt = transaction.createdAt, transactionId = transactionId).toEntity()
            )
        }
    }
    override suspend fun reopen(entry: PendingEntry) {
        database.withTransaction {
            entry.transactionId?.let { transactionDao.delete(it) }
            dao.upsert(entry.copy(isDone = false, doneAt = null, transactionId = null).toEntity())
        }
    }
    override suspend fun delete(id: Long) = dao.delete(id)
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

    override suspend fun delete(id: Long) {
        database.withTransaction {
            dao.unlinkTransactions(id)
            dao.deleteById(id)
        }
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
                cycleStart = it.cycleStartEpochDay?.let(LocalDate::ofEpochDay),
                cycleStartedAt = it.cycleStartedAtEpochMillis?.let(Instant::ofEpochMilli),
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
                startDate = LocalDate.ofEpochDay(entity.startDateEpochDay),
                endDate = LocalDate.ofEpochDay(entity.endDateEpochDay),
                closedAt = Instant.ofEpochMilli(entity.closedAtEpochMillis),
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
    cycleStartEpochDay = cycleStart?.toEpochDay(),
    cycleStartedAtEpochMillis = cycleStartedAt?.toEpochMilli(),
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
    startDateEpochDay = startDate.toEpochDay(),
    endDateEpochDay = endDate.toEpochDay(),
    closedAtEpochMillis = closedAt.toEpochMilli(),
)
