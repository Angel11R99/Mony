@file:JvmName("AndroidBudgetCycleCalculator")

package com.angel.mony.domain.model

import com.angel.mony.core.time.toKotlinLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import kotlinx.datetime.LocalTime as KotlinLocalTime

fun activeBudgetPeriod(budget: BudgetConfig?, today: LocalDate): DateRange =
    activeBudgetPeriod(budget, today.toKotlinLocalDate())

fun budgetPeriodForView(budget: BudgetConfig?, view: BudgetPeriodView, today: LocalDate): DateRange =
    budgetPeriodForView(budget, view, today.toKotlinLocalDate())

fun nextBudgetPeriod(budget: BudgetConfig?, today: LocalDate): DateRange =
    nextBudgetPeriod(budget, today.toKotlinLocalDate())

fun previousBudgetPeriod(budget: BudgetConfig?, today: LocalDate): DateRange =
    previousBudgetPeriod(budget, today.toKotlinLocalDate())

fun budgetPeriodForSchedule(schedule: BudgetCycleSchedule, today: LocalDate): DateRange =
    budgetPeriodForSchedule(schedule, today.toKotlinLocalDate())

fun canManuallyCloseBudgetCycle(budget: BudgetConfig?, today: LocalDate): Boolean =
    canManuallyCloseBudgetCycle(budget, today.toKotlinLocalDate())

fun shouldAutomaticallyCloseBudgetCycle(
    budget: BudgetConfig?,
    now: LocalDateTime,
    closeTime: LocalTime,
): Boolean = shouldAutomaticallyCloseBudgetCycle(
    budget = budget,
    now = KotlinLocalDateTime(
        now.year,
        now.monthValue,
        now.dayOfMonth,
        now.hour,
        now.minute,
        now.second,
        now.nano,
    ),
    closeTime = KotlinLocalTime(closeTime.hour, closeTime.minute, closeTime.second, closeTime.nano),
)

fun shouldAutomaticallyCloseBudgetCycle(budget: BudgetConfig?, now: LocalDateTime): Boolean =
    shouldAutomaticallyCloseBudgetCycle(budget, now, LocalTime.of(21, 0))

fun budgetPeriodToClose(budget: BudgetConfig, today: LocalDate): DateRange =
    budgetPeriodToClose(budget, today.toKotlinLocalDate())
