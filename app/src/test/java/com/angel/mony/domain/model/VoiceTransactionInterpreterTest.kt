package com.angel.mony.domain.model

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTransactionInterpreterTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Santo_Domingo"))
    private val categories = listOf(
        Category(1, "Transporte", TransactionType.EXPENSE, "car", true),
        Category(2, "Salario", TransactionType.INCOME, "payments", true),
        Category(3, "Transporte", TransactionType.EXPENSE, "car", false),
    )
    private val interpreter = VoiceTransactionInterpreter(clock)

    @Test fun `parses complete expense and terminal save command`() {
        val result = interpreter.interpret(
            "Registra un gasto de quinientos pesos en Transporte, hoy, con nota taxi al trabajo y guárdalo",
            categories,
        ) as VoiceTransactionCommand.Apply
        assertEquals(TransactionType.EXPENSE, result.draft.type)
        assertEquals(50_000L, result.draft.amountInCents)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("2026-10-05", result.draft.date.toString())
        assertEquals("taxi al trabajo", result.draft.note)
        assertTrue(result.draft.saveRequested)
    }

    @Test fun `supports income synonym and exact cents`() {
        val result = interpreter.interpret("Recibí mil doscientos pesos con cincuenta centavos en Salario ayer", categories)
            as VoiceTransactionCommand.Apply
        assertEquals(TransactionType.INCOME, result.draft.type)
        assertEquals(120_050L, result.draft.amountInCents)
        assertEquals(2L, result.draft.categoryId)
        assertEquals("2026-10-04", result.draft.date.toString())
    }

    @Test fun `does not save when guardar is literal note text`() {
        val result = interpreter.interpret("Anota un gasto de 500 pesos en Transporte con nota recordar guardar el recibo", categories)
            as VoiceTransactionCommand.Apply
        assertFalse(result.draft.saveRequested)
        assertEquals("recordar guardar el recibo", result.draft.note)

        val endsWithCommandWord = interpreter.interpret(
            "Anota un gasto de 500 pesos en Transporte con nota recordar guardar",
            categories,
        ) as VoiceTransactionCommand.Apply
        assertFalse(endsWithCommandWord.draft.saveRequested)
        assertEquals("recordar guardar", endsWithCommandWord.draft.note)
    }

    @Test fun `rejects ambiguous numeric separator`() {
        val result = SpanishMoneyParser.parse("1.500") as SpokenMoney.Invalid
        assertTrue(result.message.contains("separador"))
    }

    @Test fun `parses relative dates with controlled clock`() {
        val result = interpreter.interpret("Gasté doscientos pesos en Transporte hace 3 días", categories)
            as VoiceTransactionCommand.Apply
        assertEquals("2026-10-02", result.draft.date.toString())

        val spoken = interpreter.interpret("Gasté doscientos pesos en Transporte hace tres días", categories)
            as VoiceTransactionCommand.Apply
        assertEquals("2026-10-02", spoken.draft.date.toString())
    }

    @Test fun `resolves all weekdays to their most recent occurrence`() {
        val expected = mapOf(
            "lunes" to "2026-10-05",
            "martes" to "2026-09-29",
            "MIÉRCOLES" to "2026-09-30",
            "jueves" to "2026-10-01",
            "viernes" to "2026-10-02",
            "SÁBADO" to "2026-10-03",
            "domingo" to "2026-10-04",
        )

        expected.forEach { (weekday, date) ->
            val result = interpreter.interpret(
                "Gasté quinientos pesos en Transporte el $weekday",
                categories,
            ) as VoiceTransactionCommand.Apply
            assertEquals(weekday, date, result.draft.date.toString())
        }
    }

    @Test fun `weekday qualifiers are strict in their requested direction`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE)
        val past = interpreter.interpret("Pon la fecha del lunes pasado", categories, current)
            as VoiceTransactionCommand.Apply
        val next = interpreter.interpret("Pon la fecha del próximo viernes", categories, current)
            as VoiceTransactionCommand.Apply
        val coming = interpreter.interpret("Pon la fecha del viernes que viene", categories, current)
            as VoiceTransactionCommand.Apply

        assertEquals("2026-09-28", past.draft.date.toString())
        assertEquals("2026-10-09", next.draft.date.toString())
        assertEquals("2026-10-09", coming.draft.date.toString())
    }

    @Test fun `weekday resolution crosses month and year boundaries`() {
        val monthInterpreter = interpreterAt(LocalDate.of(2026, 3, 1))
        val previousMonth = monthInterpreter.interpret("El sábado", categories) as VoiceTransactionCommand.Apply
        assertEquals("2026-02-28", previousMonth.draft.date.toString())

        val yearInterpreter = interpreterAt(LocalDate.of(2026, 1, 1))
        val previousYear = yearInterpreter.interpret("El miércoles", categories) as VoiceTransactionCommand.Apply
        assertEquals("2025-12-31", previousYear.draft.date.toString())

        val nextYearInterpreter = interpreterAt(LocalDate.of(2026, 12, 31))
        val nextYear = nextYearInterpreter.interpret("El próximo viernes", categories) as VoiceTransactionCommand.Apply
        assertEquals("2027-01-01", nextYear.draft.date.toString())
    }

    @Test fun `explicit date wins when weekday agrees and asks when it contradicts`() {
        val matching = interpreter.interpret(
            "Registra un gasto de cien pesos en Transporte el lunes 2026-10-05",
            categories,
        ) as VoiceTransactionCommand.Apply
        assertEquals("2026-10-05", matching.draft.date.toString())

        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE, date = LocalDate.of(2026, 10, 1))
        val conflict = interpreter.interpret("Pon la fecha del viernes 2026-10-05", categories, current)
            as VoiceTransactionCommand.Apply
        assertEquals("2026-10-01", conflict.draft.date.toString())
        assertTrue(conflict.messages.any { it.contains("no coincide") })
    }

    @Test fun `weekday inside literal note does not change date and terminal save remains valid`() {
        val current = VoiceTransactionDraft(
            type = TransactionType.EXPENSE,
            amountInCents = 50_000,
            categoryId = 1,
            date = LocalDate.of(2026, 10, 1),
        )
        val note = interpreter.interpret("Cambia la nota a pago del viernes", categories, current)
            as VoiceTransactionCommand.Apply
        assertEquals("2026-10-01", note.draft.date.toString())
        assertFalse(VoiceDraftField.DATE in note.mentioned)

        val save = interpreter.interpret(
            "Gasté quinientos pesos en Transporte el viernes y guárdalo",
            categories,
        ) as VoiceTransactionCommand.Apply
        assertEquals("2026-10-02", save.draft.date.toString())
        assertTrue(save.draft.saveRequested)
    }

    @Test fun `words in note do not change type or category`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE, categoryId = 1)
        val result = interpreter.interpret("Cambia la nota a ingreso para Transporte", categories, current)
            as VoiceTransactionCommand.Apply
        assertEquals(TransactionType.EXPENSE, result.draft.type)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("ingreso para Transporte", result.draft.note)
    }

    @Test fun `parses tens joined with y as whole pesos`() {
        assertEquals(3_500L, (SpanishMoneyParser.parse("treinta y cinco") as SpokenMoney.Valid).cents)
    }

    @Test fun `requires year for explicit month date`() {
        val result = interpreter.interpret("Registra un gasto de cien pesos en Transporte el 7 de octubre", categories)
            as VoiceTransactionCommand.Apply
        assertTrue(result.messages.any { it.contains("año") })
    }

    @Test fun `does not use inactive category`() {
        val onlyInactive = categories.filter { !it.isActive }
        val result = interpreter.interpret("Registra un gasto de 100 pesos en Transporte", onlyInactive)
            as VoiceTransactionCommand.Apply
        assertNull(result.draft.categoryId)
        assertTrue(result.messages.isNotEmpty())
    }

    @Test fun `reports duplicate active category names`() {
        val duplicate = categories + Category(4, "Transporte", TransactionType.EXPENSE, "car", true)
        val result = interpreter.interpret("Suma un gasto de 100 pesos en Transporte", duplicate)
            as VoiceTransactionCommand.Apply
        assertEquals(listOf(1L, 4L), result.categoryOptions.map(Category::id))
    }

    @Test fun `rejects phrase containing both transaction types`() {
        val result = interpreter.interpret("Registra un gasto y un ingreso de cien pesos", categories)
        assertEquals(VoiceTransactionCommand.AmbiguousType, result)
    }

    @Test fun `recognizes correction without overwriting other fields`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE, categoryId = 1, note = "Taxi")
        val result = interpreter.interpret("Cambia el monto a setecientos pesos", categories, current)
            as VoiceTransactionCommand.Apply
        assertEquals(70_000L, result.draft.amountInCents)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("Taxi", result.draft.note)
    }

    @Test fun `cancel and partial save are explicit commands`() {
        assertEquals(VoiceTransactionCommand.Cancel, interpreter.interpret("Cancelar dictado", categories))
        assertEquals(VoiceTransactionCommand.Save, interpreter.interpret("Guárdalo", categories))
    }

    @Test fun `keeps partial amount while widget asks for type`() {
        val result = interpreter.interpret("quinientos pesos en Transporte", categories)
            as VoiceTransactionCommand.Apply
        assertNull(result.draft.type)
        assertEquals(50_000L, result.draft.amountInCents)
    }

    @Test fun `uses form type with natural agrega phrase and colloquial note`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE)
        val result = interpreter.interpret(
            "Agrega quinientos pesos en Transporte para el día de hoy con eso se pagó la guagua",
            categories,
            current,
        ) as VoiceTransactionCommand.Apply

        assertEquals(TransactionType.EXPENSE, result.draft.type)
        assertEquals(50_000L, result.draft.amountInCents)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("2026-10-05", result.draft.date.toString())
        assertEquals("se pagó la guagua", result.draft.note)
    }

    @Test fun `accepts recognizer joining en to exact category name`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE)
        val result = interpreter.interpret(
            "Agrega quinientos pesos entrasporte apra el día de hoy",
            categories,
            current,
        ) as VoiceTransactionCommand.Apply

        assertEquals(50_000L, result.draft.amountInCents)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("2026-10-05", result.draft.date.toString())
    }

    @Test fun `funding source is separate from category and note`() {
        val current = VoiceTransactionDraft(type = TransactionType.EXPENSE, categoryId = 1, note = "Taxi", saveRequested = true)
        val result = interpreter.interpret("La fuente es efectivo guardado y guárdalo", categories, current)
            as VoiceTransactionCommand.Apply
        assertEquals("efectivo guardado", result.draft.fundingSource)
        assertEquals(1L, result.draft.categoryId)
        assertEquals("Taxi", result.draft.note)
        assertTrue(result.draft.saveRequested)
    }

    private fun interpreterAt(date: LocalDate): VoiceTransactionInterpreter {
        val zone = ZoneId.of("America/Santo_Domingo")
        return VoiceTransactionInterpreter(Clock.fixed(date.atStartOfDay(zone).toInstant(), zone))
    }
}
