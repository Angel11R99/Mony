package com.angel.mony.core

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.core.time.toJavaLocalDate
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe un libro XLSX pequeño y compatible con Excel sin depender de servicios ni librerías
 * externas. Los montos y las fechas se conservan como valores numéricos para que el usuario
 * pueda ordenar, filtrar, sumar y crear gráficos después de exportar.
 */
object HistorySpreadsheetWriter {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun writeTo(
        stream: OutputStream,
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
        meta: HistoryPdfMeta,
    ) {
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(FinanceTransaction::amountInCents)
        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf(FinanceTransaction::amountInCents)

        ZipOutputStream(stream).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypes)
            zip.writeEntry("_rels/.rels", rootRelationships)
            zip.writeEntry("docProps/app.xml", appProperties)
            zip.writeEntry("docProps/core.xml", coreProperties(meta.generatedAt))
            zip.writeEntry("xl/workbook.xml", workbook)
            zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelationships)
            zip.writeEntry("xl/styles.xml", styles)
            zip.writeEntry(
                "xl/worksheets/sheet1.xml",
                summarySheet(transactions.size, income, expense, meta),
            )
            zip.writeEntry(
                "xl/worksheets/sheet2.xml",
                movementsSheet(transactions, categories, meta),
            )
        }
    }

    private fun summarySheet(
        movementCount: Int,
        incomeInCents: Long,
        expenseInCents: Long,
        meta: HistoryPdfMeta,
    ): String {
        val filters = meta.filterLines.ifEmpty { listOf("Sin filtros aplicados") }
        val rows = buildString {
            append(row(1, textCell("A1", "Historial financiero", 1)))
            append(row(2, textCell("A2", meta.periodLabel, 9)))
            append(row(3, textCell("A3", "Generado el ${meta.generatedAt.format(displayDateFormatter)}", 9)))
            append(row(5, textCell("A5", "Resumen", 2)))
            append(row(6, textCell("A6", "Movimientos", 11), numberCell("B6", movementCount.toDouble(), 10)))
            append(row(7, textCell("A7", "Ingresos", 11), formulaCell("B7", "SUMIF('Movimientos'!B:B,\"Ingreso\",'Movimientos'!E:E)", incomeInCents / 100.0, 6)))
            append(row(8, textCell("A8", "Gastos", 11), formulaCell("B8", "SUMIF('Movimientos'!B:B,\"Gasto\",'Movimientos'!E:E)", expenseInCents / 100.0, 7)))
            append(row(9, textCell("A9", "Balance", 11), formulaCell("B9", "B7-B8", (incomeInCents - expenseInCents) / 100.0, 8)))
            append(row(11, textCell("A11", "Criterios de exportación", 2)))
            append(row(12, textCell("A12", "Orden", 11), textCell("B12", meta.sortLabel)))
            filters.forEachIndexed { index, filter ->
                val number = 13 + index
                append(row(number, textCell("A$number", if (index == 0) "Filtros" else "", 11), textCell("B$number", filter)))
            }
            val noteRow = 14 + filters.lastIndex
            append(row(noteRow, textCell("A$noteRow", "El detalle editable está en la hoja Movimientos.", 9)))
        }
        val lastRow = 14 + filters.lastIndex
        return worksheetXml(
            dimension = "A1:E$lastRow",
            columns = """
                <cols>
                  <col min="1" max="1" width="24" customWidth="1"/>
                  <col min="2" max="2" width="35" customWidth="1"/>
                  <col min="3" max="5" width="14" customWidth="1"/>
                </cols>
            """.trimIndent(),
            rows = rows,
            mergeCells = listOf("A1:E1", "A2:E2", "A3:E3", "A5:E5", "A11:E11", "A$lastRow:E$lastRow"),
            freezePane = null,
            autoFilter = null,
        )
    }

    private fun movementsSheet(
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
        meta: HistoryPdfMeta,
    ): String {
        val lastRow = transactions.size + 4
        val rows = buildString {
            append(row(1, textCell("A1", "Movimientos", 1)))
            append(row(2, textCell("A2", meta.periodLabel, 9)))
            append(
                row(
                    4,
                    textCell("A4", "Fecha", 3),
                    textCell("B4", "Tipo", 3),
                    textCell("C4", "Categoría", 3),
                    textCell("D4", "Descripción", 3),
                    textCell("E4", "Monto (RD$)", 3),
                )
            )
            transactions.forEachIndexed { index, transaction ->
                val number = index + 5
                val type = if (transaction.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"
                val amountStyle = if (transaction.type == TransactionType.EXPENSE) 7 else 6
                append(
                    row(
                        number,
                        numberCell("A$number", transaction.date.toJavaLocalDate().toExcelSerial(), 5),
                        textCell("B$number", type),
                        textCell("C$number", categories[transaction.categoryId]?.name ?: "Sin categoría"),
                        textCell("D$number", transaction.description.orEmpty()),
                        numberCell("E$number", transaction.amountInCents / 100.0, amountStyle),
                    )
                )
            }
        }
        return worksheetXml(
            dimension = "A1:E$lastRow",
            columns = """
                <cols>
                  <col min="1" max="1" width="14" customWidth="1"/>
                  <col min="2" max="2" width="13" customWidth="1"/>
                  <col min="3" max="3" width="23" customWidth="1"/>
                  <col min="4" max="4" width="42" customWidth="1"/>
                  <col min="5" max="5" width="18" customWidth="1"/>
                </cols>
            """.trimIndent(),
            rows = rows,
            mergeCells = listOf("A1:E1", "A2:E2"),
            freezePane = "<pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/>",
            autoFilter = "A4:E$lastRow",
        )
    }

    private fun worksheetXml(
        dimension: String,
        columns: String,
        rows: String,
        mergeCells: List<String>,
        freezePane: String?,
        autoFilter: String?,
    ) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <dimension ref="$dimension"/>
          <sheetViews><sheetView workbookViewId="0">${freezePane.orEmpty()}</sheetView></sheetViews>
          <sheetFormatPr defaultRowHeight="18"/>
          $columns
          <sheetData>$rows</sheetData>
          ${if (autoFilter != null) "<autoFilter ref=\"$autoFilter\"/>" else ""}
          <mergeCells count="${mergeCells.size}">${mergeCells.joinToString("") { "<mergeCell ref=\"$it\"/>" }}</mergeCells>
          <pageMargins left="0.35" right="0.35" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
        </worksheet>
    """.trimIndent().trimStart()

    private fun row(number: Int, vararg cells: String) =
        "<row r=\"$number\">${cells.joinToString("")}</row>"

    private fun textCell(reference: String, value: String, style: Int = 0) =
        "<c r=\"$reference\" s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${value.xmlEscape()}</t></is></c>"

    private fun numberCell(reference: String, value: Double, style: Int) =
        "<c r=\"$reference\" s=\"$style\"><v>$value</v></c>"

    private fun formulaCell(reference: String, formula: String, value: Double, style: Int) =
        "<c r=\"$reference\" s=\"$style\"><f>${formula.xmlEscape()}</f><v>$value</v></c>"

    private fun LocalDate.toExcelSerial(): Double = toEpochDay() + 25_569.0

    private fun String.xmlEscape() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun ZipOutputStream.writeEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>"""
    private const val rootRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>"""
    private const val appProperties = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Mony</Application><AppVersion>1.0</AppVersion></Properties>"""
    private fun coreProperties(date: LocalDate) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>Historial financiero</dc:title><dc:creator>Mony</dc:creator><dcterms:created xsi:type="dcterms:W3CDTF">${date}T00:00:00Z</dcterms:created></cp:coreProperties>"""
    private const val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><bookViews><workbookView/></bookViews><sheets><sheet name="Resumen" sheetId="1" r:id="rId1"/><sheet name="Movimientos" sheetId="2" r:id="rId2"/></sheets><calcPr calcId="191029" fullCalcOnLoad="1"/></workbook>"""
    private const val workbookRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    private const val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="&quot;RD$&quot; #,##0.00;[Red]-&quot;RD$&quot; #,##0.00"/><numFmt numFmtId="165" formatCode="dd/mm/yyyy"/></numFmts><fonts count="6"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="20"/><color rgb="FF1F2937"/><name val="Calibri"/></font><font><b/><sz val="12"/><color rgb="FF1F2937"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font><font><sz val="11"/><color rgb="FF057A55"/><name val="Calibri"/></font><font><sz val="11"/><color rgb="FFB91C1C"/><name val="Calibri"/></font></fonts><fills count="4"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF334155"/><bgColor indexed="64"/></patternFill></fill><fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="12"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="0" fontId="2" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="0" fontId="3" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="center"/></xf><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="164" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/><xf numFmtId="164" fontId="5" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/><xf numFmtId="164" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"><alignment wrapText="1"/></xf><xf numFmtId="1" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
}
