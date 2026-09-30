package com.angel.mony.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Dominio de planificación quincenal.
 *
 * Regla central: PLANIFICADO != PAGADO != PENDIENTE.
 * Un monto planificado nunca es un movimiento financiero; solo los abonos
 * registrados mediante [FortnightPayment] generan [FinanceTransaction].
 */
enum class FortnightSlot {
    FIRST,
    SECOND,
}

fun FortnightSlot.label(): String = when (this) {
    FortnightSlot.FIRST -> "1ra quincena"
    FortnightSlot.SECOND -> "2da quincena"
}

fun FortnightSlot.cycleLabel(): String = when (this) {
    FortnightSlot.FIRST -> "1.er ciclo"
    FortnightSlot.SECOND -> "2.º ciclo"
}

fun cycleLabelForSlot(slot: FortnightSlot): String = when (slot) {
    FortnightSlot.FIRST -> "1.er ciclo"
    FortnightSlot.SECOND -> "2.º ciclo"
}

enum class FortnightItemType {
    EXPENSE,
    SAVINGS,
}

fun FortnightItemType.label(): String = when (this) {
    FortnightItemType.EXPENSE -> "Gasto"
    FortnightItemType.SAVINGS -> "Ahorro"
}

enum class FortnightItemStatus {
    PENDING,
    PARTIAL,
    PAID,
}

fun FortnightItemStatus.label(): String = when (this) {
    FortnightItemStatus.PENDING -> "Pendiente"
    FortnightItemStatus.PARTIAL -> "Parcial"
    FortnightItemStatus.PAID -> "Pagado"
}

enum class FortnightPlanStatus {
    OPEN,
    CLOSED,
}

fun FortnightPlanStatus.label(): String = when (this) {
    FortnightPlanStatus.OPEN -> "Abierta"
    FortnightPlanStatus.CLOSED -> "Cerrada"
}

/**
 * Estado derivado de un item. El estado nunca se persiste: se recalcula
 * siempre desde lo planificado y lo realmente pagado.
 */
fun fortnightItemStatus(plannedInCents: Long, paidInCents: Long): FortnightItemStatus = when {
    paidInCents <= 0L -> FortnightItemStatus.PENDING
    plannedInCents <= paidInCents -> FortnightItemStatus.PAID
    else -> FortnightItemStatus.PARTIAL
}

fun fortnightPendingInCents(plannedInCents: Long, paidInCents: Long): Long =
    if (plannedInCents <= paidInCents) 0L else plannedInCents - paidInCents

/**
 * Plantilla reutilizable. Solo describe la intsención de gasto; no contiene
 * pagos, por lo que modificarla nunca altera planes ya creados.
 */
data class FortnightTemplate(
    val id: Long = 0,
    val description: String,
    val firstFortnightAmountInCents: Long? = null,
    val secondFortnightAmountInCents: Long? = null,
    val categoryId: Long,
    val type: FortnightItemType = FortnightItemType.EXPENSE,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(description.isNotBlank()) { "Template description cannot be blank" }
        require(firstFortnightAmountInCents == null || firstFortnightAmountInCents >= 0) {
            "First fortnight amount cannot be negative"
        }
        require(secondFortnightAmountInCents == null || secondFortnightAmountInCents >= 0) {
            "Second fortnight amount cannot be negative"
        }
        require(firstFortnightAmountInCents != null || secondFortnightAmountInCents != null) {
            "A template must apply to at least one fortnight"
        }
    }

    fun amountFor(slot: FortnightSlot): Long? = when (slot) {
        FortnightSlot.FIRST -> firstFortnightAmountInCents
        FortnightSlot.SECOND -> secondFortnightAmountInCents
    }

    fun appliesTo(slot: FortnightSlot): Boolean = amountFor(slot) != null
}

data class FortnightPlan(
    val id: Long = 0,
    val period: DateRange,
    val slot: FortnightSlot,
    val budgetInCents: Long,
    val status: FortnightPlanStatus = FortnightPlanStatus.OPEN,
    val createdAt: Instant,
    val closedAt: Instant? = null,
) {
    init {
        require(budgetInCents >= 0) { "Fortnight budget cannot be negative" }
        require(status != FortnightPlanStatus.CLOSED || closedAt != null) {
            "A closed fortnight plan needs a closing date"
        }
    }

    val isClosed: Boolean get() = status == FortnightPlanStatus.CLOSED
}

/**
 * Item del plan. Es un snapshot: aunque cambie o se elimine la plantilla de
 * origen, los montos planificados del período ya creado se conservan.
 */
data class FortnightPlanItem(
    val id: Long = 0,
    val planId: Long,
    val templateId: Long? = null,
    val description: String,
    val plannedAmountInCents: Long,
    val categoryId: Long,
    val type: FortnightItemType = FortnightItemType.EXPENSE,
    val savingsGoalId: Long? = null,
    val note: String? = null,
    val position: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(description.isNotBlank()) { "Fortnight item description cannot be blank" }
        require(plannedAmountInCents >= 0) { "Fortnight item amount cannot be negative" }
        require(position >= 0) { "Fortnight item position cannot be negative" }
    }
}

/** Abono real de un item. Es el único origen del dinero pagado del plan. */
data class FortnightPayment(
    val id: Long = 0,
    val itemId: Long,
    val amountInCents: Long,
    val date: LocalDate,
    val transactionId: Long? = null,
    val createdAt: Instant,
) {
    init {
        require(amountInCents > 0) { "Fortnight payment amount must be greater than zero" }
    }
}

