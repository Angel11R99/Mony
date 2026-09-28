package com.angel.mony.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Modo en el que opera el módulo según el ciclo de cobro del usuario.
 *
 * [FORTNIGHTLY] parte el mes en dos períodos (1ra y 2da quincena).
 * [MONTHLY] trata el mes como un único período: quien cobra por mes no tiene
 * "primera ni segunda quincena".
 */
enum class FortnightPeriodStyle {
    FORTNIGHTLY,
    MONTHLY,
}

/** Modo del módulo según la configuración presupuestaria del usuario. */
fun fortnightPeriodStyle(budget: BudgetConfig?): FortnightPeriodStyle =
    when (budget?.period) {
        BudgetPeriod.MONTHLY -> FortnightPeriodStyle.MONTHLY
        else -> FortnightPeriodStyle.FORTNIGHTLY
    }

/**
 * Resolución de períodos del módulo.
 *
 * Reutiliza los mismos [BudgetCycleSchedule] y el mismo clamp de días que
 * [BudgetCycleCalculator], de modo que el módulo nunca redefina qué significa
 * "quincena": si el usuario configuró un ciclo personalizado, ambos módulos
 * coinciden.
 *
 * Para un presupuesto mensual solo existe un período por mes: se toma el
 * primer ciclo configurado (o su default de 1-al fin de mes), nunca dos.
 */
fun fortnightSchedules(budget: BudgetConfig?): List<BudgetCycleSchedule> {
    if (budget == null) return defaultCycleSchedules(BudgetPeriod.FORTNIGHTLY)
    val schedules = budget.cycleSchedules.distinct().ifEmpty {
        defaultCycleSchedules(budget.period)
    }
    return when (fortnightPeriodStyle(budget)) {
        FortnightPeriodStyle.MONTHLY -> listOf(schedules.first())
        FortnightPeriodStyle.FORTNIGHTLY -> schedules
    }
}

private fun fortnightPeriodsAround(
    date: LocalDate,
    schedules: List<BudgetCycleSchedule>,
): List<DateRange> {
    val referenceMonth = YearMonth.from(date)
    return (-1..1)
        .flatMap { offset -> schedules.map { it.toDateRange(referenceMonth.plusMonths(offset.toLong())) } }
        .distinct()
        .sortedBy(DateRange::start)
}

/** Período quincenal (1ra o 2da) que contiene la fecha dada. */
fun fortnightPeriodContaining(
    date: LocalDate,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(date, schedules)
    .firstOrNull { date in it.start..it.endInclusive }
    ?: schedules.first().toDateRange(YearMonth.from(date))

/** Período quincenal actual según la configuración presupuestaria del usuario. */
fun fortnightPeriodContaining(
    date: LocalDate,
    budget: BudgetConfig?,
): DateRange = fortnightPeriodContaining(date, fortnightSchedules(budget))

/** Siguiente período quincenal, para la navegación hacia adelante. */
fun nextFortnightPeriod(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(period.start, schedules)
    .firstOrNull { it.start.isAfter(period.start) }
    ?: schedules.first().toDateRange(YearMonth.from(period.start).plusMonths(1))

fun nextFortnightPeriod(period: DateRange, budget: BudgetConfig?): DateRange =
    nextFortnightPeriod(period, fortnightSchedules(budget))

/** Período quincenal anterior, para la navegación hacia atrás. */
fun previousFortnightPeriod(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): DateRange = fortnightPeriodsAround(period.start, schedules)
    .filter { it.endInclusive.isBefore(period.start) }
    .maxByOrNull(DateRange::endInclusive)
    ?: schedules.last().toDateRange(YearMonth.from(period.start).minusMonths(1))

fun previousFortnightPeriod(period: DateRange, budget: BudgetConfig?): DateRange =
    previousFortnightPeriod(period, fortnightSchedules(budget))

/** 1ra o 2da quincena según el calendario configurado. */
fun fortnightSlotFor(
    period: DateRange,
    schedules: List<BudgetCycleSchedule>,
): FortnightSlot {
    val openingMonth = YearMonth.from(period.start)
    val index = schedules.indexOfFirst { it.toDateRange(openingMonth) == period }
    return when (index) {
        1 -> FortnightSlot.SECOND
        0 -> FortnightSlot.FIRST
        else -> if (period.start.dayOfMonth <= 15) FortnightSlot.FIRST else FortnightSlot.SECOND
    }
}

fun fortnightSlotFor(period: DateRange, budget: BudgetConfig?): FortnightSlot =
    fortnightSlotFor(period, fortnightSchedules(budget))
