package com.example.personalfinancetracker.presentation.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Adds thousands separators without changing the numeric value stored by the text field. */
object AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val decimalIndex = text.text.indexOf('.').let { if (it == -1) text.length else it }
        val separatorsBefore = (1 until decimalIndex).filter { index ->
            (decimalIndex - index) % 3 == 0
        }
        val separatorSet = separatorsBefore.toSet()
        val formatted = buildString(text.length + separatorsBefore.size) {
            text.text.forEachIndexed { index, char ->
                if (index in separatorSet) append(',')
                append(char)
            }
        }
        val separatorPositions = separatorsBefore.mapIndexed { index, originalPosition ->
            originalPosition + index
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + separatorsBefore.count { it <= offset }).coerceIn(0, formatted.length)

            override fun transformedToOriginal(offset: Int): Int =
                (offset - separatorPositions.count { it < offset }).coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/** Keeps a positive monetary input numeric, with at most two decimal places. */
fun sanitizeAmountInput(value: String): String {
    val withoutGrouping = value.replace(",", "")
    val result = StringBuilder()
    var hasDecimal = false
    var decimalDigits = 0
    withoutGrouping.forEach { char ->
        when {
            char.isDigit() && (!hasDecimal || decimalDigits < 2) -> {
                result.append(char)
                if (hasDecimal) decimalDigits++
            }
            char == '.' && !hasDecimal -> {
                if (result.isEmpty()) result.append('0')
                result.append(char)
                hasDecimal = true
            }
        }
    }
    return result.toString()
}
