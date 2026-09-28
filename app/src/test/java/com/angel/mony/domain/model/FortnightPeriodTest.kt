package com.angel.mony.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FortnightPeriodTest {
    private val defaultSchedules = defaultCycleSchedules(BudgetPeriod.FORTNIGHTLY)

    @Test
    fun `sin configuracion el calendario es 1 al 15 y 16 al fin de mes`() {
        assertEquals(
            DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-01"), null),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-16"), null),
        )
    }

    @Test
    fun `la primera quincena es la que contiene el dia 15`() {
        val period = fortnightPeriodContaining(LocalDate.parse("2026-10-15"), null)

        assertEquals(LocalDate.parse("2026-10-15"), period.endInclusive)
    }

    @Test
    fun `febrero usa el ultimo dia disponible`() {
        val period = fortnightPeriodContaining(LocalDate.parse("2026-02-20"), null)

        assertEquals(LocalDate.parse("2026-02-28"), period.endInclusive)
    }

    @Test
    fun `un ciclo personalizado se respeta en vez de 1 al 15`() {
        val budget = BudgetConfig(
            amountInCents = 50_000L,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleSchedules = listOf(
                BudgetCycleSchedule(openingDay = 10, closingDay = 24),
                BudgetCycleSchedule(openingDay = 25, closingDay = 9),
            ),
        )

        assertEquals(
            DateRange(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-24")),
            fortnightPeriodContaining(LocalDate.parse("2026-10-12"), budget),
        )
        assertEquals(
            DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09")),
            fortnightPeriodContaining(LocalDate.parse("2026-11-02"), budget),
        )
    }

    @Test
    fun `un presupuesto mensual no altera la definicion de quincena`() {
        val budget = BudgetConfig(amountInCents = 50_000L, period = BudgetPeriod.MONTHLY)

        assertEquals(
            defaultSchedules,
            fortnightSchedules(budget),
        )
    }

    @Test
    fun `la segunda quincena cruza al mes siguiente`() {
        val period = DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09"))

        assertEquals(FortnightSlot.SECOND, fortnightSlotFor(period, defaultSchedules))
    }

    @Test
    fun `el siguiente periodo continua la navegacion`() {
        val current = DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"))

        assertEquals(
            DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")),
            nextFortnightPeriod(current, defaultSchedules),
        )
    }

    @Test
    fun `el siguiente periodo cruza de mes al final del ciclo`() {
        val current = DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31"))

        assertEquals(
            DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-15")),
            nextFortnightPeriod(current, defaultSchedules),
        )
    }

    @Test
    fun `el periodo anterior retrocede correctamente`() {
        val current = DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-15"))

        assertEquals(
            DateRange(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-31")),
            previousFortnightPeriod(current, defaultSchedules),
        )
    }

    @Test
    fun `la navegacion siempre avanza hacia adelante`() {
        val first = DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15"))
        val second = nextFortnightPeriod(first, defaultSchedules)
        val third = nextFortnightPeriod(second, defaultSchedules)

        assertTrueAfter(second, first)
        assertTrueAfter(third, second)
    }

    @Test
    fun `el slot refleja el calendario configurado`() {
        val custom = listOf(
            BudgetCycleSchedule(openingDay = 10, closingDay = 24),
            BudgetCycleSchedule(openingDay = 25, closingDay = 9),
        )

        assertEquals(
            FortnightSlot.FIRST,
            fortnightSlotFor(DateRange(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-24")), custom),
        )
        assertEquals(
            FortnightSlot.SECOND,
            fortnightSlotFor(DateRange(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-11-09")), custom),
        )
    }

    private fun assertTrueAfter(later: DateRange, earlier: DateRange) {
        assertEquals(true, later.start.isAfter(earlier.start))
    }
}
