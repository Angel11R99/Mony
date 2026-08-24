package com.example.personalfinancetracker.core

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
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
}
