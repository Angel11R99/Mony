package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_lists",
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id"],
        childColumns = ["expenseTransactionId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [
        Index(value = ["expenseTransactionId"], unique = true),
        Index(value = ["payableId"], unique = true),
        Index("expenseCategoryId"),
    ],
)
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: String,
    val budgetInCents: Long?,
    val expenseTransactionId: Long?,
    val payableId: Long?,
    val purchaseDateEpochDay: Long?,
    val paymentMethod: String?,
    val expenseCategoryId: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
)
