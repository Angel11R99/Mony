package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ListAmountCalculationsTest {
    @Test fun `calculates line quantity and adjustments in cents`() {
        assertEquals(
            32_500L,
            calculateListLineTotal(
                unitPriceInCents = 10_000,
                quantity = 3,
                adjustments = ListAmountAdjustments(discountInCents = 1_000, taxInCents = 2_500, serviceInCents = 1_000),
            ),
        )
    }

    @Test fun `calculates totals with all adjustments`() {
        assertEquals(
            35_000L,
            calculateListTotal(
                listOf(10_000, 20_000),
                ListAmountAdjustments(2_000, 3_000, 2_500, 1_500),
            ),
        )
    }

    @Test fun `supports empty and zero values`() {
        assertEquals(0L, calculateListLineTotal(500, 0))
        assertEquals(0L, calculateListTotal(emptyList()))
    }

    @Test fun `rejects invalid negative values and excessive discount`() {
        assertThrows(IllegalArgumentException::class.java) { calculateListLineTotal(-1, 1) }
        assertThrows(IllegalArgumentException::class.java) { calculateListLineTotal(1, -1) }
        assertThrows(IllegalArgumentException::class.java) { calculateListTotal(listOf(-1)) }
        assertThrows(IllegalArgumentException::class.java) {
            calculateListTotal(listOf(100), ListAmountAdjustments(discountInCents = 101))
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculateListTotal(listOf(100), ListAmountAdjustments(taxInCents = -1))
        }
    }

    @Test fun `throws on multiplication and addition overflow`() {
        assertThrows(ArithmeticException::class.java) { calculateListLineTotal(Long.MAX_VALUE, 2) }
        assertThrows(ArithmeticException::class.java) { calculateListTotal(listOf(Long.MAX_VALUE, 1)) }
        assertThrows(ArithmeticException::class.java) {
            calculateListTotal(listOf(Long.MAX_VALUE), ListAmountAdjustments(taxInCents = 1))
        }
    }
}
