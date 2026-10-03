package com.angel.mony.core

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.core.time.toJavaLocalDate
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryPdfMeta(
    val periodLabel: String,
    val filterLines: List<String>,
    val sortLabel: String,
    val generatedAt: LocalDate = LocalDate.now(),
)

/** Genera un informe A4 compacto, legible y listo para imprimir o compartir. */
object HistoryPdfWriter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val FOOTER_TOP = PAGE_HEIGHT - 34f
    private const val LINE_SPACING = 1.25f

    private val textColor = Color.rgb(31, 41, 55)
    private val secondaryColor = Color.rgb(100, 116, 139)
    private val primaryColor = Color.rgb(15, 118, 110)
    private val incomeColor = Color.rgb(5, 122, 85)
    private val expenseColor = Color.rgb(185, 28, 28)
    private val dividerColor = Color.rgb(226, 232, 240)
    private val summaryBackground = Color.rgb(248, 250, 252)
    private val tableHeaderBackground = Color.rgb(241, 245, 249)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun writeTo(
        stream: OutputStream,
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
        meta: HistoryPdfMeta,
    ) {
        val document = PdfDocument()
        try {
            val context = LayoutContext(document, meta)
            context.drawReportHeader(transactions)
            context.drawColumnHeader()
            transactions.forEach { transaction ->
                context.drawMovement(transaction, categories[transaction.categoryId])
            }
            context.finish()
            document.writeTo(stream)
        } finally {
            document.close()
        }
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = textColor) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
        }

    private class LayoutContext(
        private val document: PdfDocument,
        private val meta: HistoryPdfMeta,
    ) {
        private val brandPaint = textPaint(9f, bold = true, color = primaryColor).apply { letterSpacing = 0.12f }
        private val titlePaint = textPaint(22f, bold = true)
        private val subtitlePaint = textPaint(10.5f, color = secondaryColor)
        private val sectionPaint = textPaint(9f, bold = true, color = secondaryColor)
        private val categoryPaint = textPaint(11.5f, bold = true)
        private val descriptionPaint = textPaint(9.5f, color = secondaryColor)
        private val datePaint = textPaint(9.5f, color = secondaryColor)
        private val amountPaint = textPaint(11.5f, bold = true)
        private val summaryLabelPaint = textPaint(8.5f, bold = true, color = secondaryColor)
        private val footerPaint = textPaint(8.5f, color = secondaryColor)
        private val dividerStroke = Paint().apply { color = dividerColor; strokeWidth = 0.8f }
        private val primaryStroke = Paint().apply { color = primaryColor; strokeWidth = 3f }
        private val summaryFill = Paint().apply { color = summaryBackground }
        private val headerFill = Paint().apply { color = tableHeaderBackground }

        private var pageNumber = 1
        private var page: PdfDocument.Page = document.startPage(pageInfo(pageNumber))
        private var y = MARGIN

        private fun pageInfo(number: Int) =
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

        fun drawReportHeader(transactions: List<FinanceTransaction>) {
            val canvas = page.canvas
            canvas.drawLine(MARGIN, y, MARGIN + 42f, y, primaryStroke)
            y += 17f
            canvas.drawText("MONY", MARGIN, y, brandPaint)
            y += 29f
            canvas.drawText("Historial financiero", MARGIN, y, titlePaint)
            y += 18f
            canvas.drawText(meta.periodLabel.pdfSafe(), MARGIN, y, subtitlePaint)
            val generated = "Generado: ${meta.generatedAt.format(dateFormatter)}"
            canvas.drawText(generated, PAGE_WIDTH - MARGIN - subtitlePaint.measureText(generated), y, subtitlePaint)
            y += 16f

            val filters = meta.filterLines.joinToString(" | ").pdfSafe()
            wrapText(filters, subtitlePaint, CONTENT_WIDTH).take(2).forEach { line ->
                canvas.drawText(line, MARGIN, y, subtitlePaint)
                y += 13f
            }
            y += 8f

            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf(FinanceTransaction::amountInCents)
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf(FinanceTransaction::amountInCents)
            val balance = income - expense
            val top = y
            val height = 76f
            canvas.drawRoundRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + height, 10f, 10f, summaryFill)
            val columnWidth = CONTENT_WIDTH / 3f
            summaryColumn("INGRESOS", MoneyFormatter.format(income), MARGIN + 14f, top, incomeColor)
            summaryColumn("GASTOS", MoneyFormatter.format(expense), MARGIN + columnWidth + 14f, top, expenseColor)
            summaryColumn("BALANCE", MoneyFormatter.format(balance), MARGIN + columnWidth * 2 + 14f, top, if (balance < 0) expenseColor else textColor)
            canvas.drawLine(MARGIN + columnWidth, top + 14f, MARGIN + columnWidth, top + height - 14f, dividerStroke)
            canvas.drawLine(MARGIN + columnWidth * 2, top + 14f, MARGIN + columnWidth * 2, top + height - 14f, dividerStroke)
            y = top + height + 17f
            canvas.drawText(
                "${transactions.size} ${if (transactions.size == 1) "MOVIMIENTO" else "MOVIMIENTOS"} | ORDEN: ${meta.sortLabel.uppercase(Locale.forLanguageTag("es-DO")).pdfSafe()}",
                MARGIN,
                y,
                sectionPaint,
            )
            y += 13f
        }

        private fun summaryColumn(label: String, value: String, x: Float, top: Float, color: Int) {
            page.canvas.drawText(label, x, top + 26f, summaryLabelPaint)
            page.canvas.drawText(value, x, top + 52f, textPaint(13f, bold = true, color = color))
        }

        fun drawColumnHeader() {
            val top = y
            page.canvas.drawRoundRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + 28f, 6f, 6f, headerFill)
            page.canvas.drawText("FECHA", MARGIN + 10f, top + 18f, sectionPaint)
            page.canvas.drawText("CATEGORÍA Y DESCRIPCIÓN", MARGIN + 86f, top + 18f, sectionPaint)
            val label = "MONTO"
            page.canvas.drawText(label, PAGE_WIDTH - MARGIN - 10f - sectionPaint.measureText(label), top + 18f, sectionPaint)
            y = top + 35f
        }

        fun drawMovement(transaction: FinanceTransaction, category: Category?) {
            val descriptionLines = transaction.description
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { wrapText(it.pdfSafe(), descriptionPaint, 270f) }
                .orEmpty()
            val rowHeight = 37f + descriptionLines.size * descriptionPaint.textSize * LINE_SPACING
            ensureSpace(rowHeight)

            val canvas = page.canvas
            val startY = y
            canvas.drawText(dateFormatter.format(transaction.date.toJavaLocalDate()), MARGIN + 10f, y + 14f, datePaint)

            val isExpense = transaction.type == TransactionType.EXPENSE
            val amount = "${if (isExpense) "-" else "+"}${MoneyFormatter.format(transaction.amountInCents)}"
            val coloredAmount = textPaint(amountPaint.textSize, bold = true, color = if (isExpense) expenseColor else incomeColor)
            val amountX = PAGE_WIDTH - MARGIN - 10f - coloredAmount.measureText(amount)
            val categoryX = MARGIN + 86f
            drawClipped(
                (category?.name ?: "Sin categoría").uppercase(Locale.forLanguageTag("es-DO")).pdfSafe(),
                categoryPaint,
                categoryX,
                y + 14f,
                amountX - categoryX - 14f,
            )
            canvas.drawText(amount, amountX, y + 14f, coloredAmount)

            var descriptionY = y + 29f
            descriptionLines.forEach { line ->
                canvas.drawText(line, categoryX, descriptionY, descriptionPaint)
                descriptionY += descriptionPaint.textSize * LINE_SPACING
            }
            y = startY + rowHeight
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerStroke)
            y += 4f
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight <= FOOTER_TOP) return
            closeCurrentPage()
            pageNumber++
            page = document.startPage(pageInfo(pageNumber))
            y = MARGIN
            drawContinuationHeader()
            drawColumnHeader()
        }

        private fun drawContinuationHeader() {
            page.canvas.drawText("MONY", MARGIN, y + 8f, brandPaint)
            val title = "Historial financiero | ${meta.periodLabel.pdfSafe()}"
            drawClipped(title, subtitlePaint, MARGIN + 54f, y + 8f, CONTENT_WIDTH - 54f)
            y += 22f
            page.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, primaryStroke)
            y += 10f
        }

        fun finish() = closeCurrentPage()

        private fun closeCurrentPage() {
            val left = "Mony | Historial financiero"
            val right = "Página $pageNumber"
            page.canvas.drawLine(MARGIN, FOOTER_TOP, PAGE_WIDTH - MARGIN, FOOTER_TOP, dividerStroke)
            page.canvas.drawText(left, MARGIN, PAGE_HEIGHT - 17f, footerPaint)
            page.canvas.drawText(right, PAGE_WIDTH - MARGIN - footerPaint.measureText(right), PAGE_HEIGHT - 17f, footerPaint)
            document.finishPage(page)
        }

        private fun drawClipped(text: String, paint: Paint, x: Float, baseline: Float, maxWidth: Float) {
            if (maxWidth <= 0f) return
            if (paint.measureText(text) <= maxWidth) {
                page.canvas.drawText(text, x, baseline, paint)
                return
            }
            var end = text.length
            while (end > 1 && paint.measureText(text, 0, end) + paint.measureText("...") > maxWidth) end--
            page.canvas.drawText(text.take(end.coerceAtLeast(1)).trimEnd() + "...", x, baseline, paint)
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return emptyList()
            val lines = mutableListOf<String>()
            text.lines().forEach { paragraph ->
                var current = ""
                paragraph.split(Regex("\\s+")).filter(String::isNotBlank).forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                        current = candidate
                    } else {
                        lines += current
                        current = word
                    }
                }
                if (current.isNotEmpty()) lines += current
            }
            return lines
        }
    }

    private fun String.pdfSafe() = replace('–', '-').replace('—', '-').replace('−', '-')
}
