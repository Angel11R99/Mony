package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_cycle_history")
data class BudgetCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val budgetAmountInCents: Long,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val closedAtEpochMillis: Long,
)
