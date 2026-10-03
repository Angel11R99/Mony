package com.angel.mony.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class FortnightPlanTest {
    private val now = Instant.parse("2026-10-01T12:00:00Z")

    private fun plan(
        budgetInCents: Long = 10_000L,
        status: FortnightPlanStatus = FortnightPlanStatus.OPEN,
    ) = FortnightPlan(
        id = 1,
        period = DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-15")),
        slot = FortnightSlot.FIRST,
        budgetInCents = budgetInCents,
        status = status,
        createdAt = now,
        closedAt = if (status == FortnightPlanStatus.CLOSED) now else null,
    )

    private fun item(
        id: Long,
        plannedInCents: Long,
        planId: Long = 1,
        type: FortnightItemType = FortnightItemType.EXPENSE,
        savingsGoalId: Long? = null,
        templateId: Long? = null,
    ) = FortnightPlanItem(
        id = id,
        planId = planId,
        templateId = templateId,
        description = "Item $id",
        plannedAmountInCents = plannedInCents,
        categoryId = 10L,
        type = type,
        savingsGoalId = savingsGoalId,
        position = 0,
        createdAt = now,
        updatedAt = now,
    )

    private fun payment(id: Long, itemId: Long, amountInCents: Long) = FortnightPayment(
        id = id,
        itemId = itemId,
        amountInCents = amountInCents,
        date = LocalDate.parse("2026-10-05"),
        transactionId = 100L + id,
        createdAt = now,
    )

    @Test
    fun itemWithoutPaymentsIsPending() {
        val details = FortnightPlanDetails(plan(), listOf(item(1, 2_000L)), emptyList())
        assertEquals(FortnightItemStatus.PENDING, details.itemProgress.single().status)
        assertEquals(2_000L, details.itemProgress.single().pendingInCents)
        assertEquals(0L, details.totalPaidInCents)
        assertEquals(8_000L, details.unassignedInCents)
        assertEquals(10_000L, details.remainingInCents)
        assertEquals(1, details.pendingItemCount)
    }

    @Test
    fun partialPaymentIsNotPaid() {
        val details = FortnightPlanDetails(plan(), listOf(item(1, 2_000L)), listOf(payment(1, 1, 500L)))
        assertEquals(FortnightItemStatus.PARTIAL, details.itemProgress.single().status)
        assertEquals(500L, details.itemProgress.single().paidInCents)
        assertEquals(1_500L, details.itemProgress.single().pendingInCents)
        assertTrue(details.hasPayments)
    }

    @Test
    fun completePaymentMarksItemAsPaid() {
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, 2_000L)),
            listOf(payment(1, 1, 1_200L), payment(2, 1, 800L)),
        )
        assertEquals(FortnightItemStatus.PAID, details.itemProgress.single().status)
        assertEquals(0L, details.itemProgress.single().pendingInCents)
        assertEquals(2_000L, details.totalPaidInCents)
        assertEquals(8_000L, details.remainingInCents)
    }

    @Test
    fun plannedMoneyIsNotPaidUntilPaymentExists() {
        val details = FortnightPlanDetails(plan(), listOf(item(1, 5_000L)), emptyList())
        assertEquals(5_000L, details.totalPlannedInCents)
        assertEquals(0L, details.totalPaidInCents)
        assertEquals(5_000L, details.totalPendingInCents)
        assertEquals(5_000L, details.unassignedInCents)
        assertEquals(10_000L, details.remainingInCents)
        assertFalse(details.isOverAssigned)
        assertFalse(details.isOverSpent)
    }

    @Test
    fun planningAboveBudgetReportsOverAssignment() {
        val details = FortnightPlanDetails(
            plan(5_000L),
            listOf(item(1, 3_000L), item(2, 4_000L)),
            emptyList(),
        )
        assertTrue(details.isOverAssigned)
        assertEquals(-2_000L, details.unassignedInCents)
    }

    @Test
    fun summaryUsesSameTotalsAsDetails() {
        val items = listOf(item(1, 3_000L), item(2, 2_000L))
        val payments = listOf(payment(1, 1, 1_000L))
        val details = FortnightPlanDetails(plan(), items, payments)
        val summary = fortnightPlanSummaries(listOf(plan()), items, payments).single()
        assertEquals(details.totalPlannedInCents, summary.totalPlannedInCents)
        assertEquals(details.totalPaidInCents, summary.totalPaidInCents)
        assertEquals(details.totalPendingInCents, summary.totalPendingInCents)
        assertEquals(details.items.size, summary.itemCount)
    }

    @Test
    fun paymentsAreNotMixedBetweenItems() {
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, 1_000L), item(2, 1_000L)),
            listOf(payment(1, 1, 400L)),
        )
        assertEquals(400L, details.paidInCents(1))
        assertEquals(0L, details.paidInCents(2))
        assertEquals(FortnightItemStatus.PARTIAL, details.itemProgress[0].status)
        assertEquals(FortnightItemStatus.PENDING, details.itemProgress[1].status)
    }

    @Test
    fun individualPaymentHistoryIsPreserved() {
        val payments = listOf(payment(1, 7, 100L), payment(2, 7, 250L), payment(3, 8, 900L))
        assertEquals(listOf(100L, 250L), payments.filter { it.itemId == 7L }.map { it.amountInCents })
    }

    @Test
    fun totalsRejectCentOverflow() {
        val details = FortnightPlanDetails(
            plan(Long.MAX_VALUE),
            listOf(item(1, Long.MAX_VALUE), item(2, Long.MAX_VALUE)),
            emptyList(),
        )
        assertFailsWith<ArithmeticException> { details.totalPlannedInCents }
    }

    @Test
    fun zeroOrNegativePaymentIsInvalid() {
        assertEquals(FortnightPaymentCheck.InvalidAmount, evaluateFortnightPayment(1_000L, 0L, 0L))
        assertEquals(FortnightPaymentCheck.InvalidAmount, evaluateFortnightPayment(1_000L, 0L, -50L))
    }

    @Test
    fun paymentWithinPendingAmountIsValid() {
        assertEquals(FortnightPaymentCheck.Valid, evaluateFortnightPayment(1_000L, 400L, 600L))
        assertEquals(FortnightPaymentCheck.Valid, evaluateFortnightPayment(1_000L, 0L, 1_000L))
    }

    @Test
    fun paymentAbovePendingAmountReportsOverpayment() {
        assertEquals(
            FortnightPaymentCheck.Overpayment(600L, 300L),
            evaluateFortnightPayment(1_000L, 400L, 900L),
        )
    }

    @Test
    fun savingsItemCreatesExpenseLinkedToGoal() {
        val transaction = item(1, 1_000L, type = FortnightItemType.SAVINGS, savingsGoalId = 55L)
            .toPaymentTransaction(1_000L, LocalDate.parse("2026-10-05"), now)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(1_000L, transaction.amountInCents)
        assertEquals(55L, transaction.savingsGoalId)
        assertEquals(null, transaction.fixedEntryId)
    }

    @Test
    fun expenseItemDoesNotLinkSavingsGoal() {
        val transaction = item(1, 1_000L)
            .toPaymentTransaction(500L, LocalDate.parse("2026-10-05"), now)
        assertEquals(null, transaction.savingsGoalId)
        assertEquals(500L, transaction.amountInCents)
    }

    @Test
    fun changingTemplateDoesNotAlterExistingPlan() {
        val template = FortnightTemplate(
            id = 1,
            description = "Luz",
            firstFortnightAmountInCents = 1_500L,
            categoryId = 10L,
            createdAt = now,
            updatedAt = now,
        )
        val updated = template.copy(firstFortnightAmountInCents = 9_999L)
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, template.firstFortnightAmountInCents!!, templateId = template.id)),
            emptyList(),
        )
        assertEquals(1_500L, details.items.single().plannedAmountInCents)
        assertEquals(9_999L, updated.firstFortnightAmountInCents)
    }

    @Test
    fun templateCanApplyToOnlyOneSlot() {
        val template = FortnightTemplate(
            description = "Internet",
            secondFortnightAmountInCents = 2_000L,
            categoryId = 10L,
            createdAt = now,
            updatedAt = now,
        )
        assertEquals(null, template.amountFor(FortnightSlot.FIRST))
        assertEquals(2_000L, template.amountFor(FortnightSlot.SECOND))
        assertFalse(template.appliesTo(FortnightSlot.FIRST))
        assertTrue(template.appliesTo(FortnightSlot.SECOND))
    }

    @Test
    fun templateWithoutAmountsIsInvalid() {
        assertFailsWith<IllegalArgumentException> {
            FortnightTemplate(description = "Vacía", categoryId = 10L, createdAt = now, updatedAt = now)
        }
    }

    @Test
    fun closedPlanKeepsClosingDate() {
        val closed = plan(status = FortnightPlanStatus.CLOSED)
        assertTrue(closed.isClosed)
        assertEquals(now, closed.closedAt)
    }

    @Test
    fun zeroBudgetPlanIsValid() {
        val details = FortnightPlanDetails(plan(0L), emptyList(), emptyList())
        assertEquals(0L, details.totalPlannedInCents)
        assertEquals(0L, details.totalPaidInCents)
        assertEquals(0L, details.remainingInCents)
        assertFalse(details.isOverAssigned)
    }
}
