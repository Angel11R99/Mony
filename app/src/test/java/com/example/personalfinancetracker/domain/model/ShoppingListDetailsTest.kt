package com.example.personalfinancetracker.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingListDetailsTest {
    private val now = Instant.parse("2026-08-26T12:00:00Z")
    private val list = ShoppingList(name = "Supermercado", createdAt = now, updatedAt = now)

    @Test fun `finalizable total includes manually added pending products`() {
        val details = ShoppingListDetails(
            list,
            listOf(item(id = 1, quantity = 2, actual = 7_500, purchased = false)),
            emptyList(),
        )
        assertEquals(15_000L, details.finalizableTotalInCents)
        assertEquals(0L, details.actualTotalInCents)
    }

    @Test
    fun `total uses purchased real prices and signed adjustments`() {
        val details = ShoppingListDetails(
            list = list,
            items = listOf(
                item(id = 1, quantity = 2, actual = 8_500, purchased = true),
                item(id = 2, quantity = 1, actual = 5_000, purchased = false),
            ),
            adjustments = listOf(
                ShoppingAdjustment(1, list.id, "ITBIS", true, 1_500, now),
                ShoppingAdjustment(2, list.id, "Cupón", false, 500, now),
            ),
        )

        assertEquals(17_000L, details.purchasedSubtotalInCents)
        assertEquals(1_000L, details.adjustmentTotalInCents)
        assertEquals(18_000L, details.actualTotalInCents)
    }

    @Test
    fun `purchased item without price contributes zero when user forces completion`() {
        val details = ShoppingListDetails(
            list = list,
            items = listOf(item(id = 1, quantity = 2, actual = null, purchased = true)),
            adjustments = emptyList(),
        )

        assertEquals(0L, details.purchasedSubtotalInCents)
    }

    @Test(expected = ArithmeticException::class)
    fun `line multiplication overflow is detected`() {
        ShoppingListDetails(
            list = list,
            items = listOf(item(id = 1, quantity = 2, actual = Long.MAX_VALUE, purchased = true)),
            adjustments = emptyList(),
        ).actualTotalInCents
    }

    private fun item(
        id: Long,
        quantity: Int,
        actual: Long?,
        purchased: Boolean,
    ) = ShoppingListItem(
        id = id,
        shoppingListId = list.id,
        name = "Producto $id",
        quantity = quantity,
        actualUnitPriceInCents = actual,
        isPurchased = purchased,
        createdAt = now,
        updatedAt = now,
    )
}
