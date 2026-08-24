package com.example.personalfinancetracker.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class PendingType {
    PAYMENT,
    COLLECTION,
}

fun PendingType.toTransactionType(): TransactionType = when (this) {
    PendingType.PAYMENT -> TransactionType.EXPENSE
    PendingType.COLLECTION -> TransactionType.INCOME
}

fun PendingType.label(): String = when (this) {
    PendingType.PAYMENT -> "Pago"
    PendingType.COLLECTION -> "Cobro"
}

enum class PendingPeriodFilter {
    FORTNIGHT,
    MONTH,
    ALL,
}

enum class PendingCardSize(val label: String) {
    COMPACT("Compacto"),
    NORMAL("Normal"),
    DETAILED("Detallado"),
}

data class PendingEntry(
    val id: Long = 0,
    val type: PendingType,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val date: LocalDate,
    val reminderTime: LocalTime? = null,
    val comment: String? = null,
    val isDone: Boolean = false,
    val doneAt: Instant? = null,
    val transactionId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun pendingReminderInstant(
    date: LocalDate,
    time: LocalTime,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Instant = date.atTime(time).atZone(zoneId).toInstant()

fun isPendingReminderInFuture(
    date: LocalDate,
    time: LocalTime,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Boolean = pendingReminderInstant(date, time, zoneId).isAfter(now)

fun isPendingDateValid(
    date: LocalDate,
    today: LocalDate = LocalDate.now(),
): Boolean = !date.isBefore(today)
