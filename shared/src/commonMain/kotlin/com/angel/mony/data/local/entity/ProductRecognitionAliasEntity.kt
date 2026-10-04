package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_recognition_aliases",
    indices = [Index("normalizedAlias"), Index("barcode")],
)
data class ProductRecognitionAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val detectedText: String,
    val normalizedAlias: String,
    val displayName: String,
    val barcode: String?,
    val confirmationCount: Int,
    val lastUsedAtEpochMillis: Long,
)
