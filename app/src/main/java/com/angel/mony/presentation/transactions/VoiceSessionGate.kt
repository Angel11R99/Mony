package com.angel.mony.presentation.transactions

internal class VoiceSessionGate {
    private var generation = 0L

    fun open(): Long = ++generation

    fun invalidate() {
        generation++
    }

    fun isCurrent(sessionId: Long): Boolean = sessionId == generation
}
