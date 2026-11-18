package com.angel.mony.presentation.transactions

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceDraftField
import com.angel.mony.domain.model.VoiceTransactionCommand
import com.angel.mony.domain.model.VoiceTransactionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class VoiceFormAutofillTest {
    private val categories = listOf(
        Category(1L, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
        Category(2L, "Trabajo", TransactionType.INCOME, "briefcase", true),
        Category(3L, "Transporte", TransactionType.EXPENSE, "car", false),
    )
    private val empty = VoiceFormFields(null, "", null, "2026-11-17", "", "")

    @Test fun completeExpensePopulatesTypeAmountCategoryDateAndNote() {
        val draft = VoiceTransactionDraft(
            type = TransactionType.EXPENSE, amountInCents = 50_000L,
            categoryId = 1L, date = LocalDate.of(2026, 11, 16),
            note = "Cena de anoche",
        )
        val command = VoiceTransactionCommand.Apply(
            draft, setOf(VoiceDraftField.TYPE, VoiceDraftField.AMOUNT,
                VoiceDraftField.CATEGORY, VoiceDraftField.DATE, VoiceDraftField.NOTE)
        )
        val result = applyVoiceFields(empty, command, categories)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("500.00", result.amount)
        assertEquals(1L, result.categoryId)
        assertEquals("2026-11-16", result.date)
        assertEquals("Cena de anoche", result.note)
    }

    @Test fun incomeKeepsPreviouslyEnteredFieldsWhenNotMentioned() {
        val existing = empty.copy(note = "Trabajo de instalación", amount = "300.00")
        val command = VoiceTransactionCommand.Apply(
            VoiceTransactionDraft(type = TransactionType.INCOME, amountInCents = 100_000L),
            setOf(VoiceDraftField.TYPE, VoiceDraftField.AMOUNT),
        )
        val result = applyVoiceFields(existing, command, categories)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("1000.00", result.amount)
        assertEquals("Trabajo de instalación", result.note)
        assertEquals("2026-11-17", result.date)
    }

    @Test fun changingTypeRemovesPreviouslySelectedCategoryOfDifferentType() {
        val existing = empty.copy(type = TransactionType.EXPENSE, categoryId = 1L)
        val command = VoiceTransactionCommand.Apply(
            VoiceTransactionDraft(type = TransactionType.INCOME), setOf(VoiceDraftField.TYPE)
        )
        val result = applyVoiceFields(existing, command, categories)
        assertEquals(TransactionType.INCOME, result.type)
        assertNull(result.categoryId)
    }

    @Test fun inactiveOrWrongTypeCategoryCannotBeApplied() {
        for (categoryId in listOf(2L, 3L, 999L)) {
            val command = VoiceTransactionCommand.Apply(
                VoiceTransactionDraft(type = TransactionType.EXPENSE, categoryId = categoryId),
                setOf(VoiceDraftField.TYPE, VoiceDraftField.CATEGORY)
            )
            val result = applyVoiceFields(empty, command, categories)
            assertNull(result.categoryId)
        }
    }

    @Test fun emptyOrUnmentionedFieldsDoNotEraseUserValues() {
        val existing = VoiceFormFields(TransactionType.EXPENSE, "600.00", 1L,
            "2026-11-17", "Comida familiar", "Efectivo")
        val command = VoiceTransactionCommand.Apply(
            VoiceTransactionDraft(type = TransactionType.EXPENSE, note = null),
            setOf(VoiceDraftField.TYPE),
        )
        assertEquals(existing, applyVoiceFields(existing, command, categories))
    }
}
