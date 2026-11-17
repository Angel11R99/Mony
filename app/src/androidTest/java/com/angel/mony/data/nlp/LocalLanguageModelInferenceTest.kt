package com.angel.mony.data.nlp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.InterpretationEngine
import com.angel.mony.domain.model.LocalModelPreparation
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceTransactionCommand
import com.angel.mony.domain.model.VoiceTransactionDraft
import java.time.LocalDate
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalLanguageModelInferenceTest {
    @Test
    fun installedModelInitializesAndInterpretsARealTransactionOnDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        seedModelFromExternalTestFile(context)
        val engine = OnDeviceNaturalLanguageTransactionEngine(context)
        val installed = engine.models.filter { engine.isModelInstalled(it.id) }
        assumeTrue("Descarga un modelo antes de ejecutar esta prueba", installed.isNotEmpty())

        val previousSelection = engine.activeModelId
        val modelId = installed.first().id
        val unavailableId = engine.models.firstOrNull { !engine.isModelInstalled(it.id) }?.id
        if (unavailableId != null) {
            context.getSharedPreferences("language_model_preferences", Context.MODE_PRIVATE)
                .edit().putString("active_model_id", unavailableId).commit()
            assertEquals(modelId, OnDeviceNaturalLanguageTransactionEngine(context).activeModelId)
            assertEquals(
                modelId,
                context.getSharedPreferences("language_model_preferences", Context.MODE_PRIVATE)
                    .getString("active_model_id", null),
            )
        }
        engine.setActiveModel(modelId)
        try {
            assertEquals(LocalModelPreparation.Ready(modelId), engine.prepareActiveModel())

            val result = engine.interpret(
                transcript = "Gasté 850 pesos comprando comida para la casa",
                categories = listOf(Category(1L, "Alimentación", TransactionType.EXPENSE, "restaurant", true)),
                current = VoiceTransactionDraft(type = TransactionType.EXPENSE, date = LocalDate.now()),
            )

            assertEquals(InterpretationEngine.ON_DEVICE_LLM, result.engine)
            val interpreted = result.command as? VoiceTransactionCommand.Apply
            assertTrue("La inferencia local debe producir un borrador", interpreted != null)
            assertEquals("Borrador obtenido: ${result.command}", 85_000L, interpreted?.draft?.amountInCents)
            assertEquals("Borrador obtenido: ${result.command}", 1L, interpreted?.draft?.categoryId)

            val incomeResult = engine.interpret(
                transcript = "Recibí 15,000 pesos por un trabajo",
                categories = listOf(Category(2L, "Salario", TransactionType.INCOME, "payments", true)),
                current = VoiceTransactionDraft(type = TransactionType.INCOME, date = LocalDate.now()),
            )
            assertEquals(InterpretationEngine.ON_DEVICE_LLM, incomeResult.engine)
            val interpretedIncome = incomeResult.command as? VoiceTransactionCommand.Apply
            assertEquals("Borrador obtenido: ${incomeResult.command}", 1_500_000L, interpretedIncome?.draft?.amountInCents)
            assertEquals("Borrador obtenido: ${incomeResult.command}", 2L, interpretedIncome?.draft?.categoryId)
        } finally {
            engine.unloadActiveModel()
            if (engine.isModelInstalled(previousSelection)) engine.setActiveModel(previousSelection)
        }
    }

    private fun seedModelFromExternalTestFile(context: Context) {
        val source = context.getExternalFilesDir(null)
            ?.resolve("qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm")
            ?: return
        if (!source.isFile) return
        val destination = File(context.filesDir, "language_models/qwen3_0_6b_dynamic_int4.litertlm")
        if (!destination.exists()) {
            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = false)
        }
    }
}
