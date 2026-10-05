package com.angel.mony.presentation.transactions

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceTransactionCommand
import com.angel.mony.domain.model.VoiceTransactionInterpreter
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceExampleCatalogTest {
    private val categories = listOf(
        Category(1, "Transporte", TransactionType.EXPENSE, "car", true),
        Category(2, "Salario", TransactionType.INCOME, "payments", true),
        Category(3, "Inactiva", TransactionType.EXPENSE, "other", false),
    )

    @Test fun `catalog uses only real active categories`() {
        val catalog = VoiceExampleCatalog.build(categories)
        assertTrue(catalog.any { it.type == TransactionType.EXPENSE && "Transporte" in it.text })
        assertTrue(catalog.any { it.type == TransactionType.INCOME && "Salario" in it.text })
        assertFalse(catalog.any { "Inactiva" in it.text })
        assertTrue(catalog.filter { it.categoryId != null }.all { example ->
            categories.any { it.id == example.categoryId && it.isActive && it.type == example.type }
        })
    }

    @Test fun `every generated phrase is compatible with the voice interpreter`() {
        val interpreter = VoiceTransactionInterpreter(
            Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Santo_Domingo")),
        )
        VoiceExampleCatalog.build(categories).forEach { example ->
            val result = interpreter.interpret(example.text, categories) as VoiceTransactionCommand.Apply
            assertEquals(example.text, example.type, result.draft.type)
            assertEquals(example.text, example.categoryId, result.draft.categoryId)
            assertTrue(example.text, result.draft.amountInCents != null)
            assertTrue(example.text, result.draft.date != null)
        }
    }

    @Test fun `missing category type gets examples without invented category`() {
        val catalog = VoiceExampleCatalog.build(categories.filter { it.type == TransactionType.EXPENSE })
        val incomeExamples = catalog.filter { it.type == TransactionType.INCOME }
        assertTrue(incomeExamples.isNotEmpty())
        assertTrue(incomeExamples.all { it.categoryId == null })
        assertTrue(incomeExamples.none { " en " in it.text })
    }

    @Test fun `rotation does not repeat until catalog is exhausted or across round boundary`() {
        val catalog = VoiceExampleCatalog.build(categories).take(5)
        var rotation = VoiceExampleRotationState()
        var session: VoiceExampleSession? = null
        val selected = mutableListOf<String>()
        repeat(catalog.size + 1) { index ->
            val result = selectVoiceExample(catalog, rotation, session, "opening-$index", Random(7))
            selected += result.example.key
            rotation = result.rotationState
            session = result.session
        }
        assertEquals(catalog.size, selected.take(catalog.size).distinct().size)
        assertNotEquals(selected[catalog.lastIndex], selected.last())
    }

    @Test fun `same opening remains stable across recomposition and recreation`() {
        val catalog = VoiceExampleCatalog.build(categories)
        val first = selectVoiceExample(catalog, VoiceExampleRotationState(), null, "widget-1", Random(3))
        val restored = selectVoiceExample(catalog, first.rotationState, first.session, "widget-1", Random(99))
        assertEquals(first.example, restored.example)
        assertEquals(first.rotationState, restored.rotationState)
        assertFalse(restored.advanced)
    }

    @Test fun `invalidated category example is discarded within same opening`() {
        val original = VoiceExampleCatalog.build(categories)
        val selected = original.first { it.categoryId == 1L }
        val session = VoiceExampleSession("widget-1", selected.key)
        val withoutExpenseCategory = VoiceExampleCatalog.build(categories.filterNot { it.id == 1L })
        val replacement = selectVoiceExample(
            withoutExpenseCategory,
            VoiceExampleRotationState(lastKey = selected.key),
            session,
            "widget-1",
            Random(2),
        )
        assertNotEquals(selected.key, replacement.example.key)
        assertTrue(replacement.advanced)
    }

    @Test fun `single valid example may repeat safely`() {
        val only = VoiceExample("only", "Registra un gasto de cien pesos hoy", TransactionType.EXPENSE, null)
        val first = selectVoiceExample(listOf(only), VoiceExampleRotationState(), null, "one", Random(1))
        val second = selectVoiceExample(listOf(only), first.rotationState, first.session, "two", Random(1))
        assertEquals(only, second.example)
        assertNull(second.rotationState.remainingKeys.firstOrNull())
    }
}
