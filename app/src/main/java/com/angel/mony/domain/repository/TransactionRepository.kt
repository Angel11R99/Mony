package com.angel.mony.domain.repository

import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.ExpenseCreationResult
import com.angel.mony.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<FinanceTransaction>>
    fun observeByPeriod(period: DateRange): Flow<List<FinanceTransaction>>
    fun observeBySavingsGoal(goalId: Long): Flow<List<FinanceTransaction>>
    suspend fun getRecent(limit: Int): List<FinanceTransaction>
    suspend fun get(id: Long): FinanceTransaction?
    suspend fun create(transaction: FinanceTransaction): Long
    suspend fun update(transaction: FinanceTransaction)
    suspend fun delete(id: Long)
    suspend fun duplicate(id: Long): Long?

    /**
     * Crea un gasto con su financiación asociada de forma atómica.
     * Si el gasto no causa desbordamiento, fundingSourceDescription se ignora.
     * Si causa desbordamiento, fundingSourceDescription es obligatorio.
     */
    suspend fun createWithFunding(
        transaction: FinanceTransaction,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult

    /**
     * Actualiza un gasto y su financiación asociada de forma atómica.
     * Recalcula el desbordamiento considerando el monto anterior del gasto.
     */
    suspend fun updateWithFunding(
        transaction: FinanceTransaction,
        existingTransactionId: Long,
        fundingSourceDescription: String?,
    ): ExpenseCreationResult

    /**
     * Restaura movimientos de un respaldo dentro de una única transacción de base de datos.
     * Crea las categorías faltantes y omite movimientos duplicados.
     * Devuelve la cantidad de movimientos insertados.
     */
    suspend fun restoreBackup(movements: List<BackupMovement>): Int
}
