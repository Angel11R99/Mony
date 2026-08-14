package com.example.personalfinancetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_config")
data class BudgetConfigEntity(
    @PrimaryKey val id: Int = 1,
    val amountInCents: Long,
    val period: String,
    val cycleStartEpochDay: Long? = null,
    val cycleStartedAtEpochMillis: Long? = null,
    val incomeTransactionId: Long? = null,
    val closingDays: String = "15",
)
