package com.angel.mony.data.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VoiceModelJsonExtractionTest {
    @Test fun plainJson() {
        assertEquals("""{"type":"EXPENSE","amountInCents":50000}""",
            extractLastJsonObject("""{"type":"EXPENSE","amountInCents":50000}"""))
    }

    @Test fun codeFencesAndText() {
        assertEquals("""{"type":"INCOME"}""",
            extractLastJsonObject("Respuesta:\n```json\n{\"type\":\"INCOME\"}\n```"))
    }

    @Test fun nestedObjectsAndBracesInStrings() {
        val json = """{"note":"La cena {para dos} cuesta 500","other":{"status":"ok"}}"""
        assertEquals(json, extractLastJsonObject(json))
    }

    @Test fun choosesLastCompleteResult() {
        assertEquals("""{"type":"EXPENSE"}""",
            extractLastJsonObject("""Razonamiento {"type":"INCOME"} final {"type":"EXPENSE"}"""))
    }

    @Test fun rejectsIncompleteOrAbsentJson() {
        assertThrows(IllegalArgumentException::class.java) { extractLastJsonObject("Respuesta vacía") }
        assertThrows(IllegalArgumentException::class.java) { extractLastJsonObject("{\"type\":\"EXPENSE\"") }
    }

    @Test fun ignoresEscapedQuotes() {
        val json = """{"note":"dijo \"{cena}\"","type":"EXPENSE"}"""
        assertEquals(json, extractLastJsonObject(json))
    }
}
