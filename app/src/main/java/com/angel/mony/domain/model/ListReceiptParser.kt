package com.angel.mony.domain.model

import kotlin.math.abs

data class OcrMoneyCandidate(
    val amountInCents: Long,
    val rawValue: String,
    val occurrenceIndex: Int = 0,
)

enum class TicketAmountKind { SUBTOTAL, TAX, DISCOUNT, SHIPPING, SERVICE, TOTAL, PRODUCTO, MONTO_DETECTADO }
enum class RecognitionConfidence { HIGH, MEDIUM, LOW }

data class TicketAmountCandidate(
    val amountInCents: Long,
    val kind: TicketAmountKind,
    val sourceLine: String,
    val lineIndex: Int,
    val productName: String? = null,
    val quantity: Int = 1,
    val occurrenceId: String = "$lineIndex:0",
    val confidence: RecognitionConfidence = RecognitionConfidence.LOW,
)

data class TicketParseResult(val candidates: List<TicketAmountCandidate>)

data class TicketOcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val blockIndex: Int = 0,
    val lineIndex: Int = 0,
)

object ListOcrMoneyParser {
    private val moneyPattern = Regex(
        "(?<![\\d.,])(?:RD\\$|\\$)?\\s*(?:\\d{1,3}(?:,\\d{3})+\\.\\d{2}|" +
            "\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+[.,]\\d{2})(?![\\d.,])",
        RegexOption.IGNORE_CASE,
    )

    fun extractOccurrences(text: String): List<OcrMoneyCandidate> = moneyPattern.findAll(text)
        .mapIndexedNotNull { index, match ->
            val raw = match.value.trim()
            parseAmount(raw)?.let { OcrMoneyCandidate(it, raw, index) }
        }.toList()

    fun extractCandidates(text: String): List<OcrMoneyCandidate> {
        val seen = mutableSetOf<Long>()
        return extractOccurrences(text).filter { seen.add(it.amountInCents) }
    }

    private fun parseAmount(raw: String): Long? {
        val numeric = raw.replace(Regex("(?i)RD\\$|\\$|\\s"), "")
        val decimalSeparator = when {
            numeric.contains('.') && numeric.contains(',') -> if (numeric.lastIndexOf('.') > numeric.lastIndexOf(',')) '.' else ','
            numeric.contains('.') -> '.'
            numeric.contains(',') -> ','
            else -> return null
        }
        val separatorIndex = numeric.lastIndexOf(decimalSeparator)
        if (numeric.length - separatorIndex - 1 != 2) return null
        val whole = numeric.substring(0, separatorIndex).replace(".", "").replace(",", "")
        val decimals = numeric.substring(separatorIndex + 1)
        if (whole.isEmpty() || !whole.all(Char::isDigit) || !decimals.all(Char::isDigit)) return null
        return runCatching { Math.addExact(Math.multiplyExact(whole.toLong(), 100L), decimals.toLong()) }.getOrNull()
    }
}

object ListTicketParser {
    private const val HIGH_SCORE = 85.0
    private const val MEDIUM_SCORE = 55.0

