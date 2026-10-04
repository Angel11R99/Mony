package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Abono real de un item quincenal. Cada fila corresponde a un movimiento
 * financiero y nunca debe perderse: por eso el item se borra en cascada solo
 * junto con el plan, y la transacción vinculada se limpia con SET_NULL.
 */
@Entity(
    tableName = "fortnight_payments",
    foreignKeys = [
        ForeignKey(
            entity = FortnightPlanItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["transactionId"], unique = true),
    ],
)
data class FortnightPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val amountInCents: Long,
    val dateEpochDay: Long,
    val transactionId: Long?,
    val createdAtEpochMillis: Long,
)
