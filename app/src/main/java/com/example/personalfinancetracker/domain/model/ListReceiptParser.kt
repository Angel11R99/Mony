package com.example.personalfinancetracker.domain.model

data class OcrMoneyCandidate(
    val amountInCents: Long,
    val rawValue: String,
)

enum class TicketAmountKind {
    SUBTOTAL,
    TAX,
    DISCOUNT,
    SHIPPING,
    SERVICE,
    TOTAL,
    PRODUCTO,
    MONTO_DETECTADO,
}

data class TicketAmountCandidate(
    val amountInCents: Long,
    val kind: TicketAmountKind,
    val sourceLine: String,
    val lineIndex: Int,
    val productName: String? = null,
)

data class TicketParseResult(val candidates: List<TicketAmountCandidate>)

/** A line recognized by OCR together with its position in the scanned document. */
data class TicketOcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

object ListOcrMoneyParser {
    private val moneyPattern = Regex(
        "(?<![\\d.,])(?:RD\\$|\\$)?\\s*(?:\\d{1,3}(?:,\\d{3})+\\.\\d{2}|" +
            "\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+[.,]\\d{2})(?![\\d.,])",
        RegexOption.IGNORE_CASE,
    )

    fun extractCandidates(text: String): List<OcrMoneyCandidate> {
        val seenAmounts = mutableSetOf<Long>()
        return moneyPattern.findAll(text).mapNotNull { match ->
            val raw = match.value.trim()
            val cents = parseAmount(raw) ?: return@mapNotNull null
            if (!seenAmounts.add(cents)) return@mapNotNull null
            OcrMoneyCandidate(cents, raw)
        }.toList()
    }

    private fun parseAmount(raw: String): Long? {
        val numeric = raw.replace(Regex("(?i)RD\\$|\\$|\\s"), "")
        val decimalSeparator = when {
            numeric.contains('.') && numeric.contains(',') ->
                if (numeric.lastIndexOf('.') > numeric.lastIndexOf(',')) '.' else ','
            numeric.contains('.') -> '.'
            numeric.contains(',') -> ','
            else -> return null
        }
        val separatorIndex = numeric.lastIndexOf(decimalSeparator)
        if (numeric.length - separatorIndex - 1 != 2) return null
        val whole = numeric.substring(0, separatorIndex).replace(".", "").replace(",", "")
        val decimals = numeric.substring(separatorIndex + 1)
        if (whole.isEmpty() || !whole.all(Char::isDigit) || !decimals.all(Char::isDigit)) return null
        return runCatching {
            Math.addExact(Math.multiplyExact(whole.toLong(), 100L), decimals.toLong())
        }.getOrNull()
    }
}

