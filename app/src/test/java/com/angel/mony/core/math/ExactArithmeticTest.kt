package com.angel.mony.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExactArithmeticTest {

    private fun assertAmount(expected: Long, actual: Long?) {
        if (actual == null) {
            throw AssertionError("Se esperaba $expected pero el resultado fue null (desbordamiento)")
        }
        assertEquals(expected, actual)
    }

    @Test
    fun multiplyExactOrNullDevuelveElProductoCuandoNoDesborda() {
        assertAmount(0L, 0L.multiplyExactOrNull(12345L))
        assertAmount(0L, 12345L.multiplyExactOrNull(0L))
        assertAmount(6L, 2L.multiplyExactOrNull(3L))
        assertAmount(-6L, (-2L).multiplyExactOrNull(3L))
        assertAmount(125_000L, 1250L.multiplyExactOrNull(100L))
        assertAmount(Long.MIN_VALUE, Long.MIN_VALUE.multiplyExactOrNull(1L))
        assertAmount(Long.MAX_VALUE, Long.MAX_VALUE.multiplyExactOrNull(1L))
    }

    @Test
    fun multiplyExactOrNullDevuelveNullAlDesbordar() {
        assertNull(Long.MAX_VALUE.multiplyExactOrNull(2L))
        assertNull(Long.MIN_VALUE.multiplyExactOrNull(-1L))
        assertNull((-1L).multiplyExactOrNull(Long.MIN_VALUE))
        assertNull(Long.MIN_VALUE.multiplyExactOrNull(-2L))
    }

    @Test
    fun addExactOrNullDevuelveLaSumaCuandoNoDesborda() {
        assertAmount(0L, 0L.addExactOrNull(0L))
        assertAmount(125L, 25L.addExactOrNull(100L))
        assertAmount(-125L, 25L.addExactOrNull(-150L))
        assertAmount(Long.MAX_VALUE, Long.MAX_VALUE.addExactOrNull(0L))
        assertAmount(Long.MIN_VALUE, Long.MIN_VALUE.addExactOrNull(0L))
    }

    @Test
    fun addExactOrNullDevuelveNullAlDesbordar() {
        assertNull(Long.MAX_VALUE.addExactOrNull(1L))
        assertNull(Long.MIN_VALUE.addExactOrNull(-1L))
        assertNull(Long.MAX_VALUE.addExactOrNull(Long.MAX_VALUE))
    }

    @Test
    fun subtractExactOrNullDevuelveLaRestaCuandoNoDesborda() {
        assertAmount(0L, 0L.subtractExactOrNull(0L))
        assertAmount(-75L, 25L.subtractExactOrNull(100L))
        assertAmount(Long.MAX_VALUE, 0L.subtractExactOrNull(Long.MIN_VALUE + 1L))
    }

    @Test
    fun subtractExactOrNullDevuelveNullAlDesbordar() {
        assertNull(Long.MIN_VALUE.subtractExactOrNull(1L))
        assertNull(0L.subtractExactOrNull(Long.MIN_VALUE))
    }

    @Test
    fun losCentimalesDeUnImporteCoincidenConLaMultiplicacionExacta() {
        assertAmount(125_075L, 1250L.multiplyExactOrNull(100L)?.addExactOrNull(75L))
        assertNull(1250L.multiplyExactOrNull(100L)?.addExactOrNull(Long.MAX_VALUE))
    }
}