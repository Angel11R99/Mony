package com.angel.mony.domain.model

object CategoryValidator {
    const val MAX_NAME_LENGTH = 40

    fun validateName(
        rawName: String,
        type: TransactionType,
        existing: List<Category>,
        excludeId: Long? = null,
    ): String? {
        val name = rawName.trim()
        if (name.isEmpty()) return "Escribe el nombre de la categoría"
        if (name.length > MAX_NAME_LENGTH) {
            return "El nombre no puede tener más de $MAX_NAME_LENGTH caracteres"
        }
        val duplicated = existing.any {
            it.id != excludeId && it.type == type && it.name.equals(name, ignoreCase = true)
        }
        if (duplicated) return "Ya existe una categoría con ese nombre"
        return null
    }

    fun lastActiveIncomeError(category: Category, all: List<Category>): String? {
        val keepsActiveIncome = category.type != TransactionType.INCOME || !category.isActive
        if (keepsActiveIncome) return null
        val hasOtherActiveIncome = all.any {
            it.id != category.id && it.type == TransactionType.INCOME && it.isActive
        }
        return if (hasOtherActiveIncome) null
        else "Debe existir al menos una categoría de ingresos activa"
    }
}
