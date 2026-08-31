package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fixed_entries",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("categoryId"), Index("type")],
)
data class FixedEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val comment: String?,
    val isActive: Boolean,
    val manualDateMode: String,
    val manualSpecificDateEpochDay: Long?,
    val scheduleMode: String,
    val scheduleHour: Int,
    val scheduleSpecificDateEpochDay: Long?,
    val nextRunAtEpochMillis: Long?,
    val lastAddedAtEpochMillis: Long?,
    val lastAddedDateEpochDay: Long?,
)
