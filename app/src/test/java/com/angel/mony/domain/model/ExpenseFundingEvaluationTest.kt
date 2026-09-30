package com.angel.mony.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseFundingEvaluationTest {

    @Test
    fun case1_incomeEqualsExpenseNoOverflow() {
        // Ingresos = 5, Gastos previos = 0, Cobertura previa = 0, Nuevo gasto = 5
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 500_000,
            expenseAmountInCents = 500_000,
            previousFundingInCents = 0L,
        )

        assertEquals(0L, evaluation.overflowInCents)
        assertTrue(!evaluation.requiresFundingSource)
    }

    @Test
    fun case2_expenseExceedsIncomeOverflow() {
        // Ingresos = 5, Nuevo gasto = 6
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 500_000,
            expenseAmountInCents = 600_000,
            previousFundingInCents = 0L,
        )

        assertEquals(100_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun case3_income10PreviousExpense8NewExpense7Overflow5() {
        // Ingresos = 10, Gastos previos = 8, Nuevo gasto = 7
        // Available = 10 - 8 = 2, Gasto = 7, Overflow = 5
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 200_000,
            expenseAmountInCents = 700_000,
            previousFundingInCents = 0L,
        )

        assertEquals(500_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun case4_previousFundingConsideredNoDoubleCounting() {
        // Ingresos = 5, Gasto anterior = 6, Cobertura anterior = 1, Nuevo gasto = 2
        // Available = 5 + 1 - 6 = 0, Overflow = 2
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 0L,
            expenseAmountInCents = 200_000,
            previousFundingInCents = 100_000,
        )

        assertEquals(200_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun case5_availableZeroAfterPreviousFunding() {
        // Ingresos = 100, Cobertura previa = 20, Gastos = 120
        // Available = 0
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 0L,
            expenseAmountInCents = 1_000_000,
            previousFundingInCents = 2_000_000,
        )

        assertEquals(1_000_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }
}