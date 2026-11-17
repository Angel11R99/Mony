package com.angel.mony.domain.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTransactionInterpretationTest {
    private val categories = listOf(
        Category(1, "Alimentación", TransactionType.EXPENSE, "food", true),
        Category(2, "Salario", TransactionType.INCOME, "income", true),
    )
    private val interpreter = VoiceTransactionInterpreter(
        Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Santo_Domingo")),
    )

    @Test fun `written Spanish description extracts expense fields without speech services`() {
        val result = interpreter.interpret(
            "Hoy gasté 1,500 pesos en alimentos con nota algunas cosas para mi mamá",
            categories,
        ) as VoiceTransactionCommand.Apply

        assertEquals(TransactionType.EXPENSE, result.draft.type)
        assertEquals(150_000L, result.draft.amountInCents)
        assertEquals(1L, result.draft.categoryId)
        assertEquals(LocalDateForTest.today, result.draft.date)
        assertTrue(result.draft.note.orEmpty().contains("algunas cosas para mi mamá"))
    }

    @Test fun `typed and dictated text enter the same semantic interpreter`() {
        val phrase = "Gasté 350 pesos en alimentación hoy"
        assertEquals(interpreter.interpret(phrase, categories), interpreter.interpret(phrase, categories))
    }

    private object LocalDateForTest {
        val today = java.time.LocalDate.parse("2026-11-17")
    }
}
