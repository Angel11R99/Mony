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
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, label, line, index)
                }
            } else if (isLabelOnlyLine) {
                pendingLabel = label
                pendingLine = line
                pendingLineIndex = index
            } else if (isAmountOnlyLine && pendingLabel != null) {
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, pendingLabel, pendingLine, pendingLineIndex)
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, TicketAmountKind.MONTO_DETECTADO, line, index)
                }
                pendingLabel = null
            } else if (isAmountOnlyLine) {
                val productName = extractProductName(line)
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, TicketAmountKind.PRODUCTO, line, index, productName = productName)
                }
            } else {
                pendingLabel = null
                for (amount in amounts) {
                    labeledCandidates += TicketAmountCandidate(amount.amountInCents, TicketAmountKind.MONTO_DETECTADO, line, index)
                }
            }
        }

        return TicketParseResult(labeledCandidates)
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