    fun parse(text: String): TicketParseResult {
        val candidates = mutableListOf<TicketAmountCandidate>()
        var pendingLabel: Pair<TicketAmountKind, Pair<String, Int>>? = null
        val pendingProductLines = mutableListOf<String>()
        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            val amounts = ListOcrMoneyParser.extractOccurrences(line)
            val label = classifyLabelOnly(line)
            when {
                label != null && amounts.isNotEmpty() -> {
                    amounts.forEach { candidates += ticketCandidate(it, label, line, index) }
                    pendingLabel = null
                    pendingProductLines.clear()
                }
                label != null -> {
                    pendingLabel = label to (line to index)
                    pendingProductLines.clear()
                }
                amounts.isNotEmpty() && pendingLabel != null -> {
                    val (kind, source) = checkNotNull(pendingLabel)
                    amounts.forEach {
                        candidates += ticketCandidate(it, kind, source.first, source.second)
                        candidates += ticketCandidate(it, TicketAmountKind.MONTO_DETECTADO, line, index)
                    }
                    pendingLabel = null
                    pendingProductLines.clear()
                }
                amounts.isNotEmpty() -> {
                    val inlineName = extractProductName(line)
                    val name = inlineName ?: pendingProductLines.joinToString(" ").ifBlank { null }
                    val quantity = extractQuantity(line)
                    selectProductAmounts(amounts, quantity).forEach {
                        candidates += ticketCandidate(it, TicketAmountKind.PRODUCTO, line, index, name, quantity)
                    }
                    pendingProductLines.clear()
                }
                else -> {
                    pendingLabel = null
                    line.takeIf(::isProductDescription)?.let(pendingProductLines::add)
                }
            }
        }
        return TicketParseResult(candidates)
    }

    fun parse(text: String, ocrLines: List<TicketOcrLine>): TicketParseResult {
        if (ocrLines.isEmpty()) return parse(text)
        val descriptions = ocrLines.filter {
            ListOcrMoneyParser.extractOccurrences(it.text).isEmpty() && isProductDescription(it.text)
        }
        val candidates = buildList {
            ocrLines.forEach { priceLine ->
                val amounts = ListOcrMoneyParser.extractOccurrences(priceLine.text)
                if (amounts.isEmpty()) return@forEach
                val kind = classifyLabelOnly(priceLine.text) ?: findNearbyLabel(priceLine, ocrLines)
                val productAmounts = if (kind == null) selectProductAmounts(amounts, extractQuantity(priceLine.text)) else amounts
                productAmounts.forEach { amount ->
                    val occurrenceId = "${priceLine.blockIndex}:${priceLine.lineIndex}:${amount.occurrenceIndex}"
                    if (kind != null) {
                        add(ticketCandidate(amount, kind, priceLine.text, priceLine.lineIndex).copy(occurrenceId = occurrenceId))
                    } else {
                        val inline = extractProductName(priceLine.text)
                        val scored = if (inline != null) ScoredName(inline, 120.0, extractQuantity(priceLine.text)) else findBestName(priceLine, descriptions)
                        add(
                            ticketCandidate(
                                amount, TicketAmountKind.PRODUCTO, priceLine.text, priceLine.lineIndex,
                                scored?.name, scored?.quantity ?: extractQuantity(priceLine.text),
                            ).copy(
                                occurrenceId = occurrenceId,
                                confidence = confidenceFor(scored?.score ?: 0.0),
                            )
                        )
                    }
                }
            }
        }
        return TicketParseResult(collapseSeparateSubtotals(candidates, ocrLines))
    }

    private fun ticketCandidate(
        amount: OcrMoneyCandidate,
        kind: TicketAmountKind,
        source: String,
        lineIndex: Int,
        productName: String? = null,
        quantity: Int = 1,
    ) = TicketAmountCandidate(
        amount.amountInCents, kind, source, lineIndex, productName, quantity,
        occurrenceId = "$lineIndex:${amount.occurrenceIndex}",
        confidence = if (productName == null) RecognitionConfidence.LOW else RecognitionConfidence.MEDIUM,
    )

    private data class ScoredName(val name: String, val score: Double, val quantity: Int)

    private fun findBestName(price: TicketOcrLine, descriptions: List<TicketOcrLine>): ScoredName? {
        val ranked = descriptions.asSequence().filter { it !== price }.mapNotNull { line ->
            val height = maxOf(price.bottom - price.top, line.bottom - line.top).coerceAtLeast(1)
            val verticalGap = when {
                line.bottom < price.top -> price.top - line.bottom
                line.top > price.bottom -> line.top - price.bottom
                else -> 0
            }
            if (verticalGap > height * 6) return@mapNotNull null
            val rowOverlap = overlap(price.top, price.bottom, line.top, line.bottom)
            val horizontalOverlap = overlap(price.left, price.right, line.left, line.right)
            val sameRow = rowOverlap > 0 || abs(centerY(price) - centerY(line)) <= height * 0.8
            var score = when {
                sameRow -> 100.0
                line.bottom <= price.top -> 72.0
                else -> 52.0
            }
            score -= verticalGap.toDouble() / height * 7.0
            if (horizontalOverlap > 0) score += 10.0
            if (price.blockIndex == line.blockIndex) score += 12.0
            if (line.text.length in 4..60) score += 6.0 else score -= 10.0
            line to score
        }.sortedByDescending { it.second }.toList()
        val anchor = ranked.firstOrNull() ?: return null
        val lines = descriptions.filter { candidate ->
            candidate.blockIndex == anchor.first.blockIndex &&
                candidate.bottom <= anchor.first.top &&
                anchor.first.top - candidate.bottom <= maxOf(anchor.first.bottom - anchor.first.top, candidate.bottom - candidate.top) * 2 &&
                columnsOverlap(anchor.first, candidate)
        }.sortedBy { it.top }.takeLast(2) + anchor.first
        val distinctLines = lines.distinct()
        val quantity = distinctLines.maxOfOrNull { extractQuantity(it.text) } ?: 1
        val name = distinctLines.joinToString(" ") { cleanDescriptionText(it.text) }
            .replace(Regex("\\s+"), " ").trim()
        return ScoredName(name, anchor.second, quantity)
    }

    private fun collapseSeparateSubtotals(
        candidates: List<TicketAmountCandidate>,
        lines: List<TicketOcrLine>,
    ): List<TicketAmountCandidate> {
        val lineByOccurrence = lines.flatMap { line ->
            ListOcrMoneyParser.extractOccurrences(line.text).map { amount ->
                "${line.blockIndex}:${line.lineIndex}:${amount.occurrenceIndex}" to line
            }
        }.toMap()
        return candidates.filterNot { possibleSubtotal ->
            if (possibleSubtotal.kind != TicketAmountKind.PRODUCTO) return@filterNot false
            candidates.any { unit ->
                if (unit === possibleSubtotal || unit.kind != TicketAmountKind.PRODUCTO || unit.quantity <= 1) return@any false
                val expected = runCatching { Math.multiplyExact(unit.amountInCents, unit.quantity.toLong()) }.getOrNull()
                if (expected != possibleSubtotal.amountInCents || normalizeProductName(unit.productName.orEmpty()) != normalizeProductName(possibleSubtotal.productName.orEmpty())) return@any false
                val unitLine = lineByOccurrence[unit.occurrenceId] ?: return@any false
                val subtotalLine = lineByOccurrence[possibleSubtotal.occurrenceId] ?: return@any false
                val height = maxOf(unitLine.bottom - unitLine.top, subtotalLine.bottom - subtotalLine.top).coerceAtLeast(1)
                abs(centerY(unitLine) - centerY(subtotalLine)) <= height * 1.5
            }
        }
    }

    private fun confidenceFor(score: Double) = when {
        score >= HIGH_SCORE -> RecognitionConfidence.HIGH
        score >= MEDIUM_SCORE -> RecognitionConfidence.MEDIUM
        else -> RecognitionConfidence.LOW
    }

    private fun extractQuantity(line: String): Int = Regex("(?i)^\\s*(\\d+)\\s*(?:x|\\*|u|und\\.?|uds\\.?)\\s+")
        .find(line)?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

    private fun cleanDescriptionText(line: String): String = line
        .replace(Regex("(?i)^\\s*\\d+\\s*(?:x|\\*|u|und\\.?|uds\\.?)\\s+"), "")
        .trim()

    private fun selectProductAmounts(amounts: List<OcrMoneyCandidate>, quantity: Int): List<OcrMoneyCandidate> {
        if (quantity <= 1 || amounts.size < 2) return amounts
        return amounts.firstOrNull { unit ->
            runCatching { Math.multiplyExact(unit.amountInCents, quantity.toLong()) }.getOrNull()
                ?.let { subtotal -> amounts.any { it !== unit && it.amountInCents == subtotal } } == true
        }?.let(::listOf) ?: amounts
    }

    private fun extractProductName(line: String): String? {
        var result = line
        ListOcrMoneyParser.extractOccurrences(line).forEach { result = result.replaceFirst(it.rawValue, "") }
        result = result.replace(Regex("(?i)RD\\$|\\$"), "")
            .replace(Regex("(?i)^\\s*\\d+\\s*(?:x|\\*|u|und\\.?|uds\\.?|unid\\.?|pza\\.?)\\s+"), "")
            .replace(Regex("\\s+"), " ").trim()
        return result.takeIf(::isProductDescription)
    }

    private fun isProductDescription(value: String): Boolean {
        val line = value.trim()
        if (line.length !in 3..90 || !line.any(Char::isLetter)) return false
        if (ignoredProductText.containsMatchIn(normalizeProductName(line))) return false
        if (Regex("^[A-Z]{2,}\\d{3,}[A-Z0-9-]*$").matches(line)) return false
        if (Regex("^\\s*\\d+\\s*(?:x|\\*|und\\.?|uds\\.?|unid\\.?|pza\\.?)?\\s*$", RegexOption.IGNORE_CASE).matches(line)) return false
        if (Regex("(?i)\\b(oferta|promo|plu|codigo|barra|balanza|pesaje|calorias|ingredientes|nutricional|porcentaje)\\b").containsMatchIn(line)) return false
        if (Regex("(?i)^\\d+(?:[.,]\\d+)?\\s*(?:kg|g|gr|ml|l|oz|lb)$").matches(line)) return true
        val digits = line.count(Char::isDigit)
        return digits <= line.length / 2
    }

    private val ignoredProductText = Regex(
        "^(descripcion|producto|product|cantidad|cant|precio|price|subtotal|total|itbis|impuesto|tax|" +
            "descuento|discount|rebaja|envio|delivery|shipping|servicio|propina|service|tip|rd|" +
            "compartir|share|anadir al carrito|agregar al carrito|mi carrito|carrito|ayuda|ingresar|inicio|eliminar|remove|add|cart)$"
    )

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

    private fun findNearbyLabel(price: TicketOcrLine, lines: List<TicketOcrLine>): TicketAmountKind? = lines
        .asSequence()
        .filter { it !== price && it.bottom <= price.top }
        .mapNotNull { line -> classifyLabelOnly(line.text)?.let { kind -> line to kind } }
        .filter { (line, _) ->
            val height = maxOf(price.bottom - price.top, line.bottom - line.top).coerceAtLeast(1)
            price.top - line.bottom <= height * 3 && (line.blockIndex == price.blockIndex || columnsOverlap(price, line))
        }
        .minByOrNull { (line, _) -> price.top - line.bottom }
        ?.second

    private fun centerY(line: TicketOcrLine) = (line.top + line.bottom) / 2.0
    private fun overlap(a1: Int, a2: Int, b1: Int, b2: Int) = minOf(a2, b2) - maxOf(a1, b1)
    private fun columnsOverlap(a: TicketOcrLine, b: TicketOcrLine) = overlap(a.left, a.right, b.left, b.right) >= 0 || abs(a.left - b.left) <= maxOf(a.right - a.left, b.right - b.left) / 2
}
