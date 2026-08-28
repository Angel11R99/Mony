package com.example.personalfinancetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.personalfinancetracker.data.local.entity.KnownProductEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingAdjustmentEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingListEntity
import com.example.personalfinancetracker.data.local.entity.ShoppingListItemEntity
import com.example.personalfinancetracker.data.local.entity.TransactionEntity
import com.example.personalfinancetracker.data.local.entity.ProductRecognitionAliasEntity
import kotlinx.coroutines.flow.Flow

data class ShoppingListOverviewRow(
    @Embedded val list: ShoppingListEntity,
    val itemCount: Int,
    val totalInCents: Long,
)

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY CASE status WHEN 'SHOPPING' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END, updatedAtEpochMillis DESC")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query(
        """SELECT shopping_lists.*,
            (SELECT COUNT(*) FROM shopping_list_items WHERE shoppingListId = shopping_lists.id) AS itemCount,
            COALESCE((SELECT SUM(quantity * actualUnitPriceInCents) FROM shopping_list_items
                WHERE shoppingListId = shopping_lists.id AND isPurchased = 1 AND actualUnitPriceInCents IS NOT NULL), 0)
            + COALESCE((SELECT SUM(CASE WHEN isPositive = 1 THEN amountInCents ELSE -amountInCents END)
                FROM shopping_adjustments WHERE shoppingListId = shopping_lists.id), 0) AS totalInCents
            FROM shopping_lists
            ORDER BY CASE status WHEN 'SHOPPING' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END, updatedAtEpochMillis DESC"""
    )
    fun observeListOverviews(): Flow<List<ShoppingListOverviewRow>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    fun observeList(id: Long): Flow<ShoppingListEntity?>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY isPurchased ASC, createdAtEpochMillis ASC")
    fun observeItems(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_adjustments WHERE shoppingListId = :listId ORDER BY createdAtEpochMillis ASC")
    fun observeAdjustments(listId: Long): Flow<List<ShoppingAdjustmentEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getList(id: Long): ShoppingListEntity?

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY createdAtEpochMillis ASC")
    suspend fun getItems(listId: Long): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_adjustments WHERE shoppingListId = :listId ORDER BY createdAtEpochMillis ASC")
    suspend fun getAdjustments(listId: Long): List<ShoppingAdjustmentEntity>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: Long): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_adjustments WHERE id = :id LIMIT 1")
    suspend fun getAdjustment(id: Long): ShoppingAdjustmentEntity?

    @Query("SELECT * FROM known_products WHERE barcode = :barcode LIMIT 1")
    suspend fun findKnownProduct(barcode: String): KnownProductEntity?

    @Query("SELECT * FROM product_recognition_aliases WHERE normalizedAlias = :normalizedAlias ORDER BY confirmationCount DESC, lastUsedAtEpochMillis DESC")
    suspend fun findAliases(normalizedAlias: String): List<ProductRecognitionAliasEntity>

    @Query("SELECT transactions.* FROM transactions INNER JOIN shopping_lists ON shopping_lists.expenseTransactionId = transactions.id WHERE shopping_lists.id = :listId LIMIT 1")
    suspend fun getExpenseTransaction(listId: Long): TransactionEntity?

    @Query("SELECT id FROM shopping_lists WHERE expenseTransactionId = :transactionId LIMIT 1")
    suspend fun findListIdByExpenseTransaction(transactionId: Long): Long?

    @Insert
    suspend fun insertList(list: ShoppingListEntity): Long

    @Update
    suspend fun updateList(list: ShoppingListEntity): Int

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteList(id: Long): Int

    @Query("UPDATE shopping_lists SET updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun touchList(id: Long, updatedAt: Long): Int

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity): Int

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItem(id: Long): Int

    @Insert
    suspend fun insertAdjustment(adjustment: ShoppingAdjustmentEntity): Long

    @Update
    suspend fun updateAdjustment(adjustment: ShoppingAdjustmentEntity): Int

    @Query("DELETE FROM shopping_adjustments WHERE id = :id")
    suspend fun deleteAdjustment(id: Long): Int

    @Query("UPDATE shopping_lists SET status = 'COMPLETED', completedAtEpochMillis = :completedAt, updatedAtEpochMillis = :completedAt WHERE id = :id AND status != 'COMPLETED'")
    suspend fun markCompleted(id: Long, completedAt: Long): Int

    @Upsert
    suspend fun upsertKnownProduct(product: KnownProductEntity)

    @Query("SELECT * FROM product_recognition_aliases WHERE normalizedAlias = :normalizedAlias AND displayName = :displayName AND ((barcode IS NULL AND :barcode IS NULL) OR barcode = :barcode) LIMIT 1")
    suspend fun findAlias(normalizedAlias: String, displayName: String, barcode: String?): ProductRecognitionAliasEntity?

    @Insert
    suspend fun insertAlias(alias: ProductRecognitionAliasEntity): Long

    @Update
    suspend fun updateAlias(alias: ProductRecognitionAliasEntity): Int

    @Query("DELETE FROM known_products WHERE barcode = :barcode")
    suspend fun deleteKnownProduct(barcode: String): Int

    @Query("UPDATE shopping_lists SET status = :status, completedAtEpochMillis = NULL, expenseTransactionId = NULL, payableId = NULL, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun reopen(id: Long, status: String, updatedAt: Long): Int
}
