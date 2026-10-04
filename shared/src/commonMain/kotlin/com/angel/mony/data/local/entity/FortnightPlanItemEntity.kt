package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Item del plan. Guarda un snapshot del monto planificado, por lo que cambiar o
 * eliminar la plantilla de origen no modifica períodos ya creados.
 */
@Entity(
    tableName = "fortnight_items",
    foreignKeys = [
        ForeignKey(
            entity = FortnightPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FortnightTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsGoalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planId"), Index("templateId"), Index("categoryId"), Index("savingsGoalId")],
)
data class FortnightPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val templateId: Long?,
    val description: String,
    val plannedAmountInCents: Long,
    val categoryId: Long,
    val type: String,
    val savingsGoalId: Long?,
    val note: String?,
    val position: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
