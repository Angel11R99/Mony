package com.angel.mony.domain.repository

import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FortnightPlan
import com.angel.mony.domain.model.FortnightPlanDetails
import com.angel.mony.domain.model.FortnightPlanItem
import com.angel.mony.domain.model.FortnightPlanSummary
import com.angel.mony.domain.model.FortnightTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

sealed interface FortnightMutationResult {
    data class Success(val id: Long) : FortnightMutationResult
    data object NotFound : FortnightMutationResult
    data object ClosedPlan : FortnightMutationResult
    data object HasPayments : FortnightMutationResult
    data object TemplateInUse : FortnightMutationResult
}

sealed interface FortnightPaymentResult {
    data class Registered(
        val paymentId: Long,
        val transactionId: Long,
        val details: FortnightPlanDetails,
    ) : FortnightPaymentResult

    data class Overpayment(
        val pendingInCents: Long,
        val excessInCents: Long,
    ) : FortnightPaymentResult

    data object InvalidAmount : FortnightPaymentResult
    data object NotFound : FortnightPaymentResult
    data object ClosedPlan : FortnightPaymentResult
    data object InvalidCategory : FortnightPaymentResult

    data class RequiresFundingSource(
        val overflowInCents: Long,
        val availableBeforeExpenseInCents: Long,
        val expenseAmountInCents: Long,
        val transaction: FinanceTransaction,
        val itemId: Long,
        val amountInCents: Long,
        val date: LocalDate,
        val allowOverpayment: Boolean,
    ) : FortnightPaymentResult

    data class Error(val message: String) : FortnightPaymentResult
}

interface FortnightRepository {
    fun observeTemplates(): Flow<List<FortnightTemplate>>
    fun observeActiveTemplates(): Flow<List<FortnightTemplate>>
    suspend fun saveTemplate(template: FortnightTemplate): FortnightMutationResult
    suspend fun setTemplateActive(id: Long, isActive: Boolean): FortnightMutationResult
    suspend fun deleteTemplate(id: Long): FortnightMutationResult

    fun observePlanSummaries(): Flow<List<FortnightPlanSummary>>
    fun observeDetails(planId: Long): Flow<FortnightPlanDetails?>
    suspend fun getDetails(planId: Long): FortnightPlanDetails?
    suspend fun findPlanForPeriod(start: LocalDate, endInclusive: LocalDate): FortnightPlan?
    suspend fun createPlan(plan: FortnightPlan, items: List<FortnightPlanItem>): Long
    suspend fun saveItem(item: FortnightPlanItem): FortnightMutationResult
    suspend fun deleteItem(itemId: Long): FortnightMutationResult
    suspend fun registerPayment(
        itemId: Long,
        amountInCents: Long,
        date: LocalDate,
        allowOverpayment: Boolean = false,
    ): FortnightPaymentResult
    suspend fun registerPaymentWithFunding(
        itemId: Long,
        amountInCents: Long,
        date: LocalDate,
        allowOverpayment: Boolean,
        fundingSourceDescription: String,
    ): FortnightPaymentResult
    suspend fun deletePayment(paymentId: Long): FortnightMutationResult
    suspend fun closePlan(planId: Long): FortnightMutationResult
    suspend fun reopenPlan(planId: Long): FortnightMutationResult
    suspend fun deletePlan(planId: Long): FortnightMutationResult
}
