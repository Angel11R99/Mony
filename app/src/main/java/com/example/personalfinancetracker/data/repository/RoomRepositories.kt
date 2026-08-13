package com.example.personalfinancetracker.data.repository

import com.example.personalfinancetracker.data.local.dao.CategoryDao
import com.example.personalfinancetracker.data.local.dao.BudgetConfigDao
import com.example.personalfinancetracker.data.local.dao.TransactionDao
import com.example.personalfinancetracker.data.local.entity.BudgetConfigEntity
import com.example.personalfinancetracker.data.mapper.toDomain
import com.example.personalfinancetracker.data.mapper.toEntity
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.BudgetConfig
import com.example.personalfinancetracker.domain.model.BudgetPeriod
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.BudgetRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {
    override fun observeAll() = dao.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeByPeriod(period: DateRange) = dao.observeByPeriod(
        period.start.toEpochDay(), period.endInclusive.toEpochDay()
    ).map { items -> items.map { it.toDomain() } }
    override suspend fun get(id: Long) = dao.get(id)?.toDomain()
    override suspend fun create(transaction: FinanceTransaction) = dao.insert(transaction.toEntity())
    override suspend fun update(transaction: FinanceTransaction) = dao.update(transaction.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeActive(type: TransactionType): Flow<List<Category>> =
        dao.observeActive(type.name).map { items -> items.map { it.toDomain() } }
    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }
}

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetConfigDao,
) : BudgetRepository {
    override fun observe(): Flow<BudgetConfig?> = dao.observe().map { entity ->
        entity?.let { BudgetConfig(it.amountInCents, BudgetPeriod.valueOf(it.period)) }
    }

    override suspend fun save(config: BudgetConfig) {
        dao.upsert(BudgetConfigEntity(amountInCents = config.amountInCents, period = config.period.name))
    }
}
