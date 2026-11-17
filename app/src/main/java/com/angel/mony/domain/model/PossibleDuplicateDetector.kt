package com.angel.mony.domain.model

import java.text.Normalizer
import java.time.temporal.ChronoUnit

/** Returns a recent transaction worth reviewing; callers must ask the user before proceeding. */
fun findPossibleDuplicate(
    candidate: FinanceTransaction,
    existing: List<FinanceTransaction>,
): FinanceTransaction? = existing
    .asSequence()
    .filter { it.type == candidate.type && it.amountInCents == candidate.amountInCents }
    .filter { it.categoryId == candidate.categoryId }
    .filter { kotlin.math.abs(ChronoUnit.DAYS.between(it.date, candidate.date)) <= 1 }
    .filter { notesAreSimilar(candidate.description, it.description) }
    .maxByOrNull { it.date }

private fun notesAreSimilar(first: String?, second: String?): Boolean {
    val a = normalizedTokens(first)
    val b = normalizedTokens(second)
    if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty()
    val overlap = a.intersect(b).size.toDouble() / maxOf(a.size, b.size)
    return overlap >= 0.5
}

private fun normalizedTokens(value: String?): Set<String> = value.orEmpty()
    .lowercase()
    .let { Normalizer.normalize(it, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "") }
    .split(Regex("[^a-z0-9]+"))
    .filter(String::isNotBlank)
    .toSet()
