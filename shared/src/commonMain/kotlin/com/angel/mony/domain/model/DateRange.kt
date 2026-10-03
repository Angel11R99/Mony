package com.angel.mony.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(endInclusive >= start) }

    companion object {
        fun currentFortnight(today: LocalDate = currentLocalDate()): DateRange {
            val firstDay = LocalDate(today.year, today.monthNumber, 1)
            return if (today.dayOfMonth <= 15) {
                DateRange(firstDay, LocalDate(today.year, today.monthNumber, 15))
            } else {
                DateRange(LocalDate(today.year, today.monthNumber, 16), firstDay.endOfMonth())
            }
        }

        fun currentMonth(today: LocalDate = currentLocalDate()): DateRange {
            val firstDay = LocalDate(today.year, today.monthNumber, 1)
            return DateRange(firstDay, firstDay.endOfMonth())
        }

        fun current(period: BudgetPeriod, today: LocalDate = currentLocalDate()): DateRange =
            when (period) {
                BudgetPeriod.MONTHLY -> currentMonth(today)
                BudgetPeriod.FORTNIGHTLY -> currentFortnight(today)
            }
    }
}

internal fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun LocalDate.endOfMonth(): LocalDate =
    LocalDate(year, monthNumber, 1).plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
