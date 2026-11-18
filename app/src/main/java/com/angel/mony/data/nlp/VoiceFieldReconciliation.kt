package com.angel.mony.data.nlp

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.SpokenMoney
import com.angel.mony.domain.model.SpanishMoneyParser
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.voiceNormalized
import java.time.LocalDate

/** Verifies explicitly spoken facts; keeps uncertain fields unresolved. */
internal object VoiceFieldReconciliation {
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
            categoryUnresolved = chosen == null && (modelCategory?.isNotBlank() == true || mentioned.size > 1))
    }

    private fun relativeDate(text: String, today: LocalDate): LocalDate? = when {
        Regex("""\banteayer\b""").containsMatchIn(text) -> today.minusDays(2)
        Regex("""\bayer\b|\banoche\b""").containsMatchIn(text) -> today.minusDays(1)
        Regex("""\bhoy\b""").containsMatchIn(text) -> today
        else -> null
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
        }.distinct().toList()
        return values.singleOrNull()
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
        )
        val targets = aliases.filterKeys { containsName(text, it) }.values.flatten().toSet()
        return active.filter { normalized(it.name) in targets }.singleOrNull()
    }
}
