package com.angel.mony.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ListProductMatcherTest {
    private fun item(
        id: Long,
        name: String,
        barcode: String? = null,
        purchased: Boolean = false,
        identified: Boolean = false,
    ) = ProductMatchCandidate(id, name, barcode, purchased, identified)

    @Test fun `normalizes accents case punctuation and whitespace`() {
        assertEquals("cafe molido santo domingo", normalizeProductName("  CAFÉ, molido: Santo-Domingo!! "))
    }

    @Test fun `same barcode is a clear match`() {
        val result = ListProductMatcher.match("otro texto", "7460001234567", listOf(
            item(4, "Leche", "7460001234567"),
        ))
        assertEquals(ProductMatchResult.Clear(4), result)
    }

    @Test fun `repeated barcode skips identified item and matches next pending duplicate`() {
        val result = ListProductMatcher.match(null, "123", listOf(
            item(1, "Arroz uno", "123", identified = true),
            item(2, "Arroz dos", "123"),
        ))
        assertEquals(ProductMatchResult.Clear(2), result)
    }

    @Test fun `repeated barcode does not overwrite completed items`() {
        val candidates = listOf(
            item(1, "Arroz", "123", identified = true),
            item(2, "Arroz", "123", purchased = true),
        )
        assertEquals(ProductMatchResult.None, ListProductMatcher.match("Arroz", "123", candidates))
    }

    @Test fun `exact normalized name is clear`() {
        val result = ListProductMatcher.match("Cafe-Molido!", null, listOf(item(8, "Café molido")))
        assertEquals(ProductMatchResult.Clear(8), result)
    }

    @Test fun `clear multi token inclusion matches detailed scan`() {
        val result = ListProductMatcher.match("Leche entera Rica 1 litro", null, listOf(
            item(1, "Leche entera Rica"),
            item(2, "Pan integral"),
        ))
        assertEquals(ProductMatchResult.Clear(1), result)
    }

    @Test fun `generic single token inclusion is only a suggestion`() {
        val result = ListProductMatcher.match("Leche", null, listOf(item(3, "Leche entera Rica")))
        assertEquals(ProductMatchResult.Ambiguous(3, "Leche entera Rica"), result)
    }

    @Test fun `token overlap returns an ambiguous suggestion`() {
        val result = ListProductMatcher.match("Galletas avena miel", null, listOf(
            item(5, "Galletas de avena integral"),
        ))
        assertEquals(ProductMatchResult.Ambiguous(5, "Galletas de avena integral"), result)
    }

    @Test fun `close competing names remain ambiguous`() {
        val result = ListProductMatcher.match("Arroz integral premium", null, listOf(
            item(1, "Arroz integral"),
            item(2, "Arroz integral premium largo"),
        ))
        assertEquals(ProductMatchResult.Ambiguous(2, "Arroz integral premium largo"), result)
    }

    @Test fun `unrelated or blank input has no match`() {
        val candidates = listOf(item(1, "Aceite de oliva"))
        assertEquals(ProductMatchResult.None, ListProductMatcher.match("Jabón líquido", null, candidates))
        assertEquals(ProductMatchResult.None, ListProductMatcher.match("...", null, candidates))
    }

    @Test fun `name matching never overwrites purchased or identified items`() {
        val candidates = listOf(
            item(1, "Huevos", purchased = true),
            item(2, "Huevos", identified = true),
        )
        assertEquals(ProductMatchResult.None, ListProductMatcher.match("Huevos", null, candidates))
    }

    @Test fun `matches abbreviations presentation and brand locally`() {
        val result = ListProductMatcher.match(
            "LECHE EVAP CARN 315G",
            null,
            listOf(item(12, "Leche evaporada Carnation 315 g")),
        )
        assertEquals(ProductMatchResult.Clear(12), result)
    }
}
