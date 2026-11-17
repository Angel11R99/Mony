package com.angel.mony.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PossibleDuplicateDetectorTest {
    private val date = LocalDate.parse("2026-11-17")
    private fun transaction(
        id: Long = 1,
        amount: Long = 85_000,
        type: TransactionType = TransactionType.EXPENSE,
        category: Long = 4,
        note: String? = "Pago del internet",
        on: LocalDate = date,
    ) = FinanceTransaction(id, amount, type, category, note, on)

    @Test fun `same amount category date and similar note is a possible duplicate`() {
        val found = findPossibleDuplicate(transaction(note = "Internet de la casa"), listOf(transaction(note = "Pago internet casa")))
        assertEquals(1L, found?.id)
    }

    @Test fun `same amount and category with unrelated note is allowed`() {
        assertNull(findPossibleDuplicate(transaction(note = "Pago de agua"), listOf(transaction(note = "Pago del internet"))))
    }

    @Test fun `different type category or distant date is not treated as duplicate`() {
        val existing = transaction()
        assertNull(findPossibleDuplicate(transaction(type = TransactionType.INCOME), listOf(existing)))
        assertNull(findPossibleDuplicate(transaction(category = 7), listOf(existing)))
        assertNull(findPossibleDuplicate(transaction(on = date.minusDays(3)), listOf(existing)))
    }
}
