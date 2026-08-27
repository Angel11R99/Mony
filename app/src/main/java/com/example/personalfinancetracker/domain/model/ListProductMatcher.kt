package com.example.personalfinancetracker.domain.model

import java.text.Normalizer
import java.util.Locale

data class ProductMatchCandidate(
    val id: Long,
    val name: String,
    val barcode: String? = null,
    val isPurchased: Boolean = false,
    val isIdentified: Boolean = false,
)

sealed interface ProductMatchResult {
    data class Clear(val itemId: Long) : ProductMatchResult
    data class Ambiguous(val itemId: Long, val candidateName: String) : ProductMatchResult
    data object None : ProductMatchResult
}

object ListProductMatcher {
    private const val CLEAR_SCORE = 0.75
    private const val SUGGESTION_SCORE = 0.45
    private const val CLEAR_MARGIN = 0.15

    fun match(
        name: String?,
        barcode: String?,
        candidates: List<ProductMatchCandidate>,
    ): ProductMatchResult {
        val pending = candidates.filterNot { it.isPurchased || it.isIdentified }
        if (pending.isEmpty()) return ProductMatchResult.None

        val normalizedBarcode = barcode?.trim()?.takeIf(String::isNotEmpty)
        if (normalizedBarcode != null) {
            pending.firstOrNull { it.barcode?.trim() == normalizedBarcode }?.let {
                return ProductMatchResult.Clear(it.id)
            }
        }

        val normalizedInput = normalizeProductName(name.orEmpty())
        if (normalizedInput.isEmpty()) return ProductMatchResult.None

        val ranked = pending
            .map { it to nameMatchScore(normalizedInput, normalizeProductName(it.name)) }
            .filter { it.second >= SUGGESTION_SCORE }
            .sortedWith(compareByDescending<Pair<ProductMatchCandidate, Double>> { it.second }.thenBy { it.first.id })
        val best = ranked.firstOrNull() ?: return ProductMatchResult.None
        val secondScore = ranked.getOrNull(1)?.second ?: 0.0
        val isClear = best.second >= CLEAR_SCORE && best.second - secondScore >= CLEAR_MARGIN

        return if (isClear) {
            ProductMatchResult.Clear(best.first.id)
        } else {
            ProductMatchResult.Ambiguous(best.first.id, best.first.name)
        }
    }
}

fun normalizeProductName(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")

private fun nameMatchScore(input: String, candidate: String): Double {
    if (candidate.isEmpty()) return 0.0
    if (input == candidate) return 1.0

    val inputTokens = input.split(' ').filter { it.length >= 2 }.toSet()
    val candidateTokens = candidate.split(' ').filter { it.length >= 2 }.toSet()
    if (inputTokens.isEmpty() || candidateTokens.isEmpty()) return 0.0

    val common = inputTokens.intersect(candidateTokens).size
    if (common == 0) return 0.0
    val shorterTokens = minOf(inputTokens.size, candidateTokens.size)
    val longerTokens = maxOf(inputTokens.size, candidateTokens.size)
    val coverage = common.toDouble() / shorterTokens
    val overlap = common.toDouble() / longerTokens

    val phraseIncluded = (candidate.contains(input) || input.contains(candidate)) &&
        minOf(input.length, candidate.length) >= 4
    if (phraseIncluded) {
        return if (shorterTokens >= 2 || inputTokens == candidateTokens) 0.85 + overlap * 0.1 else 0.7
    }
    return coverage * 0.6 + overlap * 0.4
}
