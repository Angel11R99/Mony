package com.angel.mony.data.nlp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.InterpretationEngine
import com.angel.mony.domain.model.LocalModelPreparation
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceTransactionCommand
import com.angel.mony.domain.model.VoiceTransactionDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Executes real LiteRT-LM inference on a physical Android device.
 * Skips only if no downloadable Qwen model is installed.
 * Does not mock model output or use the old regex parser.
 */
@RunWith(AndroidJUnit4::class)
class QwenVoiceInterpretationInstrumentedTest {
    @Test
    fun naturalExpenseAndIncomeUseRealQwen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = OnDeviceNaturalLanguageTransactionEngine(context)
        assumeTrue("Instala un modelo Qwen compatible antes de ejecutar la prueba",
            engine.prepareActiveModel() is LocalModelPreparation.Ready)

        val categories = listOf(
            Category(1L, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
            Category(2L, "Trabajo", TransactionType.INCOME, "payments", true),
        )
        val expense = engine.interpret(
            "Agrégame una comida que compramos ayer en la noche de quinientos pesos",
            categories, VoiceTransactionDraft(date = LocalDate.now()))
        assertEquals(InterpretationEngine.ON_DEVICE_LLM, expense.engine)
        assertNull("El modelo no logró completar la inferencia", expense.modelFailure)
        val expenseDraft = (expense.command as VoiceTransactionCommand.Apply).draft
        assertEquals(TransactionType.EXPENSE, expenseDraft.type)
        assertEquals(50_000L, expenseDraft.amountInCents)
        assertEquals(LocalDate.now().minusDays(1), expenseDraft.date)
        assertEquals(1L, expenseDraft.categoryId)
        assertTrue("La nota debe conservar el contexto, sin inventarlo", !expenseDraft.note.isNullOrBlank())

        val income = engine.interpret(
            "Me entraron mil pesos por un trabajo de hoy", categories, VoiceTransactionDraft())
        assertNull(income.modelFailure)
        val incomeDraft = (income.command as VoiceTransactionCommand.Apply).draft
        assertEquals(TransactionType.INCOME, incomeDraft.type)
        assertEquals(100_000L, incomeDraft.amountInCents)
    }
    @Test
    fun variedSpanishParaphrasesKeepFinancialDirection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = OnDeviceNaturalLanguageTransactionEngine(context)
        assumeTrue("Necesita un modelo Qwen local listo",
            engine.prepareActiveModel() is LocalModelPreparation.Ready)
        val categories = listOf(
            Category(1L, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
            Category(2L, "Servicios", TransactionType.EXPENSE, "wifi", true),
            Category(3L, "Trabajo", TransactionType.INCOME, "payments", true),
        )
        val cases = listOf(
            Triple("Me gasté quinientos pesos comiendo ayer", TransactionType.EXPENSE, 50_000L),
            Triple("Compramos la cena por RD\$500", TransactionType.EXPENSE, 50_000L),
            Triple("Pon 500 pesos de internet", TransactionType.EXPENSE, 50_000L),
            Triple("Pagué 800 en el supermercado", TransactionType.EXPENSE, 80_000L),
            Triple("Me pagaron mil pesos", TransactionType.INCOME, 100_000L),
            Triple("Recibí 2500 pesos de un trabajo", TransactionType.INCOME, 250_000L),
            Triple("Me entraron quinientos pesos por una venta", TransactionType.INCOME, 50_000L),
            Triple("Gasté doscientos en un almuerzo", TransactionType.EXPENSE, 20_000L),
        )
        for ((utterance, expectedType, expectedAmount) in cases) {
            val result = engine.interpret(utterance, categories, VoiceTransactionDraft())
            assertNull("Inferencia falló: $utterance", result.modelFailure)
            val draft = (result.command as VoiceTransactionCommand.Apply).draft
            assertEquals("Tipo incorrecto: $utterance", expectedType, draft.type)
            assertEquals("Monto incorrecto: $utterance", expectedAmount, draft.amountInCents)
        }
    }

}
