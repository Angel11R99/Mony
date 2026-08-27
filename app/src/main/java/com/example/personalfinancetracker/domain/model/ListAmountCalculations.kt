package com.example.personalfinancetracker.domain.model

data class ListAmountAdjustments(
    val discountInCents: Long = 0,
    val taxInCents: Long = 0,
    val shippingInCents: Long = 0,
    val serviceInCents: Long = 0,
)

fun calculateListLineTotal(
    unitPriceInCents: Long,
    quantity: Long,
    adjustments: ListAmountAdjustments = ListAmountAdjustments(),
): Long {
    require(unitPriceInCents >= 0) { "El precio no puede ser negativo" }
    require(quantity >= 0) { "La cantidad no puede ser negativa" }
    return applyListAdjustments(Math.multiplyExact(unitPriceInCents, quantity), adjustments)
}

fun calculateListTotal(
    lineTotalsInCents: Iterable<Long>,
    adjustments: ListAmountAdjustments = ListAmountAdjustments(),
): Long {
    val subtotal = lineTotalsInCents.fold(0L) { total, lineTotal ->
        require(lineTotal >= 0) { "El total de línea no puede ser negativo" }
        Math.addExact(total, lineTotal)
    }
    return applyListAdjustments(subtotal, adjustments)
}

private fun applyListAdjustments(subtotalInCents: Long, adjustments: ListAmountAdjustments): Long {
    require(adjustments.discountInCents >= 0) { "El descuento no puede ser negativo" }
    require(adjustments.taxInCents >= 0) { "El impuesto no puede ser negativo" }
    require(adjustments.shippingInCents >= 0) { "El envío no puede ser negativo" }
    require(adjustments.serviceInCents >= 0) { "El servicio no puede ser negativo" }
    require(adjustments.discountInCents <= subtotalInCents) { "El descuento supera el subtotal" }

    var total = Math.subtractExact(subtotalInCents, adjustments.discountInCents)
    total = Math.addExact(total, adjustments.taxInCents)
    total = Math.addExact(total, adjustments.shippingInCents)
    return Math.addExact(total, adjustments.serviceInCents)
}
