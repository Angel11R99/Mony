package com.angel.mony.presentation.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupScreenTest {
    @Test
    fun `phone sized area uses an adaptive amount of icons`() {
        assertEquals(29, calculateIconCount(widthDp = 360f, heightDp = 800f, slotCount = 78))
    }

    @Test
    fun `icon amount stays inside requested limits`() {
        assertEquals(12, calculateIconCount(widthDp = 240f, heightDp = 320f, slotCount = 40))
        assertEquals(60, calculateIconCount(widthDp = 1_200f, heightDp = 1_200f, slotCount = 100))
    }

    @Test
    fun `slot capacity reserves free positions for relocation`() {
        assertEquals(12, calculateIconCount(widthDp = 360f, heightDp = 800f, slotCount = 12))
        assertEquals(16, calculateIconCount(widthDp = 360f, heightDp = 800f, slotCount = 20))
    }
}
