package com.example.personalfinancetracker.widget

import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.DateRange
import com.example.personalfinancetracker.domain.model.SavingsGoalProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-DO"))

fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

fun cycleDaysLeft(period: DateRange, today: LocalDate): Int =
    ChronoUnit.DAYS.between(today, period.endInclusive).coerceAtLeast(0).toInt()

enum class PendingDayKind { TODAY, TOMORROW, OVERDUE, ON_DATE }

data class PendingDayLabel(val kind: PendingDayKind, val dateText: String)

fun pendingDayLabel(date: LocalDate, today: LocalDate): PendingDayLabel = when {
    date.isBefore(today) -> PendingDayLabel(PendingDayKind.OVERDUE, formatShortDate(date))
    date == today -> PendingDayLabel(PendingDayKind.TODAY, formatShortDate(date))
    date == today.plusDays(1) -> PendingDayLabel(PendingDayKind.TOMORROW, formatShortDate(date))
    else -> PendingDayLabel(PendingDayKind.ON_DATE, formatShortDate(date))
}

fun signedAmountLabel(isIncome: Boolean, amountInCents: Long): String =
    (if (isIncome) "+" else "−") + MoneyFormatter.format(amountInCents)

data class ExpenseTrend(val direction: Direction, val percent: Int?) {
    enum class Direction { UP, DOWN, FLAT }
}

fun expenseTrend(currentExpenseInCents: Long, previousExpenseInCents: Long?): ExpenseTrend? {
    if (previousExpenseInCents == null || previousExpenseInCents <= 0L) return null
    val percent = (((currentExpenseInCents - previousExpenseInCents) * 100) / previousExpenseInCents).toInt()
    val direction = when {
        percent > 0 -> ExpenseTrend.Direction.UP
        percent < 0 -> ExpenseTrend.Direction.DOWN
        else -> ExpenseTrend.Direction.FLAT
    }
    return ExpenseTrend(direction, abs(percent))
}

fun sortSavingsGoals(goals: List<SavingsGoalProgress>): List<SavingsGoalProgress> =
    goals.sortedWith(
        compareBy(SavingsGoalProgress::isCompleted)
            .thenByDescending { it.percent }
            .thenByDescending { it.savedInCents },
    )
