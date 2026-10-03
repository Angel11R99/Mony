package com.angel.mony.domain.model

import com.angel.mony.core.math.addExactOrNull
import com.angel.mony.core.math.multiplyExactOrNull
import com.angel.mony.core.math.subtractExactOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class ShoppingListStatus {
    PENDING,
    SHOPPING,
    COMPLETED,
}

enum class ShoppingPaymentMethod {
    CASH,
    DEBIT,
    CREDIT,
}

data class ShoppingList(
    val id: Long = 0,
    val name: String,
    val status: ShoppingListStatus = ShoppingListStatus.PENDING,
    val budgetInCents: Long? = null,
    val expenseTransactionId: Long? = null,
    val payableId: Long? = null,
    val purchaseDate: LocalDate? = null,
    val paymentMethod: ShoppingPaymentMethod? = null,
    val expenseCategoryId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Shopping list name cannot be blank" }
        require(budgetInCents == null || budgetInCents >= 0) { "Budget cannot be negative" }
        require(expenseTransactionId == null || payableId == null) { "A purchase cannot have two financial links" }
    }
}

data class ProductRecognitionAlias(
    val id: Long = 0,
    val detectedText: String,
    val normalizedAlias: String,
    val displayName: String,
    val barcode: String? = null,
    val confirmationCount: Int = 1,
    val lastUsedAt: Instant,
)

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
            item.estimatedUnitPriceInCents?.let { exactMultiply(item.quantity.toLong(), it) } ?: 0L
        }

    val purchasedSubtotalInCents: Long
        get() = items.moneySum { item ->
            if (item.isPurchased) {
                item.actualUnitPriceInCents?.let { exactMultiply(item.quantity.toLong(), it) } ?: 0L
            } else {
                0L
            }
        }

    val finalizableSubtotalInCents: Long
        get() = items.moneySum { item ->
            item.actualUnitPriceInCents?.let { exactMultiply(item.quantity.toLong(), it) } ?: 0L
        }

    val adjustmentTotalInCents: Long
        get() = adjustments.moneySum { adjustment ->
            if (adjustment.isPositive) adjustment.amountInCents else exactSubtract(0L, adjustment.amountInCents)
        }

    val actualTotalInCents: Long
        get() = exactAdd(purchasedSubtotalInCents, adjustmentTotalInCents)

    val finalizableTotalInCents: Long
        get() = exactAdd(finalizableSubtotalInCents, adjustmentTotalInCents)

    val remainingBudgetInCents: Long?
        get() = list.budgetInCents?.let { exactSubtract(it, actualTotalInCents) }
}

data class ShoppingListOverview(
    val list: ShoppingList,
    val itemCount: Int,
    val totalInCents: Long,
)

private inline fun <T> Iterable<T>.moneySum(value: (T) -> Long): Long {
    var total = 0L
    for (item in this) total = exactAdd(total, value(item))
    return total
}

private fun exactAdd(left: Long, right: Long): Long =
    left.addExactOrNull(right) ?: throw ArithmeticException("Long overflow")

private fun exactSubtract(left: Long, right: Long): Long =
    left.subtractExactOrNull(right) ?: throw ArithmeticException("Long overflow")

private fun exactMultiply(left: Long, right: Long): Long =
    left.multiplyExactOrNull(right) ?: throw ArithmeticException("Long overflow")
