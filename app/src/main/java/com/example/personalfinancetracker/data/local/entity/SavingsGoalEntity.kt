package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmountInCents: Long,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)
