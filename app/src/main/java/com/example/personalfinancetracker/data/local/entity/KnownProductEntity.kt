package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "known_products")
data class KnownProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val lastPriceInCents: Long?,
    val lastUsedAtEpochMillis: Long,
)
