package com.example.personalfinancetracker.data.repository

import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.BudgetCycleDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.dao.FixedEntryDao
import com.example.personalfinancetracker.data.local.dao.PendingEntryDao
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import com.example.personalfinancetracker.data.local.entity.BudgetCycleEntity
import com.example.personalfinancetracker.data.local.database.FinanceDatabase
import com.example.personalfinancetracker.data.mapper.toDomain
import com.example.personalfinancetracker.data.mapper.toEntity
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetCycleSchedule
import com.example.personalfinancetracker.domain.model.BudgetCycle
import com.example.personalfinancetracker.domain.model.BudgetPeriod
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val fixedEntryDao: FixedEntryDao,
    private val database: FinanceDatabase,
) : TransactionRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeByPeriod(period: DateRange) = dao.observeByPeriod(
        period.start.toEpochDay(), period.endInclusive.toEpochDay()
    ).map { items -> items.map { it.toDomain() } }
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
}

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeActive(type: TransactionType): Flow<List<Category>> =
        dao.observeActive(type.name).map { items -> items.map { it.toDomain() } }
    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }
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
