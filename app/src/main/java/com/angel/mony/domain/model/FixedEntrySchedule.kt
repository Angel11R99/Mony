package com.angel.mony.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

fun FixedEntry.manualPostingDate(today: LocalDate = LocalDate.now()): LocalDate = when (manualDateMode) {
    FixedDateMode.TODAY -> today
    FixedDateMode.PREVIOUS_FORTNIGHT -> previousFortnightEnd(today)
    FixedDateMode.PREVIOUS_MONTH -> YearMonth.from(today).minusMonths(1).atEndOfMonth()
    FixedDateMode.SPECIFIC_DATE -> manualSpecificDate ?: today
}

fun previousFortnightEnd(today: LocalDate): LocalDate = if (today.dayOfMonth > 15) {
    today.withDayOfMonth(15)
} else {
    YearMonth.from(today).minusMonths(1).atEndOfMonth()
}

fun calculateNextRun(
    mode: FixedScheduleMode,
    hour: Int,
    specificDate: LocalDate?,
    after: Instant,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Instant? {
    val safeHour = hour.coerceIn(0, 23)
    val localNow = after.atZone(zoneId)
    fun atHour(date: LocalDate) = date.atTime(safeHour, 0).atZone(zoneId).toInstant()

    return when (mode) {
        FixedScheduleMode.MANUAL -> null
        FixedScheduleMode.SPECIFIC_DATE_TIME -> specificDate?.let(::atHour)?.takeIf { it > after }
        FixedScheduleMode.AFTER_MONTH -> generateSequence(YearMonth.from(localNow).atDay(1)) {
            YearMonth.from(it).plusMonths(1).atDay(1)
        }.map(::atHour).first { it > after }
        FixedScheduleMode.AFTER_FORTNIGHT -> {
            val month = YearMonth.from(localNow)
            sequenceOf(
                month.atDay(1),
                month.atDay(16),
                month.plusMonths(1).atDay(1),
                month.plusMonths(1).atDay(16),
            ).map(::atHour).first { it > after }
        }
    }
}
