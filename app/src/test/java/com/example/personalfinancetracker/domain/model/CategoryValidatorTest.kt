package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryValidatorTest {
    private fun category(
        id: Long,
        name: String,
        type: TransactionType,
        isActive: Boolean = true,
    ) = Category(id, name, type, "label", isActive)

    @Test fun `rejects blank name`() {
        val error = CategoryValidator.validateName("   ", TransactionType.EXPENSE, emptyList())
        assertEquals("Escribe el nombre de la categoría", error)
    }

    @Test fun `rejects name longer than max length`() {
        val error = CategoryValidator.validateName("a".repeat(41), TransactionType.EXPENSE, emptyList())
        assertEquals("El nombre no puede tener más de 40 caracteres", error)
    }

    @Test fun `accepts valid unused name`() {
        assertNull(CategoryValidator.validateName("Mascotas", TransactionType.EXPENSE, emptyList()))
    }

    @Test fun `detects duplicate ignoring case and trimming`() {
        val existing = listOf(category(1, "Alimentación", TransactionType.EXPENSE))
        val error = CategoryValidator.validateName("  alimentación ", TransactionType.EXPENSE, existing)
        assertEquals("Ya existe una categoría con ese nombre", error)
    }

    @Test fun `allows same name in other type`() {
        val existing = listOf(category(1, "Otros", TransactionType.INCOME))
        assertNull(CategoryValidator.validateName("Otros", TransactionType.EXPENSE, existing))
    }

    @Test fun `ignores own id when editing`() {
        val existing = listOf(category(1, "Salud", TransactionType.EXPENSE))
        assertNull(CategoryValidator.validateName("SALUD", TransactionType.EXPENSE, existing, excludeId = 1))
    }

    @Test fun `blocks deactivating last active income category`() {
        val all = listOf(category(1, "Salario", TransactionType.INCOME), category(2, "Comida", TransactionType.EXPENSE))
        val error = CategoryValidator.lastActiveIncomeError(all.first(), all)
        assertEquals("Debe existir al menos una categoría de ingresos activa", error)
    }

    @Test fun `blocks deleting active income category when it is the only one`() {
        val all = listOf(category(1, "Salario", TransactionType.INCOME))
        assertEquals(
            "Debe existir al menos una categoría de ingresos activa",
            CategoryValidator.lastActiveIncomeError(all.first(), all),
        )
    }

    @Test fun `allows income operation when another active income exists`() {
        val all = listOf(
            category(1, "Salario", TransactionType.INCOME),
            category(2, "Freelance", TransactionType.INCOME),
        )
        assertNull(CategoryValidator.lastActiveIncomeError(all.first(), all))
    }

    @Test fun `never blocks inactive or expense categories`() {
        val all = listOf(category(1, "Salario", TransactionType.INCOME, isActive = false))
        assertNull(CategoryValidator.lastActiveIncomeError(all.first(), all))
        assertNull(CategoryValidator.lastActiveIncomeError(category(2, "Comida", TransactionType.EXPENSE), all))
    }
}
