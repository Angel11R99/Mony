package com.angel.mony.data.nlp

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VoiceFieldReconciliationTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val categories = listOf(
        Category(1L, "Compras", TransactionType.EXPENSE, "shopping", true),
        Category(2L, "Trabajo", TransactionType.INCOME, "work", true),
        Category(3L, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
        Category(4L, "Compras", TransactionType.INCOME, "coins", true),
        Category(5L, "Compras antiguas", TransactionType.EXPENSE, "archive", false),
    )
    private fun check(
        speech: String,
        modelType: TransactionType?,
        modelAmount: Long?,
        modelCategory: String?,
        modelDate: LocalDate? = today,
    ) = VoiceFieldReconciliation.verify(speech, modelType, modelAmount,
        modelCategory, modelDate, categories, today)

    @Test fun yesterdayIncomeOverridesWrongModelDateAndAmount() {
        val got = check("Agrega 20 mil pesos de un ingreso que nos cayó ayer por un trabajo bueno",
            TransactionType.INCOME, 20_000L, null)
        assertEquals(TransactionType.INCOME, got.type)
        assertEquals(2_000_000L, got.amountInCents)
        assertEquals(today.minusDays(1), got.date)
        assertEquals(2L, got.categoryId)
    }
    @Test fun yesterdayPurchaseFindsPluralCategory() {
        val got = check("Un gasto de una compra que hicimos ayer por 500 pesos",
            TransactionType.EXPENSE, 500L, null)
        assertEquals(TransactionType.EXPENSE, got.type)
        assertEquals(50_000L, got.amountInCents)
        assertEquals(today.minusDays(1), got.date)
        assertEquals(1L, got.categoryId)
    }
    @Test fun purchasedItemCanMatchPurchasesCategory() {
        val got = check("Compramos ayer 500 pesos de materiales",
            TransactionType.EXPENSE, 50_000L, null)
        assertEquals(1L, got.categoryId)
        assertEquals(today.minusDays(1), got.date)
    }
    @Test fun unrecognizedCategoryIsExplicitlyRequestedNotSilentlyIgnored() {
        val got = check("Me pagaron 500 pesos por un encargo",
            TransactionType.INCOME, 50_000L, null)
        assertNull(got.categoryId)
        assertTrue(got.categoryUnresolved)
    }
    @Test fun lastNightAndDayBeforeYesterday() {
        assertEquals(today.minusDays(1),
            check("Gasté 500 pesos anoche", TransactionType.EXPENSE, null, null).date)
        assertEquals(today.minusDays(2),
            check("Compré algo anteayer", TransactionType.EXPENSE, null, null).date)
    }
    @Test fun validModelCategorySelectedEvenIfNotSpoken() {
        assertEquals(3L, check("Cenamos por 600 pesos", TransactionType.EXPENSE,
            60_000L, "Alimentación").categoryId)
    }
    @Test fun invalidOrInactiveCategoriesNeverSelected() {
        val got = check("Un gasto de 500 pesos", TransactionType.EXPENSE, 50_000L, "Trabajo")
        assertNull(got.categoryId)
        assertTrue(got.categoryUnresolved)
        assertNull(check("Una compra por 500 pesos", TransactionType.INCOME, 50_000L,
            "Compras antiguas").categoryId.takeIf { it == 5L })
    }
    @Test fun conflictingRelativeDatesDoNotOverrideContextualModelDate() {
        val got = check("Me pagaron hoy por un trabajo de ayer", TransactionType.INCOME,
            20_000L, "Trabajo", modelDate = today)
        assertEquals(today, got.date)
    }
    @Test fun amountWithNoMonetaryMarkerPreservesModelInference() {
        assertEquals(23_500L, check("Compré 3 botellas", TransactionType.EXPENSE,
            23_500L, null).amountInCents)
    }
    @Test fun wrongIncomeTypeFromModelIsOverriddenByExplicitExpense() {
        assertEquals(TransactionType.EXPENSE,
            check("Gasté 500 pesos en comida", TransactionType.INCOME, 50_000L, null).type)
    }
    @Test fun multipleMonetaryAmountsRemainUnresolvedForOverride() {
        assertEquals(50_000L, check("Pagamos 500 pesos y 200 pesos de propina",
            TransactionType.EXPENSE, 50_000L, null).amountInCents)
    }
}
