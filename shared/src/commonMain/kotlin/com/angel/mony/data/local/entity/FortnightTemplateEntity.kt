package com.angel.mony.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Plantilla reutilizable de un item quincenal. No guarda pagos, por lo que
 * modificarla nunca altera planes ya existentes.
 */
@Entity(
    tableName = "fortnight_templates",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("categoryId"), Index("isActive")],
)
data class FortnightTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val firstFortnightAmountInCents: Long?,
    val secondFortnightAmountInCents: Long?,
    val categoryId: Long,
    val type: String,
    val note: String?,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
