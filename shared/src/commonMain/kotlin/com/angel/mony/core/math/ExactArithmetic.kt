package com.angel.mony.core.math

/**
 * Aritmética entera verificada para código común.
 *
 * `java.lang.Math.multiplyExact` y `Math.addExact` lanzan `ArithmeticException` al
 * desbordar, pero no existen en Kotlin/Native. Estas variantes devuelven `null` en lugar de
 * lanzar, de modo que quien llama pueda tratar el desbordamiento como dato inválido en lugar
 * de propagar una excepción, que es como se usaban envueltas en `runCatching`.
 *
 * Los importes son centavos en `Long`, así que el desbordamiento es alcanzable con
 * multiplicaciones de cantidades por precios o al acumular subtotales.
 */

/** Devuelve `this * other`, o `null` si el resultado no cabe en un `Long`. */
fun Long.multiplyExactOrNull(other: Long): Long? {
    if (this == 0L || other == 0L) return 0L
    if (this == -1L && other == Long.MIN_VALUE) return null
    if (other == -1L && this == Long.MIN_VALUE) return null
    val result = this * other
    return if (result / other == this) result else null
}

/** Devuelve `this + other`, o `null` si el resultado no cabe en un `Long`. */
fun Long.addExactOrNull(other: Long): Long? {
    val result = this + other
    return if (((this xor result) and (other xor result)) < 0L) null else result
}

/** Devuelve `this - other`, o `null` si el resultado no cabe en un `Long`. */
fun Long.subtractExactOrNull(other: Long): Long? {
    val result = this - other
    return if (((this xor other) and (this xor result)) < 0L) null else result
}