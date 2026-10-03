package com.angel.mony.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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
    val sourceShoppingListId: Long? = null,
)

fun pendingReminderInstant(
    date: LocalDate,
    time: LocalTime,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Instant = LocalDateTime(date, time).toInstant(timeZone)

fun isPendingReminderInFuture(
    date: LocalDate,
    time: LocalTime,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean = pendingReminderInstant(date, time, timeZone) > now

fun isPendingDateValid(
    date: LocalDate,
    today: LocalDate = currentLocalDate(),
): Boolean = date >= today
