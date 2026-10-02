package com.angel.mony.core.text

/**
 * Sustituye a `java.text.Normalizer.normalize(value, NFD)` seguido de
 * `Regex("\\p{M}+")` en código común.
 *
 * `Normalizer` y la clase de caracteres Unicode `\p{M}` no existen en Kotlin/Native.
 * [DIACRITIC_BASE] está generada a partir de esas mismas operaciones, por lo que aplicar
 * la tabla carácter a carácter produce un resultado idéntico al de la implementación
 * original: la descomposición canónica no cruza límites de carácter, de modo que
 * normalizar y limpiar cada carácter por separado equivale a hacerlo sobre la cadena entera.
 *
 * Los signos de combinación están mapeados a cadena vacía, así que desaparecen igual que
 * con `\p{M}+`. Los caracteres sin descomposición canónica (`ø`, `đ`, `ł`, `æ`…)
 * no aparecen en la tabla y se conservan, para que el filtro posterior `[^a-z0-9]+` los
 * trate exactamente igual que antes.
 */
internal fun String.stripDiacritics(): String {
    if (none { it in DIACRITIC_BASE }) return this
    return buildString(length) {
        for (character in this@stripDiacritics) {
            val replacement = DIACRITIC_BASE[character]
            if (replacement != null) append(replacement) else append(character)
        }
    }
}