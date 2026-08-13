package com.example.personalfinancetracker.presentation.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountVisualTransformationTest {
    @Test fun `adds thousands separators while preserving decimals`() {
        val transformed = AmountVisualTransformation.filter(AnnotatedString("25000.50"))

        assertEquals("25,000.50", transformed.text.text)
    }

    @Test fun `sanitizes pasted grouping separators and decimal precision`() {
        assertEquals("25000.50", sanitizeAmountInput("25,000.509"))
    }

    @Test fun `maps cursor across inserted separator`() {
        val transformed = AmountVisualTransformation.filter(AnnotatedString("25000"))

        assertEquals(3, transformed.offsetMapping.originalToTransformed(2))
        assertEquals(2, transformed.offsetMapping.transformedToOriginal(3))
    }
}
