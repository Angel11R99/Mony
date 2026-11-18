package com.angel.mony.data.nlp

/**
 * Extracts the last balanced JSON object from generative model output.
 * Accepts markdown fences and other prose; treats braces inside strings as data.
 * Does not invent financial values when the model fails to produce an object.
 */
internal fun extractLastJsonObject(response: String): String {
    var start = -1
    var depth = 0
    var quoted = false
    var escaped = false
    var last: String? = null
    for (index in response.indices) {
        val char = response[index]
        if (depth == 0) {
            if (char == '{') {
                start = index
                depth = 1
                quoted = false
                escaped = false
            }
            continue
        }
        if (quoted) {
            if (escaped) escaped = false
            else if (char == '\\') escaped = true
            else if (char == '"') quoted = false
            continue
        }
        when (char) {
            '"' -> quoted = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0 && start >= 0) last = response.substring(start, index + 1)
            }
        }
    }
    return last ?: throw IllegalArgumentException("Model response did not contain a complete JSON object")
}
