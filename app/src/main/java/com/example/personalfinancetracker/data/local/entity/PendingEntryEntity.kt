package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_entries",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [
        Index("categoryId"),
        Index("type"),
        Index("dateEpochDay"),
        Index("isDone"),
        Index("sourceShoppingListId", unique = true),
    ],
)
data class PendingEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val dateEpochDay: Long,
    val reminderMinutesOfDay: Int?,
    val comment: String?,
    val isDone: Boolean,
    val doneAtEpochMillis: Long?,
    val transactionId: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val sourceShoppingListId: Long? = null,
)
