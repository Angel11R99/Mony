package com.angel.mony.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun FixedEntry.manualPostingDate(today: LocalDate = currentLocalDate()): LocalDate = when (manualDateMode) {
    FixedDateMode.TODAY -> today
    FixedDateMode.PREVIOUS_FORTNIGHT -> previousFortnightEnd(today)
    FixedDateMode.PREVIOUS_MONTH ->
        LocalDate(today.year, today.monthNumber, 1).plus(-1, DateTimeUnit.MONTH).endOfMonth()
    FixedDateMode.SPECIFIC_DATE -> manualSpecificDate ?: today
}

fun previousFortnightEnd(today: LocalDate): LocalDate = if (today.dayOfMonth > 15) {
    LocalDate(today.year, today.monthNumber, 15)
} else {
    LocalDate(today.year, today.monthNumber, 1).plus(-1, DateTimeUnit.MONTH).endOfMonth()
}

fun calculateNextRun(
    mode: FixedScheduleMode,
    hour: Int,
    specificDate: LocalDate?,
    after: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Instant? {
    val safeHour = hour.coerceIn(0, 23)
    val localNow = after.toLocalDateTime(timeZone)
    fun atHour(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime(safeHour, 0)).toInstant(timeZone)

    return when (mode) {
        FixedScheduleMode.MANUAL -> null
        FixedScheduleMode.SPECIFIC_DATE_TIME -> specificDate?.let(::atHour)?.takeIf { it > after }
        FixedScheduleMode.AFTER_MONTH -> {
            var date = LocalDate(localNow.year, localNow.monthNumber, 1)
            while (atHour(date) <= after) date = date.plus(1, DateTimeUnit.MONTH)
            atHour(date)
        }
        FixedScheduleMode.AFTER_FORTNIGHT -> {
            val month = LocalDate(localNow.year, localNow.monthNumber, 1)
            sequenceOf(
                month,
                LocalDate(month.year, month.monthNumber, 16),
                month.plus(1, DateTimeUnit.MONTH),
                month.plus(1, DateTimeUnit.MONTH).let { LocalDate(it.year, it.monthNumber, 16) },
            ).map(::atHour).first { it > after }
        }
    }
}
