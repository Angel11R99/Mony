package com.angel.mony.data.nlp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angel.mony.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs real Gemini Nano inference only on supported Android/AICore devices. */
@RunWith(AndroidJUnit4::class)
class LocalLanguageModelInferenceTest {
    @Test
    fun geminiNanoInterpretsExpenseAndIncomeIfSupported() = runBlocking {
        val engine = GoogleVoiceTransactionEngine()
        assumeTrue("Gemini Nano / AICore no disponible", engine.prepareActiveModel() is LocalModelPreparation.Ready)
        val categories = listOf(
            Category(1, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
            Category(2, "Trabajos", TransactionType.INCOME, "payments", true)
        )
        val expense = engine.interpret(
            "Ayer gasté quinientos pesos cenando", categories,
            VoiceTransactionDraft(date = LocalDate.now())
        )
        assertEquals(InterpretationEngine.ON_DEVICE_LLM, expense.engine)
        assertTrue("Error de inferencia: ${expense.modelFailure}", expense.modelFailure == null)
        val command = expense.command as VoiceTransactionCommand.Apply
        assertEquals(TransactionType.EXPENSE, command.draft.type)
        assertEquals(50_000L, command.draft.amountInCents)
        assertEquals(LocalDate.now().minusDays(1), command.draft.date)

        val income = engine.interpret(
            "Me pagaron mil pesos por hacer un trabajo", categories, VoiceTransactionDraft()
        )
        assertTrue(income.modelFailure == null)
        val incomeCommand = income.command as VoiceTransactionCommand.Apply
        assertEquals(TransactionType.INCOME, incomeCommand.draft.type)
        assertEquals(100_000L, incomeCommand.draft.amountInCents)
    }
}
