package com.angel.mony.data.nlp

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.SpokenMoney
import com.angel.mony.domain.model.SpanishMoneyParser
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.voiceNormalized
import java.time.LocalDate

/** Verifies explicitly spoken facts; keeps uncertain fields unresolved. */
internal object VoiceFieldReconciliation {
    fun conciseNote(note: String?): String? {
        if (note.isNullOrBlank()) return null
        val cleaned = note.trim()
            .replace(Regex("\\s+(?:(?:del?|de)\\s+)?(?:hoy|ayer|anoche|anteayer|el\\s+(?:lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)|lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:(?:de|del|en|por|para|el|la|los|las|un|una)\\s+)+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:de|del|en|por|para|el|la|los|las|un|una)$", RegexOption.IGNORE_CASE), "")
            .trim(' ', ',', '.', ';', ':')
        return cleaned.takeIf(String::isNotBlank)
    }

    data class Verified(
        val type: TransactionType?,
        val amountInCents: Long?,
        val categoryId: Long?,
        val date: LocalDate?,
        val categoryUnresolved: Boolean = false,
    )
    fun verify(
        transcript: String,
        modelType: TransactionType?,
        modelAmountInCents: Long?,
        modelCategory: String?,
        modelDate: LocalDate?,
        categories: List<Category>,
        today: LocalDate,
    ): Verified {
        val phrase = transcript.voiceNormalized()
        val type = explicitType(phrase) ?: modelType
        val amount = explicitAmount(phrase) ?: modelAmountInCents
        val date = relativeDate(phrase, today) ?: modelDate
        val active = categories.filter { it.isActive && it.type == type }
        val mentioned = active.filter { nameMentioned(phrase, it.name) }
        val modelMatches = active.filter {
            normalized(it.name) == normalized(modelCategory.orEmpty()) ||
                (modelCategory?.isNotBlank() == true && singular(normalized(it.name)) == singular(normalized(modelCategory)))
        }
        val chosen = mentioned.singleOrNull() ?: modelMatches.singleOrNull() ?: semanticAlias(phrase, active)
        return Verified(type, amount, chosen?.id, date,
            categoryUnresolved = chosen == null && type != null)
    }

    private fun relativeDate(text: String, today: LocalDate): LocalDate? {
        val offsets = buildSet {
            if (Regex("""\banteayer\b""").containsMatchIn(text)) add(-2)
            if (Regex("""\bayer\b|\banoche\b""").containsMatchIn(text)) add(-1)
            if (Regex("""\bhoy\b""").containsMatchIn(text)) add(0)
        }
        // "Me pagaron hoy por trabajo de ayer" contains two dates. Do not
        // force either date over the model's contextual interpretation.
        return offsets.singleOrNull()?.let { today.plusDays(it.toLong()) }
    }

    private fun explicitType(text: String): TransactionType? {
        val income = Regex("""\bingreso(?:s)?\b|\bme pagaron\b|\bnos pagaron\b|\brecibi\b|\bcobre\b""").containsMatchIn(text)
        val expense = Regex("""\bgasto(?:s)?\b|\bgaste\b|\bcompre\b|\bcompramos\b|\bpague\b|\bpagamos\b""").containsMatchIn(text)
        return when {
            income && !expense -> TransactionType.INCOME
            expense && !income -> TransactionType.EXPENSE
            else -> null
        }
    }

    private fun explicitAmount(text: String): Long? {
        // Monetary tokens only: avoid treating dates or quantities as money.
        val pattern = Regex("""(?<![a-z0-9])(?:rd\$\s*)?(\d[\d.,]*\s*(?:mil|millones?)?\s*pesos?|\d[\d.,]*\s*(?:mil|millones?)\b|rd\$\s*\d[\d.,]*)(?![a-z])""")
        val values = pattern.findAll(text).mapNotNull { match ->
            (SpanishMoneyParser.parse(match.value.trim()) as? SpokenMoney.Valid)?.cents
        }.toMutableList()
        // SpeechRecognizer may write amounts as words ("quinientos pesos",
        // "veinte mil pesos"). Reuse the existing monetary parser rather than
        // asking the language model to convert a number it may miscalculate.
        val words = text.split(Regex("""\s+"""))
        for (index in words.indices) {
            if (words[index] != "pesos" && words[index] != "peso") continue
            // Digit amounts are already handled above. Never re-interpret
            // the trailing word "mil" in "20 mil pesos" as a separate 1000.
            if (index > 0 && words[index - 1].any(Char::isDigit)) continue
            if (index > 1 && words[index - 1] == "mil" &&
                words[index - 2].any(Char::isDigit)) continue
            val candidates = (1..minOf(index, 6)).mapNotNull { length ->
                val prefix = words.subList(index - length, index)
                // parseDigits ignores other words; letting "2 artículos por
                // quinientos" through would incorrectly return two pesos.
                if (prefix.any { token -> token.any(Char::isDigit) }) return@mapNotNull null
                val token = prefix.joinToString(" ") + " pesos"
                (SpanishMoneyParser.parse(token) as? SpokenMoney.Valid)?.let { length to it.cents }
            }
            candidates.maxByOrNull { it.first }?.let { values += it.second }
        }
        return values.distinct().singleOrNull()
    }

    private fun normalized(s: String) = s.voiceNormalized()
        .replace(Regex("""[^a-z0-9 ]"""), " ").replace(Regex("""\s+"""), " ").trim()
    private fun singular(s: String) = s.split(' ').joinToString(" ") {
        if (it.length > 4 && it.endsWith("s")) it.dropLast(1) else it
    }
    private fun containsName(text: String, name: String): Boolean {
        if (name.isBlank()) return false
        return Regex("(?:^|[^a-z0-9])" + Regex.escape(name) + "(?:$|[^a-z0-9])").containsMatchIn(text)
    }
    private fun nameMentioned(text: String, name: String): Boolean =
        containsName(text, normalized(name)) || containsName(text, singular(normalized(name)))

    private fun semanticAlias(text: String, active: List<Category>): Category? {
        val aliases = mapOf(
            "comida" to setOf("alimentacion", "comida"),
            "cena" to setOf("alimentacion", "comida"),
            "almuerzo" to setOf("alimentacion", "comida"),
            "trabajo" to setOf("trabajo", "ingresos laborales"),
            "compra" to setOf("compras", "compra"),
            "compramos" to setOf("compras", "compra"),
        )
        val targets = aliases.filterKeys { containsName(text, it) }.values.flatten().toSet()
        return active.filter { normalized(it.name) in targets }.singleOrNull()
    }
}
