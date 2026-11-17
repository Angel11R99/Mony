package com.angel.mony.presentation.list



internal fun calculateTicketReviewTotal(
    products: List<TicketProductDraft>,
    adjustments: List<AdjustmentDraft>,
): Long? = runCatching {
    val productTotal = products.filter { it.included }.fold(0L) { total, product ->
        Math.addExact(total, Math.multiplyExact(product.quantity.toLong(), product.unitPriceInCents))
    }
    val adjustmentTotal = adjustments.fold(0L) { total, adjustment ->
        val signedAmount = if (adjustment.isPositive) adjustment.amountInCents
        else Math.negateExact(adjustment.amountInCents)
        Math.addExact(total, signedAmount)
    }
    Math.addExact(productTotal, adjustmentTotal)
}.getOrNull()
