package com.angel.mony.domain.usecase

import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.defaultCycleSchedules
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class SaveBudget @Inject constructor(
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(amountInCents: Long, period: BudgetPeriod) {
        require(amountInCents > 0) { "El monto debe ser mayor que cero" }

        val existing = budgets.observe().first()
        val incomeCategories = categories.observeActive(TransactionType.INCOME).first()
        val categoryId = incomeCategories.firstOrNull { it.name.equals("Salario", ignoreCase = true) }?.id
            ?: incomeCategories.firstOrNull()?.id
            ?: error("No hay una categoría de ingreso disponible")
        val now = Instant.now()
        val existingIncome = existing?.incomeTransactionId?.let { transactions.get(it) }
        val income = FinanceTransaction(
            id = existingIncome?.id ?: 0,
            amountInCents = amountInCents,
            type = TransactionType.INCOME,
            categoryId = existingIncome?.categoryId ?: categoryId,
            description = if (period == BudgetPeriod.MONTHLY) "Ingreso mensual" else "Ingreso quincenal",
            date = existingIncome?.date ?: LocalDate.now(),
            createdAt = existingIncome?.createdAt ?: now,
            updatedAt = now,
        )
        val incomeTransactionId = if (existingIncome == null) {
            transactions.create(income)
        } else {
            transactions.update(income)
            income.id
        }

        budgets.save(
            BudgetConfig(
                amountInCents = amountInCents,
                period = period,
                cycleStart = existing?.cycleStart,
                cycleStartedAt = existing?.cycleStartedAt,
                incomeTransactionId = incomeTransactionId,
                cycleSchedules = if (existing == null || existing.period != period) {
                    defaultCycleSchedules(period)
                } else {
                    existing.cycleSchedules
                },
            )
        )
    }
}
