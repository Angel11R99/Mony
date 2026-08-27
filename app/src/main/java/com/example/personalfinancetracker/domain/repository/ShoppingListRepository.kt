package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.KnownProduct
import com.example.personalfinancetracker.domain.model.ShoppingAdjustment
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListDetails
import com.example.personalfinancetracker.domain.model.ShoppingListItem
import com.example.personalfinancetracker.domain.model.ShoppingListOverview
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

sealed interface ShoppingMutationResult {
    data class Success(val id: Long) : ShoppingMutationResult
    data object NotFound : ShoppingMutationResult
    data object CompletedList : ShoppingMutationResult
}

sealed interface FinalizePurchaseResult {
    data class Completed(val transactionId: Long, val totalInCents: Long) : FinalizePurchaseResult
    data class AlreadyCompleted(val transactionId: Long?) : FinalizePurchaseResult
    data class MissingActualPrices(val itemIds: List<Long>) : FinalizePurchaseResult
    data object ListNotFound : FinalizePurchaseResult
    data object InvalidExpenseCategory : FinalizePurchaseResult
    data object TotalNotPositive : FinalizePurchaseResult
    data object CalculationOverflow : FinalizePurchaseResult
}

interface ShoppingListRepository {
    fun observeLists(): Flow<List<ShoppingList>>
    fun observeListOverviews(): Flow<List<ShoppingListOverview>>
    fun observeDetails(listId: Long): Flow<ShoppingListDetails?>
    suspend fun getDetails(listId: Long): ShoppingListDetails?
    suspend fun create(list: ShoppingList): Long
    suspend fun update(list: ShoppingList): ShoppingMutationResult
    suspend fun delete(listId: Long): ShoppingMutationResult
    suspend fun saveItem(item: ShoppingListItem): ShoppingMutationResult
    suspend fun deleteItem(itemId: Long): ShoppingMutationResult
    suspend fun saveAdjustment(adjustment: ShoppingAdjustment): ShoppingMutationResult
    suspend fun saveAdjustments(adjustments: List<ShoppingAdjustment>): ShoppingMutationResult
    suspend fun deleteAdjustment(adjustmentId: Long): ShoppingMutationResult
    suspend fun findKnownProduct(barcode: String): KnownProduct?
    suspend fun duplicate(listId: Long): Long?
    suspend fun finalizePurchase(
        listId: Long,
        categoryId: Long,
        date: LocalDate,
        allowMissingPrices: Boolean = false,
    ): FinalizePurchaseResult
}
