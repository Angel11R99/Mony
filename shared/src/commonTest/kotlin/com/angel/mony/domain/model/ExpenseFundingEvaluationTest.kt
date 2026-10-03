package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpenseFundingEvaluationTest {
    @Test
    fun incomeEqualsExpenseHasNoOverflow() {
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 500_000,
            expenseAmountInCents = 500_000,
            previousFundingInCents = 0L,
        )

        assertEquals(0L, evaluation.overflowInCents)
        assertTrue(!evaluation.requiresFundingSource)
    }

    @Test
    fun expenseExceedingIncomeReportsOverflow() {
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 500_000,
            expenseAmountInCents = 600_000,
            previousFundingInCents = 0L,
        )

        assertEquals(100_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun previousExpensesReduceAvailableAmount() {
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 200_000,
            expenseAmountInCents = 700_000,
            previousFundingInCents = 0L,
        )

        assertEquals(500_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun previousFundingIsNotCountedTwice() {
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 0L,
            expenseAmountInCents = 200_000,
            previousFundingInCents = 100_000,
        )

        assertEquals(200_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }

    @Test
    fun zeroAvailableAmountRequiresFullFunding() {
        val evaluation = ExpenseFundingEvaluation(
            availableBeforeExpenseInCents = 0L,
            expenseAmountInCents = 1_000_000,
            previousFundingInCents = 2_000_000,
        )

        assertEquals(1_000_000, evaluation.overflowInCents)
        assertTrue(evaluation.requiresFundingSource)
    }
}
