package com.angel.mony.presentation.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketTotalCalculatorTest {
    @Test
    fun `sums included products and signed adjustments`() {
        val products = listOf(
            product(quantity = 2, price = 100),
            product(quantity = 3, price = 50, included = false),
        )
        val adjustments = listOf(
            AdjustmentDraft("Impuesto", isPositive = true, amountInCents = 20),
            AdjustmentDraft("Descuento", isPositive = false, amountInCents = 5),
        )

        assertEquals(215L, calculateTicketReviewTotal(products, adjustments))
    }

    @Test
    fun `returns no total when multiplication overflows`() {
        assertNull(
            calculateTicketReviewTotal(
                products = listOf(product(quantity = Int.MAX_VALUE, price = Long.MAX_VALUE)),
                adjustments = emptyList(),
            ),
        )
    }

    @Test
    fun `returns no total when adjustment or final sum overflows`() {
        val product = listOf(product(quantity = 1, price = Long.MAX_VALUE))
        val positiveAdjustment = listOf(AdjustmentDraft("Impuesto", true, 1))
        assertNull(calculateTicketReviewTotal(product, positiveAdjustment))
        assertNull(
            calculateTicketReviewTotal(
                products = emptyList(),
                adjustments = listOf(AdjustmentDraft("Ajuste", false, Long.MIN_VALUE)),
            ),
        )
    }

    private fun product(
        quantity: Int,
        price: Long,
        included: Boolean = true,
    ) = TicketProductDraft(
        occurrenceId = "1",
        detectedText = "Producto",
        name = "Producto",
        quantity = quantity,
        unitPriceInCents = price,
        selectedItemId = null,
        suggestedItemId = null,
        confidence = com.angel.mony.domain.model.RecognitionConfidence.HIGH,
        included = included,
    )
}
