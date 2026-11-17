package com.angel.mony.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalLanguageModelSelectionTest {
    private val priority = listOf("qwen-small", "qwen-large")

    @Test
    fun keepsPreviouslySelectedModelWhenItIsInstalled() {
        assertEquals(
            "qwen-large",
            resolveInstalledLanguageModel("qwen-large", setOf("qwen-small", "qwen-large"), priority),
        )
    }

    @Test
    fun fallsBackToFirstInstalledModelWhenPreferenceIsUnavailable() {
        assertEquals(
            "qwen-small",
            resolveInstalledLanguageModel("qwen-large", setOf("qwen-small"), priority),
        )
    }

    @Test
    fun returnsNoModelWhenCatalogHasNoInstalledFiles() {
        assertNull(resolveInstalledLanguageModel("qwen-large", emptySet(), priority))
    }
}
