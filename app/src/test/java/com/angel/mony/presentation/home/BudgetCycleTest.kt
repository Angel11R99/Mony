package com.angel.mony.presentation.home

import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.BudgetPeriodView
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.model.availableForBudget
import com.angel.mony.domain.model.belongsToActiveBudgetCycle
import com.angel.mony.domain.model.budgetPeriodForView
import com.angel.mony.domain.model.budgetPeriodForSchedule
import com.angel.mony.domain.model.budgetPeriodToClose
import com.angel.mony.domain.model.canManuallyCloseBudgetCycle
import com.angel.mony.domain.model.nextBudgetPeriod
import com.angel.mony.domain.model.shouldAutomaticallyCloseBudgetCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BudgetCycleTest {
    private val today = LocalDate.of(2026, 8, 13)

    @Test fun `explicit opening and closing days are inclusive`() {
        val config = customScheduleConfig()

        assertEquals(
            DateRange(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 29)),
            activeBudgetPeriod(config, LocalDate.of(2026, 8, 20)),
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 14)),
            nextBudgetPeriod(config, LocalDate.of(2026, 8, 20)),
        )
    }

    @Test fun `cross month cycle is current through its configured closing day`() {
        val config = customScheduleConfig()

        assertEquals(
            DateRange(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 14)),
            activeBudgetPeriod(config, LocalDate.of(2026, 9, 14)),
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 29)),
            nextBudgetPeriod(config, LocalDate.of(2026, 9, 14)),
        )
    }

    @Test fun `day 31 is clamped to the end of short months`() {
        val config = BudgetConfig(
            amountInCents = 100_000,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleSchedules = listOf(
                BudgetCycleSchedule(1, 15),
                BudgetCycleSchedule(16, 31),
            ),
        )

        assertEquals(
            DateRange(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 28)),
            activeBudgetPeriod(config, LocalDate.of(2026, 2, 20)),
        )
    }

    @Test fun `default next fortnight follows first fortnight`() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)

        assertEquals(
            DateRange(LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31)),
            budgetPeriodForView(config, BudgetPeriodView.NEXT, today),
        )
    }

    @Test fun `schedule period selects its occurrence in the current month`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31)),
            budgetPeriodForSchedule(BudgetCycleSchedule(16, 31), today),
        )
    }

    @Test fun `schedule period supports cycles crossing months`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 14)),
            budgetPeriodForSchedule(BudgetCycleSchedule(30, 14), today),
        )
    }

    @Test fun `next fortnight moves to following month`() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)

        assertEquals(
            DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)),
            budgetPeriodForView(config, BudgetPeriodView.NEXT, LocalDate.of(2026, 8, 20)),
        )
    }

    @Test fun `next fortnight becomes current and creates a new next period`() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)

        assertEquals(
            budgetPeriodForView(config, BudgetPeriodView.NEXT, LocalDate.of(2026, 8, 15)),
            budgetPeriodForView(config, BudgetPeriodView.CURRENT, LocalDate.of(2026, 8, 16)),
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)),
            budgetPeriodForView(config, BudgetPeriodView.NEXT, LocalDate.of(2026, 8, 16)),
        )
    }

    @Test fun `explicit view calculates available using only its movements`() {
        val config = BudgetConfig(2_500_000, BudgetPeriod.FORTNIGHTLY)
        val currentExpense = transaction(
            id = 1,
            date = LocalDate.of(2026, 8, 13),
            amountInCents = 500_000,
        )
        val nextExpense = transaction(
            id = 2,
            date = LocalDate.of(2026, 8, 16),
            amountInCents = 750_000,
        )

        assertEquals(
            1_750_000,
            availableForBudget(
                config,
                listOf(currentExpense, nextExpense),
                DateRange(LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 31)),
            ),
        )
    }

    @Test fun `transactions are filtered by the selected period dates`() {
        val config = BudgetConfig(100_000, BudgetPeriod.FORTNIGHTLY)
        val current = budgetPeriodForView(config, BudgetPeriodView.CURRENT, today)
        val next = budgetPeriodForView(config, BudgetPeriodView.NEXT, today)
        val currentTransaction = transaction(1, LocalDate.of(2026, 8, 13), 10_000)
        val nextTransaction = transaction(2, LocalDate.of(2026, 8, 16), 20_000)

        assertTrue(currentTransaction.belongsToActiveBudgetCycle(config, current))
        assertFalse(nextTransaction.belongsToActiveBudgetCycle(config, current))
        assertTrue(nextTransaction.belongsToActiveBudgetCycle(config, next))
    }

    @Test fun `creation boundary excludes earlier same day transactions`() {
        val boundary = Instant.parse("2026-08-01T12:00:00Z")
        val config = BudgetConfig(
            amountInCents = 100_000,
            period = BudgetPeriod.FORTNIGHTLY,
            cycleStart = LocalDate.of(2026, 8, 1),
            cycleStartedAt = boundary,
        )
        val period = activeBudgetPeriod(config, LocalDate.of(2026, 8, 1))

        assertFalse(
            transaction(1, LocalDate.of(2026, 8, 1), 10_000, Instant.parse("2026-08-01T11:59:59Z"))
                .belongsToActiveBudgetCycle(config, period),
        )
        assertTrue(
            transaction(2, LocalDate.of(2026, 8, 1), 10_000, Instant.parse("2026-08-01T12:00:01Z"))
                .belongsToActiveBudgetCycle(config, period),
        )
    }

    @Test fun `manual close is available only on inclusive closing day`() {
        val config = customScheduleConfig()

        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 28)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 29)))
        assertTrue(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 9, 14)))
        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 9, 15)))
    }

    @Test fun `cycle cannot close twice after next opening was created`() {
        val config = customScheduleConfig().copy(cycleStart = LocalDate.of(2026, 8, 30))

        assertFalse(canManuallyCloseBudgetCycle(config, LocalDate.of(2026, 8, 29)))
        assertFalse(shouldAutomaticallyCloseBudgetCycle(
            config,
            LocalDateTime.of(2026, 8, 29, 21, 0),
        ))
    }

    @Test fun `automatic close respects closing day and configured time`() {
        val config = customScheduleConfig()

        assertFalse(shouldAutomaticallyCloseBudgetCycle(
            config,
            LocalDateTime.of(2026, 8, 29, 14, 59),
            LocalTime.of(15, 0),
        ))
        assertTrue(shouldAutomaticallyCloseBudgetCycle(
            config,
            LocalDateTime.of(2026, 8, 29, 15, 0),
            LocalTime.of(15, 0),
        ))
        assertFalse(shouldAutomaticallyCloseBudgetCycle(
            config,
            LocalDateTime.of(2026, 8, 30, 15, 0),
            LocalTime.of(15, 0),
        ))
    }

    @Test fun `period to close keeps configured inclusive boundaries`() {
        val config = customScheduleConfig()

        assertEquals(
            DateRange(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 29)),
            budgetPeriodToClose(config, LocalDate.of(2026, 8, 29)),
        )
    }

    @Test fun `budget income description identifies its period`() {
        assertEquals("Ingreso quincenal", budgetIncomeDescription(BudgetPeriod.FORTNIGHTLY))
        assertEquals("Ingreso mensual", budgetIncomeDescription(BudgetPeriod.MONTHLY))
    }

    private fun customScheduleConfig() = BudgetConfig(
        amountInCents = 100_000,
        period = BudgetPeriod.FORTNIGHTLY,
        cycleSchedules = listOf(
            BudgetCycleSchedule(openingDay = 15, closingDay = 29),
            BudgetCycleSchedule(openingDay = 30, closingDay = 14),
        ),
    )

    private fun transaction(
        id: Long,
        date: LocalDate,
        amountInCents: Long,
        createdAt: Instant = Instant.parse("2026-08-13T12:00:00Z"),
    ) = FinanceTransaction(
        id = id,
        amountInCents = amountInCents,
        type = TransactionType.EXPENSE,
        categoryId = 1,
        description = null,
        date = date,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
