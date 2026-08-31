package com.angel.mony.presentation.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetLimitParsingTest {
    @Test fun `blank means no limit`() {
        assertEquals(BudgetLimitInput.Valid(null), parseBudgetLimit(""))
        assertEquals(BudgetLimitInput.Valid(null), parseBudgetLimit("   "))
    }

    @Test fun `parses valid amounts to cents`() {
        assertEquals(BudgetLimitInput.Valid(150_000L), parseBudgetLimit("1500"))
        assertEquals(BudgetLimitInput.Valid(150_005L), parseBudgetLimit("1500,05"))
        assertEquals(BudgetLimitInput.Valid(250L), parseBudgetLimit("2.50"))
    }

    @Test fun `zero is an explicit limit`() {
        assertEquals(BudgetLimitInput.Valid(0L), parseBudgetLimit("0"))
    }

    @Test fun `rejects invalid or negative amounts`() {
        assertTrue(parseBudgetLimit("abc") is BudgetLimitInput.Invalid)
        assertTrue(parseBudgetLimit("-5") is BudgetLimitInput.Invalid)
        assertTrue(parseBudgetLimit("12.345") is BudgetLimitInput.Invalid)
    }
}
