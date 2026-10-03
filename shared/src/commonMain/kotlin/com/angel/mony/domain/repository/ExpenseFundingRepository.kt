package com.angel.mony.domain.repository

import com.angel.mony.domain.model.ExpenseFunding
import kotlinx.coroutines.flow.Flow

interface ExpenseFundingRepository {
    fun observeAll(): Flow<List<ExpenseFunding>>
    fun observeByTransaction(transactionId: Long): Flow<ExpenseFunding?>
    fun observeByPeriod(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseFunding>>
    suspend fun getByTransaction(transactionId: Long): ExpenseFunding?
    suspend fun create(funding: ExpenseFunding): Long
    suspend fun update(funding: ExpenseFunding)
    suspend fun deleteByTransaction(transactionId: Long)
    suspend fun sumByPeriod(startEpochDay: Long, endEpochDay: Long): Long
}