object ListTicketParser {
    fun parse(text: String): TicketParseResult {
        val labeledCandidates = mutableListOf<TicketAmountCandidate>()
        var pendingLabel: TicketAmountKind? = null
        var pendingLine = ""
        var pendingLineIndex = -1
        val pendingProductLines = mutableListOf<String>()

        for ((index, rawLine) in text.lineSequence().withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val amounts = ListOcrMoneyParser.extractCandidates(line)
            val label = classifyLabelOnly(line)
            val isLabeledLine = label != null && amounts.isNotEmpty()
            val isLabelOnlyLine = label != null && amounts.isEmpty()
            val isAmountOnlyLine = label == null && amounts.isNotEmpty()

            if (isLabeledLine) {
                pendingLabel = null
                pendingProductLines.clear()
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, label, line, index)
                }
            } else if (isLabelOnlyLine) {
                pendingLabel = label
                pendingLine = line
                pendingLineIndex = index
                pendingProductLines.clear()
            } else if (isAmountOnlyLine && pendingLabel != null) {
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, pendingLabel, pendingLine, pendingLineIndex)
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, TicketAmountKind.MONTO_DETECTADO, line, index)
                }
                pendingLabel = null
                pendingProductLines.clear()
            } else if (isAmountOnlyLine) {
                val productName = extractProductName(line) ?: pendingProductLines.joinToString(" ").ifBlank { null }
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, TicketAmountKind.PRODUCTO, line, index, productName = productName)
                }
                pendingProductLines.clear()
            } else {
                pendingLabel = null
                line.takeIf(::isProductDescription)?.let(pendingProductLines::add)
            }
        }

        return TicketParseResult(labeledCandidates)
    }

    /**
     * Uses OCR geometry to connect a price with the closest description on the same visual row.
     * This avoids depending on the order in which OCR returns separate columns of a ticket.
     */
    fun parse(text: String, ocrLines: List<TicketOcrLine>): TicketParseResult {
        val parsed = parse(text)
        if (ocrLines.isEmpty()) return parsed

        return TicketParseResult(parsed.candidates.map { candidate ->
            if (candidate.kind != TicketAmountKind.PRODUCTO) candidate
            else candidate.copy(
                productName = findProductNameByPosition(candidate, ocrLines) ?: candidate.productName,
            )
        })
    }

    private fun extractProductName(line: String): String? {
        var result = line
        for (candidate in ListOcrMoneyParser.extractCandidates(line)) {
            result = result.replace(candidate.rawValue, "")
        }
        result = result.replace(Regex("(?i)RD\\$|\\$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return result.ifBlank { null }
    }

    private fun isProductDescription(line: String): Boolean =
        line.any(Char::isLetter) &&
            !Regex("(?i)^\\s*(descripcion|descripción|producto|product|cantidad|cant\\.?|precio|price)\\s*$")
                .matches(line)

    private fun findProductNameByPosition(
        candidate: TicketAmountCandidate,
        ocrLines: List<TicketOcrLine>,
    ): String? {
        val priceLines = ocrLines.filter { line ->
            ListOcrMoneyParser.extractCandidates(line.text).any { it.amountInCents == candidate.amountInCents }
        }
        val matchingSourceLines = priceLines.filter { it.text.trim() == candidate.sourceLine.trim() }
        val priceLine = (matchingSourceLines.ifEmpty { priceLines }).firstOrNull() ?: return null

        val descriptionLines = ocrLines.filter {
            it != priceLine &&
                ListOcrMoneyParser.extractCandidates(it.text).isEmpty() &&
                isProductDescription(it.text.trim())
        }
        val anchor = descriptionLines
            .filter { isOnSameVisualRow(priceLine, it) }
            .minByOrNull { distanceBetween(priceLine, it) }
            ?: descriptionLines
                .filter { it.bottom <= priceLine.top && isCloseAbove(priceLine, it) }
                .minByOrNull { priceLine.top - it.bottom }
            ?: return null

        val parts = mutableListOf(anchor.text.trim())
        var current = anchor
        while (true) {
            val previous = descriptionLines
                .filter { it.bottom <= current.top }
                .filter { isCloseAbove(current, it) && isInSameDescriptionColumn(current, it) }
                .minByOrNull { current.top - it.bottom }
                ?: break
            parts += previous.text.trim()
            current = previous
        }
        return parts.asReversed().joinToString(" ")
    }

    private fun isOnSameVisualRow(first: TicketOcrLine, second: TicketOcrLine): Boolean {
        val firstCenter = (first.top + first.bottom) / 2.0
        val secondCenter = (second.top + second.bottom) / 2.0
        val largestHeight = maxOf(first.bottom - first.top, second.bottom - second.top).coerceAtLeast(1)
        return kotlin.math.abs(firstCenter - secondCenter) <= largestHeight * 0.8
    }

    private fun distanceBetween(first: TicketOcrLine, second: TicketOcrLine): Double {
        val horizontalGap = when {
            first.right < second.left -> second.left - first.right
            second.right < first.left -> first.left - second.right
            else -> 0
        }
        val firstCenter = (first.top + first.bottom) / 2.0
        val secondCenter = (second.top + second.bottom) / 2.0
        return horizontalGap + kotlin.math.abs(firstCenter - secondCenter) * 2
    }

    private fun isCloseAbove(lower: TicketOcrLine, upper: TicketOcrLine): Boolean {
        val largestHeight = maxOf(lower.bottom - lower.top, upper.bottom - upper.top).coerceAtLeast(1)
        return lower.top - upper.bottom <= largestHeight * 4
    }

    private fun isInSameDescriptionColumn(first: TicketOcrLine, second: TicketOcrLine): Boolean =
        first.left <= second.right && second.left <= first.right ||
            kotlin.math.abs(first.left - second.left) <= maxOf(first.right - first.left, second.right - second.left) / 2

    private fun classifyLabelOnly(line: String): TicketAmountKind? {
        val normalized = normalizeProductName(line)
        return when {
            Regex("\\bsubtotal\\b").containsMatchIn(normalized) -> TicketAmountKind.SUBTOTAL
            Regex("\\b(itbis|impuesto|tax)\\b").containsMatchIn(normalized) -> TicketAmountKind.TAX
            Regex("\\b(descuento|discount|rebaja)\\b").containsMatchIn(normalized) -> TicketAmountKind.DISCOUNT
            Regex("\\b(envio|delivery|shipping)\\b").containsMatchIn(normalized) -> TicketAmountKind.SHIPPING
            Regex("\\b(servicio|propina|service|tip)\\b").containsMatchIn(normalized) -> TicketAmountKind.SERVICE
            Regex("\\b(total|importe a pagar|monto a pagar)\\b").containsMatchIn(normalized) -> TicketAmountKind.TOTAL
            else -> null
        }
    }
}
