package com.angel.mony.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FortnightPlanTest {
    private val now: Instant = Instant.parse("2026-10-01T12:00:00Z")

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

    private fun payment(
        id: Long,
        itemId: Long,
        amountInCents: Long,
    ) = FortnightPayment(
        id = id,
        itemId = itemId,
        amountInCents = amountInCents,
        date = LocalDate.parse("2026-10-05"),
        transactionId = 100L + id,
        createdAt = now,
    )

    @Test
    fun `item sin pagos queda pendiente`() {
        val details = FortnightPlanDetails(plan(), listOf(item(1, 2_000L)), emptyList())

        assertEquals(FortnightItemStatus.PENDING, details.itemProgress.single().status)
        assertEquals(2_000L, details.itemProgress.single().pendingInCents)
        assertEquals(0L, details.totalPaidInCents)
        assertEquals(8_000L, details.unassignedInCents)
        assertEquals(10_000L, details.remainingInCents)
        assertEquals(1, details.pendingItemCount)
    }

    @Test
    fun `pago parcial no equivale a pagado`() {
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, 2_000L)),
            listOf(payment(1, itemId = 1, amountInCents = 500L)),
        )

        val progress = details.itemProgress.single()
        assertEquals(FortnightItemStatus.PARTIAL, progress.status)
        assertEquals(500L, progress.paidInCents)
        assertEquals(1_500L, progress.pendingInCents)
        assertTrue(details.hasPayments)
    }

    @Test
    fun `pago que completa el monto marca pagado`() {
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, 2_000L)),
            listOf(
                payment(1, itemId = 1, amountInCents = 1_200L),
                payment(2, itemId = 1, amountInCents = 800L),
            ),
        )

        val progress = details.itemProgress.single()
        assertEquals(FortnightItemStatus.PAID, progress.status)
        assertEquals(0L, progress.pendingInCents)
        assertEquals(2_000L, details.totalPaidInCents)
        assertEquals(8_000L, details.remainingInCents)
    }

    @Test
    fun `los pagos nunca coinciden con lo planificado hasta que el usuario paga`() {
        val planned = FortnightPlanDetails(plan(), listOf(item(1, 5_000L)), emptyList())

        assertEquals(5_000L, planned.totalPlannedInCents)
        assertEquals(0L, planned.totalPaidInCents)
        assertEquals(5_000L, planned.totalPendingInCents)
        assertEquals(5_000L, planned.unassignedInCents)
        assertEquals(10_000L, planned.remainingInCents)
        assertFalse(planned.isOverAssigned)
        assertFalse(planned.isOverSpent)
    }

    @Test
    fun `planificar mas que el presupuesto se reporta como sobreasignacion`() {
        val details = FortnightPlanDetails(
            plan(budgetInCents = 5_000L),
            listOf(item(1, 3_000L), item(2, 4_000L)),
            emptyList(),
        )

        assertTrue(details.isOverAssigned)
        assertEquals(-2_000L, details.unassignedInCents)
    }

    @Test
    fun `los totales del plan resumen usan la misma matematica que el detalle`() {
        val items = listOf(item(1, 3_000L), item(2, 2_000L))
        val payments = listOf(payment(1, itemId = 1, amountInCents = 1_000L))

        val details = FortnightPlanDetails(plan(), items, payments)
        val summary = fortnightPlanSummaries(listOf(plan()), items, payments).single()

        assertEquals(details.totalPlannedInCents, summary.totalPlannedInCents)
        assertEquals(details.totalPaidInCents, summary.totalPaidInCents)
        assertEquals(details.totalPendingInCents, summary.totalPendingInCents)
        assertEquals(details.items.size, summary.itemCount)
    }

    @Test
    fun `los pagos no se mezclan entre items`() {
        val details = FortnightPlanDetails(
            plan(),
            listOf(item(1, 1_000L), item(2, 1_000L)),
            listOf(payment(1, itemId = 1, amountInCents = 400L)),
        )

        assertEquals(400L, details.paidInCents(1))
        assertEquals(0L, details.paidInCents(2))
        assertEquals(FortnightItemStatus.PARTIAL, details.itemProgress[0].status)
        assertEquals(FortnightItemStatus.PENDING, details.itemProgress[1].status)
    }

    @Test
    fun `el resumen de pagos por item mantiene el historial individual`() {
        val payments = listOf(
            payment(1, itemId = 7, amountInCents = 100L),
            payment(2, itemId = 7, amountInCents = 250L),
            payment(3, itemId = 8, amountInCents = 900L),
        )

        assertEquals(listOf(100L, 250L), payments.filter { it.itemId == 7L }.map { it.amountInCents })
    }

    @Test
    fun `los totales se protegen contra desbordamiento de centavos`() {
        val details = FortnightPlanDetails(
            plan(budgetInCents = Long.MAX_VALUE),
            listOf(item(1, Long.MAX_VALUE), item(2, Long.MAX_VALUE)),
            emptyList(),
        )

        val overflow = runCatching { details.totalPlannedInCents }
        assertTrue(overflow.isFailure)
        assertTrue(overflow.exceptionOrNull() is ArithmeticException)
    }

    @Test
    fun `un abono de cero es invalido`() {
        assertEquals(
            FortnightPaymentCheck.InvalidAmount,
            evaluateFortnightPayment(plannedInCents = 1_000L, alreadyPaidInCents = 0L, amountInCents = 0L),
        )
        assertEquals(
            FortnightPaymentCheck.InvalidAmount,
            evaluateFortnightPayment(plannedInCents = 1_000L, alreadyPaidInCents = 0L, amountInCents = -50L),
        )
    }

    @Test
    fun `un abono dentro del pendiente es valido`() {
        assertEquals(
            FortnightPaymentCheck.Valid,
            evaluateFortnightPayment(plannedInCents = 1_000L, alreadyPaidInCents = 400L, amountInCents = 600L),
        )
        assertEquals(
            FortnightPaymentCheck.Valid,
            evaluateFortnightPayment(plannedInCents = 1_000L, alreadyPaidInCents = 0L, amountInCents = 1_000L),
        )
    }

    @Test
    fun `un abono que excede el pendiente se reporta como sobrepago`() {
        val check = evaluateFortnightPayment(
            plannedInCents = 1_000L,
            alreadyPaidInCents = 400L,
            amountInCents = 900L,
        )

        assertEquals(FortnightPaymentCheck.Overpayment(pendingInCents = 600L, excessInCents = 300L), check)
    }

    @Test
    fun `un item de ahorro genera un gasto enlazado a la meta`() {
        val transaction = item(
            id = 1,
            plannedInCents = 1_000L,
            type = FortnightItemType.SAVINGS,
            savingsGoalId = 55L,
        ).toPaymentTransaction(
            amountInCents = 1_000L,
            date = LocalDate.parse("2026-10-05"),
            now = now,
        )

        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(1_000L, transaction.amountInCents)
        assertEquals(55L, transaction.savingsGoalId)
        assertEquals(null, transaction.fixedEntryId)
    }

    @Test
    fun `un item de gasto no toca la meta de ahorro`() {
        val transaction = item(id = 1, plannedInCents = 1_000L, savingsGoalId = null)
            .toPaymentTransaction(amountInCents = 500L, date = LocalDate.parse("2026-10-05"), now = now)

        assertEquals(null, transaction.savingsGoalId)
        assertEquals(500L, transaction.amountInCents)
    }

    @Test
    fun `cambiar una plantilla no altera los planes ya existentes`() {
        val template = FortnightTemplate(
            id = 1,
            description = "Luz",
            firstFortnightAmountInCents = 1_500L,
            categoryId = 10L,
            createdAt = now,
            updatedAt = now,
        )
        val updatedTemplate = template.copy(firstFortnightAmountInCents = 9_999L, updatedAt = now)

        val items = listOf(item(1, template.firstFortnightAmountInCents!!, templateId = template.id))
        val details = FortnightPlanDetails(plan(), items, emptyList())

        assertEquals(1_500L, details.items.single().plannedAmountInCents)
        assertEquals(9_999L, updatedTemplate.firstFortnightAmountInCents)
    }

    @Test
    fun `una plantilla puede aplicar solo a una de las dos quincenas`() {
        val onlySecond = FortnightTemplate(
            description = "Internet",
            secondFortnightAmountInCents = 2_000L,
            categoryId = 10L,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(null, onlySecond.amountFor(FortnightSlot.FIRST))
        assertEquals(2_000L, onlySecond.amountFor(FortnightSlot.SECOND))
        assertFalse(onlySecond.appliesTo(FortnightSlot.FIRST))
        assertTrue(onlySecond.appliesTo(FortnightSlot.SECOND))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `una plantilla sin ningun monto es invalida`() {
        FortnightTemplate(
            description = "Vacia",
            categoryId = 10L,
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test
    fun `un plan cerrado conserva su fecha de cierre`() {
        val closed = plan(status = FortnightPlanStatus.CLOSED)

        assertTrue(closed.isClosed)
        assertEquals(now, closed.closedAt)
    }

    @Test
    fun `un plan con presupuesto cero es valido`() {
        val details = FortnightPlanDetails(plan(budgetInCents = 0L), emptyList(), emptyList())

        assertEquals(0L, details.totalPlannedInCents)
        assertEquals(0L, details.totalPaidInCents)
        assertEquals(0L, details.remainingInCents)
        assertFalse(details.isOverAssigned)
    }
}
