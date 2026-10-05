package com.angel.mony.presentation.transactions

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceTypeBadgeStateTest {
    private val categories = listOf(
        Category(1, "Transporte", TransactionType.EXPENSE, "car", true),
        Category(2, "Salario", TransactionType.INCOME, "payments", true),
        Category(3, "Archivada", TransactionType.EXPENSE, "other", false),
    )

    @Test fun `badge distinguishes unidentified ambiguous expense and income`() {
        assertEquals(VoiceTypeBadgeState.UNIDENTIFIED, voiceTypeBadgeState(null, false))
        assertEquals(VoiceTypeBadgeState.CONFIRMATION_REQUIRED, voiceTypeBadgeState(null, true))
        assertEquals(VoiceTypeBadgeState.CONFIRMATION_REQUIRED, voiceTypeBadgeState(TransactionType.EXPENSE, true))
        assertEquals(VoiceTypeBadgeState.EXPENSE, voiceTypeBadgeState(TransactionType.EXPENSE, false))
        assertEquals(VoiceTypeBadgeState.INCOME, voiceTypeBadgeState(TransactionType.INCOME, false))
    }

    @Test fun `changing type keeps only an active category of the new type`() {
        assertEquals(1L, validCategoryIdForType(1, TransactionType.EXPENSE, categories))
        assertNull(validCategoryIdForType(1, TransactionType.INCOME, categories))
        assertNull(validCategoryIdForType(3, TransactionType.EXPENSE, categories))
    }
}
