package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_adjustments",
    foreignKeys = [ForeignKey(
        entity = ShoppingListEntity::class,
        parentColumns = ["id"],
        childColumns = ["shoppingListId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("shoppingListId")],
)
data class ShoppingAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val isPositive: Boolean,
    val amountInCents: Long,
    val createdAtEpochMillis: Long,
)
