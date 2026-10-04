package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingListEntity::class,
        parentColumns = ["id"],
        childColumns = ["shoppingListId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("shoppingListId"), Index("barcode")],
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val quantity: Int,
    val estimatedUnitPriceInCents: Long?,
    val actualUnitPriceInCents: Long?,
    val barcode: String?,
    val isPurchased: Boolean,
    val isIdentified: Boolean,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
