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
    UNCLASSIFIED,
}

data class TicketAmountCandidate(
    val amountInCents: Long,
    val kind: TicketAmountKind,
    val sourceLine: String,
    val lineIndex: Int,
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
        val candidates = text.lineSequence().mapIndexedNotNull { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@mapIndexedNotNull null
            val kind = classify(line)
            ListOcrMoneyParser.extractCandidates(line).map { amount ->
                TicketAmountCandidate(amount.amountInCents, kind, line, index)
            }.takeIf(List<TicketAmountCandidate>::isNotEmpty)
        }.flatten().toList()
        return TicketParseResult(candidates)
    }

    private fun classify(line: String): TicketAmountKind {
        val normalized = normalizeProductName(line)
        return when {
            Regex("\\bsubtotal\\b").containsMatchIn(normalized) -> TicketAmountKind.SUBTOTAL
            Regex("\\b(itbis|impuesto|tax)\\b").containsMatchIn(normalized) -> TicketAmountKind.TAX
            Regex("\\b(descuento|discount|rebaja)\\b").containsMatchIn(normalized) -> TicketAmountKind.DISCOUNT
            Regex("\\b(envio|delivery|shipping)\\b").containsMatchIn(normalized) -> TicketAmountKind.SHIPPING
            Regex("\\b(servicio|propina|service|tip)\\b").containsMatchIn(normalized) -> TicketAmountKind.SERVICE
            Regex("\\b(total|importe a pagar|monto a pagar)\\b").containsMatchIn(normalized) -> TicketAmountKind.TOTAL
            else -> TicketAmountKind.UNCLASSIFIED
        }
    }
}
