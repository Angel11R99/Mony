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

// ---------------------------------------------------------------------------
// Daily pace (Gasto de hoy)
// ---------------------------------------------------------------------------

/**
 * Daily allowance for the rest of the cycle: remaining budget spread over the
 * days left including today. Null when there is no budget configured.
 */
fun dailyAllowanceInCents(
    budgetInCents: Long?,
    spentInCycleInCents: Long,
    daysLeft: Int,
): Long? {
    if (budgetInCents == null || budgetInCents <= 0L) return null
    val days = daysLeft.coerceAtLeast(1)
    return (budgetInCents - spentInCycleInCents) / days
}

/** 0f..1f+ fraction of the daily allowance already spent today. */
fun dailyPaceFraction(todayExpenseInCents: Long, allowanceInCents: Long?): Float {
    if (allowanceInCents == null || allowanceInCents <= 0L) return 0f
    return todayExpenseInCents.toFloat() / allowanceInCents
}

// ---------------------------------------------------------------------------
// Category limits (Límites por categoría)
// ---------------------------------------------------------------------------

data class CategoryLimitUsage(
    val slice: CategorySlice,
    val limitInCents: Long,
) {
    /** 0f..1f+ usage against the category limit. */
    val fraction: Float get() = if (limitInCents <= 0L) 0f else slice.amountInCents.toFloat() / limitInCents
    val isOverLimit: Boolean get() = limitInCents > 0L && slice.amountInCents > limitInCents
}

/**
 * Pairs top expense slices with their configured category limits. Slices
 * without a limit are kept at the end so limited categories surface first.
 */
fun buildCategoryLimitUsages(slices: List<CategorySlice>): List<CategoryLimitUsage> {
    val (limitedSlices, unlimitedSlices) =
        slices.partition { (it.limitInCents ?: 0L) > 0L }
    val limited = limitedSlices
        .map { CategoryLimitUsage(it, it.limitInCents!!) }
        .sortedWith(
            compareByDescending(CategoryLimitUsage::isOverLimit)
                .thenByDescending(CategoryLimitUsage::fraction),
        )
    val unlimited = unlimitedSlices.map { CategoryLimitUsage(it, 0L) }
    return limited + unlimited
}

fun sumSavedTotals(goals: List<SavingsGoalProgress>): Pair<Long, Long> =
    goals.fold(0L to 0L) { (saved, target), goal ->
        (saved + goal.savedInCents) to (target + goal.goal.targetAmountInCents)
    }
