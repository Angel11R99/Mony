package com.example.personalfinancetracker.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import java.io.OutputStream
import java.time.format.DateTimeFormatter

data class HistoryPdfMeta(
    val periodLabel: String,
    val filterLines: List<String>,
    val sortLabel: String,
)

/**
 * Genera un PDF del historial financiero pensado para lectura vertical en teléfonos:
 * bloques tipo tarjeta en lugar de tablas anchas, tipografía grande y montos destacados.
 */
object HistoryPdfWriter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val FOOTER_RESERVED = 30f
    private const val LINE_SPACING = 1.32f
    private const val BLOCK_ESTIMATE = 64f

    private val textColor = Color.rgb(31, 41, 55)
    private val secondaryColor = Color.rgb(107, 114, 128)
    private val incomeColor = Color.rgb(5, 122, 85)
    private val expenseColor = Color.rgb(185, 28, 28)
    private val dividerColor = Color.rgb(229, 231, 235)
    private val summaryBackground = Color.rgb(243, 244, 246)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun writeTo(
        stream: OutputStream,
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
        meta: HistoryPdfMeta,
    ) {
        val document = PdfDocument()
        try {
            val context = LayoutContext(document)
            context.drawHeader(transactions, meta)
            transactions.forEachIndexed { index, transaction ->
                context.ensureSpace(BLOCK_ESTIMATE)
                if (index > 0) context.drawDivider()
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

    private class LayoutContext(private val document: PdfDocument) {
        private val titlePaint = textPaint(21f, bold = true)
        private val subtitlePaint = textPaint(11.5f)
        private val sectionPaint = textPaint(10f, bold = true, color = secondaryColor)
        private val categoryPaint = textPaint(13f, bold = true)
        private val descriptionPaint = textPaint(11f, color = secondaryColor)
        private val datePaint = textPaint(10.5f, color = secondaryColor)
        private val amountPaint = textPaint(14f, bold = true)
        private val summaryLabelPaint = textPaint(9.5f, color = secondaryColor)
        private val summaryValuePaint = textPaint(13f, bold = true)
        private val footerPaint = textPaint(9f, color = secondaryColor)
        private val dividerStroke = Paint().apply {
            color = dividerColor
            strokeWidth = 0.8f
        }
        private val backgroundFill = Paint().apply { color = summaryBackground }

        private var page: PdfDocument.Page = document.startPage(pageInfo(1))
        private var pageNumber = 1
        private var y = MARGIN + 4f

        private fun pageInfo(number: Int) =
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight <= PAGE_HEIGHT - MARGIN - FOOTER_RESERVED) return
            drawFooter()
            document.finishPage(page)
            pageNumber++
            page = document.startPage(pageInfo(pageNumber))
            y = MARGIN
        }

        fun drawDivider() {
            page.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerStroke)
            y += 14f
        }

        fun drawMovement(transaction: FinanceTransaction, category: Category?) {
            val isExpense = transaction.type == TransactionType.EXPENSE
            val sign = if (isExpense) "−" else "+"
            val amount = "$sign${MoneyFormatter.format(transaction.amountInCents)}"
            val coloredAmount = textPaint(amountPaint.textSize, bold = true, color = if (isExpense) expenseColor else incomeColor)

            val categoryName = (category?.name ?: "Sin categoría").uppercase(java.util.Locale.forLanguageTag("es-DO"))
            val amountWidth = amountPaint.measureText(amount)
            val maxCategoryWidth = CONTENT_WIDTH - amountWidth - 16f
            val baseline = y + categoryPaint.textSize
            drawClipped(categoryName, categoryPaint, MARGIN, baseline, maxCategoryWidth)
            page.canvas.drawText(amount, PAGE_WIDTH - MARGIN - amountWidth, baseline, coloredAmount)
            y += categoryPaint.textSize * LINE_SPACING

            val dateBaseline = y + datePaint.textSize
            page.canvas.drawText(dateFormatter.format(transaction.date), MARGIN + 2f, dateBaseline, datePaint)
            y += datePaint.textSize * LINE_SPACING

            transaction.description?.takeIf { it.isNotBlank() }?.let { note ->
                wrapText(note, descriptionPaint, CONTENT_WIDTH - 8f).forEach { line ->
                    ensureSpace(descriptionPaint.textSize * LINE_SPACING)
                    page.canvas.drawText(line, MARGIN + 2f, y + descriptionPaint.textSize, descriptionPaint)
                    y += descriptionPaint.textSize * LINE_SPACING
                }
            }
            y += 12f
        }

        fun drawHeader(transactions: List<FinanceTransaction>, meta: HistoryPdfMeta) {
            val canvas = page.canvas
            y += titlePaint.textSize
            canvas.drawText("Historial financiero", MARGIN, y, titlePaint)
            y += titlePaint.textSize * 0.7f
            canvas.drawText(meta.periodLabel, MARGIN, y, subtitlePaint)
            y += subtitlePaint.textSize * LINE_SPACING
            meta.filterLines.forEach { line ->
                canvas.drawText("· $line", MARGIN + 2f, y, subtitlePaint)
                y += subtitlePaint.textSize * LINE_SPACING
            }
            y += 8f

            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf(FinanceTransaction::amountInCents)
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf(FinanceTransaction::amountInCents)
            val balance = income - expense
            val boxTop = y
            val boxHeight = 122f
            canvas.drawRoundRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxTop + boxHeight, 10f, 10f, backgroundFill)
            val innerLeft = MARGIN + 16f
            val innerRight = PAGE_WIDTH - MARGIN - 16f
            var rowY = boxTop + 26f
            canvas.drawText(
                "RESUMEN · ${transactions.size} ${if (transactions.size == 1) "MOVIMIENTO" else "MOVIMIENTOS"}",
                innerLeft,
                rowY,
                sectionPaint,
            )
            rowY += 24f
            rowY = summaryRow("INGRESOS", MoneyFormatter.format(income), innerLeft, innerRight, rowY, incomeColor)
            rowY = summaryRow("GASTOS", MoneyFormatter.format(expense), innerLeft, innerRight, rowY, expenseColor)
            summaryRow("BALANCE", MoneyFormatter.format(balance), innerLeft, innerRight, rowY, if (balance < 0) expenseColor else textColor)
            y = boxTop + boxHeight + 28f
            canvas.drawText("ORDEN: ${meta.sortLabel.uppercase()}", MARGIN, y, sectionPaint)
            y += 20f
        }

        private fun summaryRow(label: String, value: String, left: Float, right: Float, rowY: Float, valueColor: Int): Float {
            val canvas = page.canvas
            canvas.drawText(label, left, rowY, summaryLabelPaint)
            val valueColored = textPaint(summaryValuePaint.textSize, bold = true, color = valueColor)
            canvas.drawText(value, right - valueColored.measureText(value), rowY + 1f, valueColored)
            return rowY + 26f
        }

        fun finish() {
            drawFooter()
            document.finishPage(page)
        }

        private fun drawFooter() {
            val footer = "Página $pageNumber"
            val width = footerPaint.measureText(footer)
            page.canvas.drawText(footer, (PAGE_WIDTH - width) / 2f, PAGE_HEIGHT - 18f, footerPaint)
        }

        private fun drawClipped(text: String, paint: Paint, x: Float, baseline: Float, maxWidth: Float) {
            if (paint.measureText(text) <= maxWidth) {
                page.canvas.drawText(text, x, baseline, paint)
                return
            }
            var end = text.length
            while (end > 1 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
            page.canvas.drawText(text.take(end.coerceAtLeast(1)).trimEnd() + "…", x, baseline, paint)
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val lines = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                var current = StringBuilder()
                paragraph.split(' ').forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                        current = StringBuilder(candidate)
                    } else {
                        lines.add(current.toString())
                        current = StringBuilder(word)
                    }
                }
                if (current.isNotEmpty()) lines.add(current.toString())
            }
            return lines
        }
    }
}
