package com.example.personalfinancetracker.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(start)) }

    companion object {
        fun currentFortnight(today: LocalDate = LocalDate.now()): DateRange {
            val month = YearMonth.from(today)
            return if (today.dayOfMonth <= 15) {
                DateRange(month.atDay(1), month.atDay(15))
            } else {
                DateRange(month.atDay(16), month.atEndOfMonth())
            }
        }

        fun currentMonth(today: LocalDate = LocalDate.now()): DateRange {
            val month = YearMonth.from(today)
            return DateRange(month.atDay(1), month.atEndOfMonth())
        }

        fun current(period: BudgetPeriod, today: LocalDate = LocalDate.now()): DateRange =
            when (period) {
                BudgetPeriod.MONTHLY -> currentMonth(today)
                BudgetPeriod.FORTNIGHTLY -> currentFortnight(today)
            }
    }
}
