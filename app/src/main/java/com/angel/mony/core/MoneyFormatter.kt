package com.angel.mony.core

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-DO")).apply {
        currency = Currency.getInstance("DOP")
    }

    fun format(cents: Long): String = synchronized(formatter) {
        formatter.format(cents / 100.0)
    }

    fun parseToCents(value: String): Long? = runCatching {
        value.trim().replace(',', '.').toBigDecimalOrNull()
            ?.movePointRight(2)?.longValueExact()
    }.getOrNull()
}
