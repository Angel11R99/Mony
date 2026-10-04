package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Plan de un período quincenal. El presupuesto es solo una referencia de
 * planeación: nunca genera movimientos financieros.
 */
@Entity(
    tableName = "fortnight_plans",
    indices = [
        Index(value = ["startDateEpochDay", "endDateEpochDay"], unique = true),
        Index("status"),
    ],
)
data class FortnightPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val slot: String,
    val budgetInCents: Long,
    val status: String,
    val createdAtEpochMillis: Long,
    val closedAtEpochMillis: Long?,
)
