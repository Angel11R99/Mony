package com.angel.mony.domain.model

import java.text.Normalizer
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verificación temporal: compara la implementación común contra la original con Normalizer. */
class NormalizationEquivalenceTest {

    private fun legacyNormalize(value: String): String {
        val basic = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        return basic.split(' ').filter(String::isNotBlank).joinToString(" ") { token ->
            when (token) {
                "evap" -> "evaporada"
                "cond" -> "condensada"
                "gallet" -> "galletas"
                "und", "uds", "unid" -> "unidad"
                "lt", "lts" -> "l"
                "gr", "grs" -> "g"
                "kg", "g", "ml", "l" -> token
                else -> token
            }
        }.replace(Regex("(\\d+)\\s+(kg|g|ml|l)\\b"), "$1$2")
    }

    @Test
    fun `common normalization matches the legacy java implementation`() {
        val corpus = buildList {
            // Barrido de todos los bloques latinos Unicode. Así no dependemos de acertar
            // a mano qué caracteres tienen descomposición canónica.
            for (c in 'À'..'ɏ') {
                add(c.toString())
                add("leche $c Whole")
                add("CAF$c MOLIDO")
            }
            for (c in 'Ḁ'..'ỿ') {
                add(c.toString())
                add("producto $c 250g")
            }
            // Vocabulario real de supermercado dominicano.
            addAll(
                listOf(
                    "  CAFÉ, molido: Santo-Domingo!! ",
                    "Leche EVAP Carnation 315g",
                    "Leche condensada La Key 397 g",
                    "Aroba de arroz premium",
                    "Habichuelas verdes",
                    "Mangu Ecovit",
                    "Sal marina yodada",
                    "Jabón líquido Hypo",
                    "Papel higiénico doble hoja",
                    "Queso blanco duro",
                    "Café molido 250 g",
                    "ACEITE DE OLIVA VIRGEN EXTRA",
                    "Sazón totals 100 g",
                    "Yodada 1lb",
                    "Detergente líquido 4lt",
                    "Refresco de cola 2lt",
                    "Caféinstantáneo 100g",
                    "Pañales para bebé t/3",
                    "Azúcar morena 1kg",
                    "Aji dulceDominicano",
                )
            )
            // Formas ya descompuestas (base + signo combinante).
            add("cafe\u0301 molido")
            add("n\u0303evos")
            add("CORAZ\u0303ON")
            add("MAN\u0303ANA\u0301")
            // Casos límite sin descomposición canónica.
            addAll(listOf("ølsen", "łódka", "đak", "ıspanak", "æon", "straße", "ŉube", "ðor"))
        }

        val mismatches = corpus.map { it to (legacyNormalize(it) to normalizeProductName(it)) }
            .filter { (_, pair) -> pair.first != pair.second }

        fun dump(value: String): String =
            value.map { if (it.code < 128) it.toString() else "\\u%04X".format(it.code) }.joinToString("")

        assertEquals(
            "Divergencias entre la implementación legacy (Normalizer) y la común: " +
                mismatches.size + "\n" +
                mismatches.take(60).joinToString("\n") { (input, res) ->
                    "  in=[${dump(input)}] legacy=[${dump(res.first)}] comun=[${dump(res.second)}]"
                } + "\n" +
                "  primeros codepoints divergentes: " +
                mismatches.map { (i, _) -> i }.map { s -> "\\u%04X".format(s.codePointAt(0)) }.distinct().joinToString(" "),
            0L,
            mismatches.size.toLong(),
        )
    }
}