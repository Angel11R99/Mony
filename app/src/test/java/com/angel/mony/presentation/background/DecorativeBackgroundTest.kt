package com.angel.mony.presentation.background

import org.junit.Assert.assertEquals
import org.junit.Test

class DecorativeBackgroundTest {
    @Test
    fun `intensity maps to subtle visual alpha`() {
        assertEquals(0f, decorationVisualAlpha(0f), 0.0001f)
        assertEquals(0.15f, decorationVisualAlpha(0.5f), 0.0001f)
        assertEquals(0.30f, decorationVisualAlpha(1f), 0.0001f)
    }

    @Test
    fun `visual alpha clamps invalid intensity`() {
        assertEquals(0f, decorationVisualAlpha(-1f), 0.0001f)
        assertEquals(0.30f, decorationVisualAlpha(2f), 0.0001f)
    }

    @Test
    fun `medical decoration uses stronger visual alpha`() {
        assertEquals(0.21f, medicalDecorationVisualAlpha(0.5f), 0.0001f)
        assertEquals(0.42f, medicalDecorationVisualAlpha(1f), 0.0001f)
    }
}