data class FortnightItemProgress(
    val item: FortnightPlanItem,
    val paidInCents: Long,
    val pendingInCents: Long,
    val status: FortnightItemStatus,
)

/**
 * Detalle completo de un período con todos los totales derivados.
 * No contiene datos mutables de UI.
 */
data class FortnightPlanDetails(
    val plan: FortnightPlan,
    val items: List<FortnightPlanItem>,
    val payments: List<FortnightPayment>,
) {
    private val paymentsByItem: Map<Long, List<FortnightPayment>> =
        payments.groupBy(FortnightPayment::itemId)

    val itemProgress: List<FortnightItemProgress> = items.map { item ->
        val paid = paidInCents(item.id)
        FortnightItemProgress(
            item = item,
            paidInCents = paid,
            pendingInCents = fortnightPendingInCents(item.plannedAmountInCents, paid),
            status = fortnightItemStatus(item.plannedAmountInCents, paid),
        )
    }

    fun paymentsFor(itemId: Long): List<FortnightPayment> = paymentsByItem[itemId].orEmpty()

    fun paidInCents(itemId: Long): Long = paymentsFor(itemId).moneySumOf { it.amountInCents }

    val totalPlannedInCents: Long get() = items.moneySumOf { it.plannedAmountInCents }

    val totalPaidInCents: Long get() = payments.moneySumOf { it.amountInCents }

    val totalPendingInCents: Long get() = itemProgress.moneySumOf { it.pendingInCents }

    /** Dinero del presupuesto que todavía no fue asignado a ningún item. */
    val unassignedInCents: Long
        get() = Math.subtractExact(plan.budgetInCents, totalPlannedInCents)

    /** Dinero del presupuesto que aún no se ha pagado realmente. */
    val remainingInCents: Long get() = Math.subtractExact(plan.budgetInCents, totalPaidInCents)

    val pendingItemCount: Int get() = itemProgress.count { it.status != FortnightItemStatus.PAID }

    val paidItemCount: Int get() = itemProgress.count { it.status == FortnightItemStatus.PAID }

    val isOverAssigned: Boolean get() = unassignedInCents < 0

    val isOverSpent: Boolean get() = remainingInCents < 0

    val hasPayments: Boolean get() = payments.isNotEmpty()
}

/** Fila compacta del historial: mismo cálculo de totales que el detalle. */
data class FortnightPlanSummary(
    val plan: FortnightPlan,
    val totalPlannedInCents: Long,
    val totalPaidInCents: Long,
    val totalPendingInCents: Long,
    val itemCount: Int,
)

/**
 * Construye las filas del historial reutilizando exactamente la matemática de
 * [FortnightPlanDetails], evitando reglas duplicadas entre dashboard y lista.
 */
fun fortnightPlanSummaries(
    plans: List<FortnightPlan>,
    items: List<FortnightPlanItem>,
    payments: List<FortnightPayment>,
): List<FortnightPlanSummary> {
    val itemsByPlan = items.groupBy(FortnightPlanItem::planId)
    val paymentsByItem = payments.groupBy(FortnightPayment::itemId)
    return plans.map { plan ->
        val planItems = itemsByPlan[plan.id].orEmpty()
        val progress = planItems.map { item ->
            val paid = paymentsByItem[item.id].orEmpty().moneySumOf { it.amountInCents }
            item.plannedAmountInCents to fortnightPendingInCents(item.plannedAmountInCents, paid)
        }
        FortnightPlanSummary(
            plan = plan,
            totalPlannedInCents = planItems.moneySumOf { it.plannedAmountInCents },
            totalPaidInCents = planItems.flatMap { paymentsByItem[it.id].orEmpty() }.moneySumOf { it.amountInCents },
            totalPendingInCents = progress.moneySumOf { it.second },
            itemCount = planItems.size,
        )
    }
}

/**
 * Único punto donde un abono quincenal se convierte en movimiento financiero.
 * El ahorro se registra como gasto enlazado a la meta para que el progreso de
 * Savings se calcule automáticamente sin duplicar reglas.
 */
fun FortnightPlanItem.toPaymentTransaction(
    amountInCents: Long,
    date: LocalDate,
    now: Instant,
): FinanceTransaction = FinanceTransaction(
    amountInCents = amountInCents,
    type = TransactionType.EXPENSE,
    categoryId = categoryId,
    description = description,
    date = date,
    createdAt = now,
    updatedAt = now,
    fixedEntryId = null,
    savingsGoalId = if (type == FortnightItemType.SAVINGS) savingsGoalId else null,
)

/** Resultado de validar un abono antes de tocar la base de datos. */
sealed interface FortnightPaymentCheck {
    data object Valid : FortnightPaymentCheck
    data object InvalidAmount : FortnightPaymentCheck
    data class Overpayment(
        val pendingInCents: Long,
        val excessInCents: Long,
    ) : FortnightPaymentCheck
}

fun evaluateFortnightPayment(
    plannedInCents: Long,
    alreadyPaidInCents: Long,
    amountInCents: Long,
): FortnightPaymentCheck {
    if (amountInCents <= 0L) return FortnightPaymentCheck.InvalidAmount
    val pending = fortnightPendingInCents(plannedInCents, alreadyPaidInCents)
    if (amountInCents > pending) {
        return FortnightPaymentCheck.Overpayment(
            pendingInCents = pending,
            excessInCents = Math.subtractExact(amountInCents, pending),
        )
    }
    return FortnightPaymentCheck.Valid
}

private inline fun <T> Iterable<T>.moneySumOf(selector: (T) -> Long): Long {
    var total = 0L
    for (element in this) total = Math.addExact(total, selector(element))
    return total
}
