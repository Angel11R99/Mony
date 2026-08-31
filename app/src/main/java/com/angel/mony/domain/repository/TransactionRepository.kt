package com.angel.mony.domain.repository

import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<FinanceTransaction>>
    fun observeByPeriod(period: DateRange): Flow<List<FinanceTransaction>>
    fun observeBySavingsGoal(goalId: Long): Flow<List<FinanceTransaction>>
    suspend fun get(id: Long): FinanceTransaction?
    suspend fun create(transaction: FinanceTransaction): Long
    suspend fun update(transaction: FinanceTransaction)
    suspend fun delete(id: Long)
    suspend fun duplicate(id: Long): Long?

    /**
     * Restaura movimientos de un respaldo dentro de una única transacción de base de datos.
     * Crea las categorías faltantes y omite movimientos duplicados.
     * Devuelve la cantidad de movimientos insertados.
     */
    suspend fun restoreBackup(movements: List<BackupMovement>): Int
}
