package com.example.personalfinancetracker.domain.model

import java.time.Instant

enum class ShoppingListStatus {
    PENDING,
    SHOPPING,
    COMPLETED,
}

data class ShoppingList(
    val id: Long = 0,
    val name: String,
    val status: ShoppingListStatus = ShoppingListStatus.PENDING,
    val budgetInCents: Long? = null,
    val expenseTransactionId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Shopping list name cannot be blank" }
        require(budgetInCents == null || budgetInCents >= 0) { "Budget cannot be negative" }
    }
}

data class ShoppingListItem(
    val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val quantity: Int = 1,
    val estimatedUnitPriceInCents: Long? = null,
    val actualUnitPriceInCents: Long? = null,
    val barcode: String? = null,
    val isPurchased: Boolean = false,
    val isIdentified: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Shopping list item name cannot be blank" }
        require(quantity >= 1) { "Quantity must be at least one" }
        require(estimatedUnitPriceInCents == null || estimatedUnitPriceInCents >= 0) {
            "Estimated price cannot be negative"
        }
        require(actualUnitPriceInCents == null || actualUnitPriceInCents >= 0) {
            "Actual price cannot be negative"
        }
    }
}

data class ShoppingAdjustment(
    val id: Long = 0,
    val shoppingListId: Long,
    val name: String,
    val isPositive: Boolean,
    val amountInCents: Long,
    val createdAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Shopping adjustment name cannot be blank" }
        require(amountInCents >= 0) { "Adjustment amount cannot be negative" }
    }
}

data class KnownProduct(
    val barcode: String,
    val name: String,
    val lastPriceInCents: Long? = null,
    val lastUsedAt: Instant,
) {
    init {
        require(barcode.isNotBlank()) { "Barcode cannot be blank" }
        require(name.isNotBlank()) { "Known product name cannot be blank" }
        require(lastPriceInCents == null || lastPriceInCents >= 0) { "Last price cannot be negative" }
    }
}

data class ShoppingListDetails(
    val list: ShoppingList,
    val items: List<ShoppingListItem>,
    val adjustments: List<ShoppingAdjustment>,
) {
    val estimatedSubtotalInCents: Long
        get() = items.moneySum { item ->
            item.estimatedUnitPriceInCents?.let { Math.multiplyExact(item.quantity.toLong(), it) } ?: 0L
        }

    val purchasedSubtotalInCents: Long
        get() = items.moneySum { item ->
            if (item.isPurchased) {
                item.actualUnitPriceInCents?.let { Math.multiplyExact(item.quantity.toLong(), it) } ?: 0L
            } else {
                0L
            }
        }

    val adjustmentTotalInCents: Long
        get() = adjustments.moneySum { adjustment ->
            if (adjustment.isPositive) adjustment.amountInCents else Math.negateExact(adjustment.amountInCents)
        }

    val actualTotalInCents: Long
        get() = Math.addExact(purchasedSubtotalInCents, adjustmentTotalInCents)

    val remainingBudgetInCents: Long?
        get() = list.budgetInCents?.let { Math.subtractExact(it, actualTotalInCents) }
}

data class ShoppingListOverview(
    val list: ShoppingList,
    val itemCount: Int,
    val totalInCents: Long,
)

private inline fun <T> Iterable<T>.moneySum(value: (T) -> Long): Long =
    fold(0L) { total, item -> Math.addExact(total, value(item)) }
