package com.angel.mony.core

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class HistorySpreadsheetWriterTest {
    @Test
    fun `creates excel workbook with summary formulas and editable movements`() {
        val output = ByteArrayOutputStream()
        val transactions = listOf(
            transaction(1, TransactionType.INCOME, 1, "Nómina", 25_000_00, LocalDate.of(2026, 9, 15)),
            transaction(2, TransactionType.EXPENSE, 2, "Café & pan", 450_50, LocalDate.of(2026, 9, 16)),
        )

        HistorySpreadsheetWriter.writeTo(
            output,
            transactions,
            mapOf(
                1L to Category(1, "Salario", TransactionType.INCOME, "payments", true),
                2L to Category(2, "Alimentación", TransactionType.EXPENSE, "restaurant", true),
            ),
            HistoryPdfMeta(
                periodLabel = "Ciclo: 15/09/2026 - 29/09/2026",
                filterLines = listOf("Tipo: Todos"),
                sortLabel = "Más recientes",
                generatedAt = LocalDate.of(2026, 9, 29),
            ),
        )
        val entries = unzip(output.toByteArray())
        assertTrue(entries.keys.containsAll(listOf("xl/workbook.xml", "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml")))
        entries.filterKeys { it.endsWith(".xml") || it.endsWith(".rels") }.forEach { (path, xml) ->
            assertTrue("$path debe comenzar con la declaración XML", xml.startsWith("<?xml"))
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        }
        assertTrue(entries.getValue("xl/workbook.xml").contains("name=\"Resumen\""))
        assertTrue(entries.getValue("xl/workbook.xml").contains("name=\"Movimientos\""))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("SUMIF(&apos;Movimientos&apos;!B:B,&quot;Ingreso&quot;"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("<f>B7-B8</f>"))
        assertTrue(entries.getValue("xl/worksheets/sheet2.xml").contains("<autoFilter ref=\"A4:E6\"/>"))
        assertTrue(entries.getValue("xl/worksheets/sheet2.xml").contains("Café &amp; pan"))
        assertTrue(entries.getValue("xl/worksheets/sheet2.xml").contains("<v>25000.0</v>"))
        assertTrue(entries.getValue("xl/styles.xml").contains("&quot;RD\$&quot; #,##0.00"))
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        return entries
    }

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
        createdAt = Instant.parse("2026-09-15T10:00:00Z"),
        updatedAt = Instant.parse("2026-09-15T10:00:00Z"),
    )
}
