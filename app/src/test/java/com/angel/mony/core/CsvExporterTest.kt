package com.angel.mony.core

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CsvExporterTest {
    private fun transaction(
        id: Long,
        type: TransactionType,
        categoryId: Long,
        description: String,
        amountInCents: Long,
        date: LocalDate,
    ) = FinanceTransaction(
        id = id,
        amountInCents = amountInCents,
        type = type,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = Instant.parse("2026-08-24T10:00:00Z"),
        updatedAt = Instant.parse("2026-08-24T10:00:00Z"),
    )

    @Test fun `builds header and one row`() {
        val csv = CsvExporter.buildCsv(
            listOf(
                transaction(1, TransactionType.EXPENSE, 5, "Compra", 125_050, LocalDate.of(2026, 8, 24))
            ),
            mapOf(5L to Category(5, "Compras", TransactionType.EXPENSE, "more_horiz", true)),
        )
        assertEquals(
            "\uFEFF\"Fecha\",\"Tipo\",\"Categoría\",\"Monto (RD\$)\",\"Descripción\"\r\n" +
                "\"2026-08-24\",\"Gasto\",\"Compras\",\"1250.50\",\"Compra\"",
            csv,
        )
    }

    @Test fun `exports income rows and unknown category fallback`() {
        val csv = CsvExporter.buildCsv(
            listOf(transaction(1, TransactionType.INCOME, 99, "Sueldo", 25_000_00, LocalDate.of(2026, 8, 1))),
            emptyMap(),
        )
        assertTrue(csv.contains("\"2026-08-01\",\"Ingreso\",\"Sin categoría\",\"25000.00\",\"Sueldo\""))
    }

    @Test fun `escapes quotes commas and line breaks in fields`() {
        val csv = CsvExporter.buildCsv(
            listOf(
                transaction(1, TransactionType.EXPENSE, 2, "Pago \"extra\", con\nsalto", 100, LocalDate.of(2026, 8, 2))
            ),
            mapOf(2L to Category(2, "Deudas, préstamos", TransactionType.EXPENSE, "account_balance", true)),
        )
        assertTrue(csv.contains("\"\"extra\"\", con\nsalto"))
        assertTrue(csv.contains("\"Deudas, préstamos\""))
    }

    @Test fun `formats cents with two decimals`() {
        val csv = CsvExporter.buildCsv(
            listOf(transaction(1, TransactionType.EXPENSE, 2, "Café", 5, LocalDate.of(2026, 8, 3))),
            emptyMap(),
        )
        assertTrue(csv.contains("\"0.05\""))
    }

    @Test fun `parses exported csv back to movements`() {
        val csv = CsvExporter.buildCsv(
            listOf(
                transaction(1, TransactionType.EXPENSE, 5, "Almuerzo", 125_050, LocalDate.of(2026, 8, 24)),
                transaction(2, TransactionType.INCOME, 1, "", 25_000_00, LocalDate.of(2026, 8, 1)),
            ),
            mapOf(
                5L to Category(5, "Comidas", TransactionType.EXPENSE, "restaurant", true),
                1L to Category(1, "Salario", TransactionType.INCOME, "payments", true),
            ),
        )
        val movements = CsvExporter.parseBackup(csv.removePrefix(CsvExporter.UTF8_BOM))
        assertEquals(2, movements.size)
        assertEquals(LocalDate.of(2026, 8, 24), movements[0].date)
        assertEquals(TransactionType.EXPENSE, movements[0].type)
        assertEquals("Comidas", movements[0].categoryName)
        assertEquals(125_050L, movements[0].amountInCents)
        assertEquals("Almuerzo", movements[0].description)
        assertEquals("Salario", movements[1].categoryName)
        assertNull(movements[1].description)
    }

    @Test fun `preserves spanish accents through round trip`() {
        val csv = CsvExporter.buildCsv(
            listOf(
                transaction(1, TransactionType.EXPENSE, 3, "Cita médica ñandú ültimo", 999, LocalDate.of(2026, 7, 15))
            ),
            mapOf(3L to Category(3, "Salúd y Educación", TransactionType.EXPENSE, "medical", true)),
        )
        val movements = CsvExporter.parseBackup(csv.removePrefix(CsvExporter.UTF8_BOM))
        assertEquals("Salúd y Educación", movements[0].categoryName)
        assertEquals("Cita médica ñandú ültimo", movements[0].description)
    }

    @Test fun `parses quoted multiline and comma fields`() {
        val csv = CsvExporter.buildCsv(
            listOf(
                transaction(1, TransactionType.EXPENSE, 2, "Pago \"extra\", con\nsalto", 100, LocalDate.of(2026, 8, 2))
            ),
            mapOf(2L to Category(2, "Deudas, préstamos", TransactionType.EXPENSE, "card", true)),
        )
        val movements = CsvExporter.parseBackup(csv.removePrefix(CsvExporter.UTF8_BOM))
        assertEquals("Deudas, préstamos", movements[0].categoryName)
        assertEquals("Pago \"extra\", con\nsalto", movements[0].description)
    }

    @Test(expected = IllegalStateException::class)
    fun `rejects file with unexpected header`() {
        CsvExporter.parseBackup("\"Fecha\",\"Concepto\"\r\n\"2026-01-01\",\"Otro\"")
    }

    @Test(expected = IllegalStateException::class)
    fun `rejects row with invalid amount`() {
        CsvExporter.parseBackup(
            "\"Fecha\",\"Tipo\",\"Categoría\",\"Monto (RD\$)\",\"Descripción\"\r\n" +
                "\"2026-08-24\",\"Gasto\",\"Compras\",\"abc\",\"Nota\""
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `rejects row with invalid type`() {
        CsvExporter.parseBackup(
            "\"Fecha\",\"Tipo\",\"Categoría\",\"Monto (RD\$)\",\"Descripción\"\r\n" +
                "\"2026-08-24\",\"Transferencia\",\"Compras\",\"10.00\",\"Nota\""
        )
    }
}
