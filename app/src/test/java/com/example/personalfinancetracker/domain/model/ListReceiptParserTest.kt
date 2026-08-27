package com.example.personalfinancetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListReceiptParserTest {
    @Test fun `extracts supported latin money formats`() {
        val result = ListOcrMoneyParser.extractCandidates(
            "RD$ 89.95 | $89,95 | RD$1,234.56 | 1.234,56",
        )
        assertEquals(listOf(8_995L, 123_456L), result.map { it.amountInCents })
    }

    @Test fun `deduplicates equivalent monetary candidates preserving first raw value`() {
        val result = ListOcrMoneyParser.extractCandidates("RD$ 89.95\n$89,95\n89.95")
        assertEquals(1, result.size)
        assertEquals(8_995L, result.single().amountInCents)
        assertEquals("RD$ 89.95", result.single().rawValue)
    }

    @Test fun `extracts multiple distinct candidates without selecting one`() {
        val result = ListOcrMoneyParser.extractCandidates("Subtotal 100,00 Total 118.00")
        assertEquals(listOf(10_000L, 11_800L), result.map { it.amountInCents })
    }

    @Test fun `rejects integers malformed grouping and overflow`() {
        val result = ListOcrMoneyParser.extractCandidates(
            "RD$ 100; 1,234; 12.3; 1,23,4.56; 999999999999999999999999.99",
        )
        assertTrue(result.isEmpty())
    }

    @Test fun `ticket classifies labeled amounts and detects product prices`() {
        val result = ListTicketParser.parse(
            """
            Café molido RD$ 250.00
            SUBTOTAL 250,00
            ITBIS RD$45.00
            Descuento $10,00
            Envío 25.00
            Cargo por servicio 5,00
            TOTAL A PAGAR RD$315.00
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                TicketAmountKind.PRODUCTO,
                TicketAmountKind.SUBTOTAL,
                TicketAmountKind.TAX,
                TicketAmountKind.DISCOUNT,
                TicketAmountKind.SHIPPING,
                TicketAmountKind.SERVICE,
                TicketAmountKind.TOTAL,
            ),
            result.candidates.map { it.kind },
        )
        assertEquals(listOf(25_000L, 25_000L, 4_500L, 1_000L, 2_500L, 500L, 31_500L), result.candidates.map { it.amountInCents })
    }

    @Test fun `subtotal is not misclassified as total`() {
        val result = ListTicketParser.parse("Subtotal RD$ 1,000.00")
        assertEquals(TicketAmountKind.SUBTOTAL, result.candidates.single().kind)
    }

    @Test fun `accented tax shipping and service labels are normalized`() {
        val result = ListTicketParser.parse("Impuesto 18,00\nENVÍO 20.00\nPropina 10,00")
        assertEquals(
            listOf(TicketAmountKind.TAX, TicketAmountKind.SHIPPING, TicketAmountKind.SERVICE),
            result.candidates.map { it.kind },
        )
    }

    @Test fun `label only line propagates context to next line with amount`() {
        val result = ListTicketParser.parse("ITBIS\n18.00")
        assertEquals(2, result.candidates.size)
        assertEquals(TicketAmountKind.TAX, result.candidates[0].kind)
        assertEquals(1_800L, result.candidates[0].amountInCents)
        assertEquals(TicketAmountKind.MONTO_DETECTADO, result.candidates[1].kind)
        assertEquals(1_800L, result.candidates[1].amountInCents)
    }

    @Test fun `two consecutive labeled lines do not leak context to unrelated amounts`() {
        val result = ListTicketParser.parse("ITBIS\n18.00\nDescuento\n5.00")
        assertEquals(4, result.candidates.size)
        assertEquals(listOf(TicketAmountKind.TAX, TicketAmountKind.MONTO_DETECTADO, TicketAmountKind.DISCOUNT, TicketAmountKind.MONTO_DETECTADO), result.candidates.map { it.kind })
    }

    @Test fun `context is cleared when labeled line has amounts`() {
        val result = ListTicketParser.parse("ITBIS 18.00\n5.00")
        assertEquals(2, result.candidates.size)
        assertEquals(TicketAmountKind.TAX, result.candidates[0].kind)
        assertEquals(TicketAmountKind.PRODUCTO, result.candidates[1].kind)
    }

    @Test fun `uses preceding product description when OCR separates it from price`() {
        val result = ListTicketParser.parse("Leche Evaporada Carnation Queso 135 ml\n64.95")

        assertEquals(1, result.candidates.size)
        assertEquals(TicketAmountKind.PRODUCTO, result.candidates.single().kind)
        assertEquals(6_495L, result.candidates.single().amountInCents)
        assertEquals("Leche Evaporada Carnation Queso 135 ml", result.candidates.single().productName)
    }

    @Test fun `does not use ticket column header as product name`() {
        val result = ListTicketParser.parse("Producto\n64.95")

        assertEquals(null, result.candidates.single().productName)
    }

    @Test fun `candidate can be edited immutably without parser assumptions`() {
        val parsed = ListTicketParser.parse("Total 100.00").candidates.single()
        val edited = parsed.copy(amountInCents = 9_900, kind = TicketAmountKind.SUBTOTAL)
        assertEquals(9_900L, edited.amountInCents)
        assertEquals(TicketAmountKind.SUBTOTAL, edited.kind)
        assertEquals(10_000L, parsed.amountInCents)
    }
}
