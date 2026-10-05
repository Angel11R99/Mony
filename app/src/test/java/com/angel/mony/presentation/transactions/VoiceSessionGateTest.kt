package com.angel.mony.presentation.transactions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSessionGateTest {
    @Test
    fun `close invalidates callbacks and reopen creates a valid session`() {
        val gate = VoiceSessionGate()
        val first = gate.open()
        assertTrue(gate.isCurrent(first))

        gate.invalidate()
        assertFalse(gate.isCurrent(first))

        val second = gate.open()
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }
}
