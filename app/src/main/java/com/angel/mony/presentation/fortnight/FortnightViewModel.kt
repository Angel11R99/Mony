package com.angel.mony.presentation.fortnight

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.DateRange
import com.angel.mony.domain.model.FortnightItemType
import com.angel.mony.domain.model.FortnightPeriodStyle
import com.angel.mony.domain.model.FortnightPlan
import com.angel.mony.domain.model.FortnightPlanDetails
import com.angel.mony.domain.model.FortnightPlanItem
import com.angel.mony.domain.model.FortnightPlanStatus
import com.angel.mony.domain.model.FortnightSlot
import com.angel.mony.domain.model.FortnightTemplate
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.fortnightPeriodContaining
import com.angel.mony.domain.model.fortnightPeriodStyle
import com.angel.mony.domain.model.fortnightSlotFor
import com.angel.mony.domain.model.nextFortnightPeriod
import com.angel.mony.domain.model.previousFortnightPeriod
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.FortnightMutationResult
import com.angel.mony.domain.repository.FortnightPaymentResult
import com.angel.mony.domain.repository.FortnightRepository
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FortnightUiState(
    val period: DateRange = DateRange.currentFortnight(),
    val slot: FortnightSlot = FortnightSlot.FIRST,
    val budget: BudgetConfig? = null,
    val details: FortnightPlanDetails? = null,
    val categories: List<Category> = emptyList(),
    val savingsGoals: List<SavingsGoalProgress> = emptyList(),
    val templates: List<FortnightTemplate> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val hasPlan: Boolean get() = details != null
    val isClosed: Boolean get() = details?.plan?.isClosed == true
    val canEditItems: Boolean get() = hasPlan && !isClosed
    val openSavingsGoals: List<SavingsGoalProgress> get() = savingsGoals.filter { !it.isCompleted }

    /** Plantillas activas que aplican al período visible. */
    val applicableTemplates: List<FortnightTemplate>
        get() = templates.filter { it.appliesTo(slot) }
}

/** Item en edición dentro de los sheets de alta/edición. */
data class FortnightItemDraft(
    val itemId: Long? = null,
    val description: String = "",
    val amountText: String = "",
    val categoryId: Long? = null,
    val type: FortnightItemType = FortnightItemType.EXPENSE,
    val savingsGoalId: Long? = null,
    val note: String = "",
) {
    val isEditing: Boolean get() = itemId != null
    val amountInCents: Long? get() = MoneyFormatter.parseToCents(amountText)
}

/** Abono en curso, pendiente de confirmación. */
data class FortnightPaymentDraft(
    val itemId: Long,
    val itemDescription: String,
    val amountText: String,
    val date: LocalDate = LocalDate.now(),
) {
    val amountInCents: Long? get() = MoneyFormatter.parseToCents(amountText)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FortnightViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: FortnightRepository,
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    savingsRepository: SavingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialPeriod: DateRange? = savedStateHandle.get<String>(PERIOD_START_KEY)?.let { start ->
        savedStateHandle.get<String>(PERIOD_END_KEY)?.let { end ->
            runCatching { DateRange(LocalDate.parse(start), LocalDate.parse(end)) }.getOrNull()
        }
    }

    private val selectedPeriod = MutableStateFlow(
        initialPeriod ?: fortnightPeriodContaining(LocalDate.now(), null),
    )

    val state: StateFlow<FortnightUiState> = run {
        val periodWithPlan = combine(
            selectedPeriod,
            repository.observePlanSummaries(),
        ) { period, summaries ->
            period to summaries.firstOrNull { it.plan.period == period }?.plan?.id
        }
        val details = periodWithPlan.flatMapLatest { (_, planId) ->
            planId?.let { repository.observeDetails(it) } ?: flowOf(null)
        }
        val pickers = combine(
            categoryRepository.observeActive(TransactionType.EXPENSE),
            savingsRepository.observeGoals(),
        ) { categories, goals -> categories to goals }

        combine(
            budgetRepository.observe(),
            periodWithPlan,
            details,
            pickers,
            repository.observeActiveTemplates(),
        ) { budget, (period, _), details, (categories, goals), templates ->
            FortnightUiState(
                period = period,
                slot = fortnightSlotFor(period, budget),
                budget = budget,
                details = details,
                categories = categories,
                savingsGoals = goals,
                templates = templates,
                isLoading = false,
            )
        }
    }
        .catch { emit(FortnightUiState(isLoading = false, hasError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FortnightUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)

    private val mutableCreatingPlan = MutableStateFlow(false)
    val isCreatingPlanVisible: StateFlow<Boolean> = mutableCreatingPlan
    val planBudgetText = MutableStateFlow("")

    private val mutableItemDraft = MutableStateFlow<FortnightItemDraft?>(null)
    val itemDraft: StateFlow<FortnightItemDraft?> = mutableItemDraft

    private val mutablePaymentDraft = MutableStateFlow<FortnightPaymentDraft?>(null)
    val paymentDraft: StateFlow<FortnightPaymentDraft?> = mutablePaymentDraft

    private val mutableOverpayment = MutableStateFlow<FortnightPaymentDraft?>(null)
    val overpaymentDraft: StateFlow<FortnightPaymentDraft?> = mutableOverpayment

    private val mutablePendingClose = MutableStateFlow(false)
    val isCloseConfirmVisible: StateFlow<Boolean> = mutablePendingClose

    private val mutablePendingDeletePlan = MutableStateFlow(false)
    val isDeletePlanConfirmVisible: StateFlow<Boolean> = mutablePendingDeletePlan

    private val mutablePendingDeleteItem = MutableStateFlow<FortnightPlanItem?>(null)
    val pendingItemDelete: StateFlow<FortnightPlanItem?> = mutablePendingDeleteItem

    private val mutablePendingDeletePayment = MutableStateFlow<Long?>(null)
    val pendingPaymentDelete: StateFlow<Long?> = mutablePendingDeletePayment

    fun consumeMessage() { message.value = null }

    private val monthlyMode: Boolean
        get() = fortnightPeriodStyle(state.value.budget) == FortnightPeriodStyle.MONTHLY

    private fun periodAlreadyHasAPlan(): String =
        if (monthlyMode) "Este mes ya tiene un plan." else "Esta quincena ya tiene un plan."

    private fun periodIsClosed(): String =
        if (monthlyMode) "El mes está cerrado." else "La quincena está cerrada."

    private fun periodCreated(): String =
        if (monthlyMode) "Plan del mes creado." else "Plan de la quincena creado."

    private fun periodClosed(): String =
        if (monthlyMode) "Mes cerrado." else "Quincena cerrada."

    private fun periodReopened(): String =
        if (monthlyMode) "Mes reabierto." else "Quincena reabierta."

    private fun periodUnavailable(): String =
        if (monthlyMode) "El mes ya no está disponible." else "La quincena ya no está disponible."

    private fun couldNotClosePeriod(): String =
        if (monthlyMode) "No se pudo cerrar el mes." else "No se pudo cerrar la quincena."

    private fun couldNotReopenPeriod(): String =
        if (monthlyMode) "No se pudo reabrir el mes." else "No se pudo reabrir la quincena."

    private fun reopenPeriodBeforeDelete(): String =
        if (monthlyMode) "Reabre el mes antes de eliminarlo." else "Reabre la quincena antes de eliminarla."

    fun showCreatePlan() {
        if (state.value.hasPlan) {
            message.value = periodAlreadyHasAPlan()
            return
        }
        val suggested = state.value.budget?.amountInCents
        planBudgetText.value = suggested?.let(MoneyFormatter::formatToInput).orEmpty()
        mutableCreatingPlan.value = true
    }

    fun hideCreatePlan() { mutableCreatingPlan.value = false }

    fun updatePlanBudget(text: String) { planBudgetText.value = text }

    fun createPlan(selectedTemplateIds: Set<Long>, categoryAmounts: Map<Long, String>) {
        if (isSaving.value) return
        val uiState = state.value
        if (uiState.hasPlan) {
            message.value = periodAlreadyHasAPlan()
            mutableCreatingPlan.value = false
            return
        }
        val budget = MoneyFormatter.parseToCents(planBudgetText.value)
        if (planBudgetText.value.isBlank()) {
            message.value = if (monthlyMode) "Indica el presupuesto del mes." else "Indica el presupuesto de la quincena."
            return
        }
        if (budget == null || budget < 0) {
            message.value = "El presupuesto debe ser mayor que cero."
            return
        }
        val now = Instant.now()
        val fromTemplates = uiState.templates
            .filter { it.id in selectedTemplateIds && it.appliesTo(uiState.slot) }
            .map { template ->
                FortnightPlanItem(
                    planId = 0,
                    templateId = template.id,
                    description = template.description,
                    plannedAmountInCents = template.amountFor(uiState.slot) ?: 0L,
                    categoryId = template.categoryId,
                    type = template.type,
                    note = template.note,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        val fromCategories = uiState.categories.mapNotNull { category ->
            val raw = categoryAmounts[category.id]
            val amount = raw?.takeIf { it.isNotBlank() }?.let(MoneyFormatter::parseToCents)
            if (amount == null) null else FortnightPlanItem(
                planId = 0,
                description = category.name,
                plannedAmountInCents = amount,
                categoryId = category.id,
                type = FortnightItemType.EXPENSE,
                createdAt = now,
                updatedAt = now,
            )
        }
        val items = fromTemplates + fromCategories
        if (items.isEmpty()) {
            message.value = "Selecciona al menos una categoría o plantilla antes de continuar."
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            runCatching {
                repository.createPlan(
                    plan = FortnightPlan(
                        period = uiState.period,
                        slot = uiState.slot,
                        budgetInCents = budget,
                        status = FortnightPlanStatus.OPEN,
                        createdAt = now,
                    ),
                    items = items,
                )
            }.onSuccess {
                message.value = periodCreated()
                mutableCreatingPlan.value = false
            }.onFailure {
                message.value = "No se pudo crear el plan."
            }
            isSaving.value = false
        }
    }

    fun goToNextPeriod() {
        selectPeriod(nextFortnightPeriod(selectedPeriod.value, state.value.budget))
    }

    fun goToPreviousPeriod() {
        selectPeriod(previousFortnightPeriod(selectedPeriod.value, state.value.budget))
    }

    fun goToCurrentPeriod() {
        selectPeriod(fortnightPeriodContaining(LocalDate.now(), state.value.budget))
    }

    private fun selectPeriod(period: DateRange) {
        selectedPeriod.value = period
        savedStateHandle[PERIOD_START_KEY] = period.start.toString()
        savedStateHandle[PERIOD_END_KEY] = period.endInclusive.toString()
    }

    fun startAddItem() {
        if (!state.value.canEditItems) return
        mutableItemDraft.value = FortnightItemDraft(categoryId = state.value.categories.firstOrNull()?.id)
    }

    fun startEditItem(item: FortnightPlanItem) {
        if (!state.value.canEditItems) return
        mutableItemDraft.value = FortnightItemDraft(
            itemId = item.id,
            description = item.description,
            amountText = MoneyFormatter.formatToInput(item.plannedAmountInCents),
            categoryId = item.categoryId,
            type = item.type,
            savingsGoalId = item.savingsGoalId,
            note = item.note.orEmpty(),
        )
    }

    fun updateItemDraft(update: (FortnightItemDraft) -> FortnightItemDraft) {
        mutableItemDraft.value = mutableItemDraft.value?.let(update)
    }

    fun cancelItemDraft() { mutableItemDraft.value = null }

    fun saveItem() {
        if (isSaving.value) return
        val uiState = state.value
        val draft = mutableItemDraft.value ?: return
        if (!uiState.canEditItems) {
            message.value = periodIsClosed()
            cancelItemDraft()
            return
        }
        val description = draft.description.trim()
        val amount = draft.amountInCents
        val categoryId = draft.categoryId
        when {
            description.isEmpty() -> message.value = "Indica la descripción del concepto."
            amount == null || amount < 0 -> message.value = "Indica el monto planificado del concepto."
            categoryId == null -> message.value = "Selecciona una categoría antes de continuar."
            draft.type == FortnightItemType.SAVINGS && draft.savingsGoalId == null ->
                message.value = "Selecciona la meta de ahorro antes de continuar."
            else -> {
                val details = uiState.details ?: return
                val now = Instant.now()
                viewModelScope.launch {
                    isSaving.value = true
                    val item = FortnightPlanItem(
                        id = draft.itemId ?: 0,
                        planId = details.plan.id,
                        description = description,
                        plannedAmountInCents = amount,
                        categoryId = categoryId,
                        type = draft.type,
                        savingsGoalId = if (draft.type == FortnightItemType.SAVINGS) draft.savingsGoalId else null,
                        note = draft.note.trim().ifEmpty { null },
                        position = details.items.size,
                        createdAt = now,
                        updatedAt = now,
                    )
                    runCatching { repository.saveItem(item) }
                        .onSuccess { result ->
                            message.value = when (result) {
                                is FortnightMutationResult.Success -> if (draft.isEditing) {
                                    "Concepto actualizado."
                                } else {
                                    "Concepto agregado."
                                }
                                FortnightMutationResult.ClosedPlan -> periodIsClosed()
                                FortnightMutationResult.HasPayments -> "El concepto tiene abonos registrados."
                                FortnightMutationResult.NotFound -> "El concepto ya no existe."
                                FortnightMutationResult.TemplateInUse -> "La plantilla está en uso."
                            }
                            if (result is FortnightMutationResult.Success) cancelItemDraft()
                        }
                        .onFailure { message.value = "No se pudo guardar el concepto." }
                    isSaving.value = false
                }
            }
        }
    }

    fun requestDeleteItem(item: FortnightPlanItem) { mutablePendingDeleteItem.value = item }
    fun cancelDeleteItem() { mutablePendingDeleteItem.value = null }

    fun confirmDeleteItem() {
        val item = mutablePendingDeleteItem.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.deleteItem(item.id) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> "Concepto eliminado."
                        FortnightMutationResult.ClosedPlan -> periodIsClosed()
                        FortnightMutationResult.HasPayments ->
                            "Elimina o revierte los abonos antes de quitar el concepto."
                        FortnightMutationResult.NotFound -> "El concepto ya no existe."
                        FortnightMutationResult.TemplateInUse -> "La plantilla está en uso."
                    }
                }
                .onFailure { message.value = "No se pudo eliminar el concepto." }
            mutablePendingDeleteItem.value = null
            isSaving.value = false
        }
    }

    fun startPayment(item: FortnightPlanItem) {
        if (!state.value.canEditItems) return
        val progress = state.value.details?.itemProgress?.firstOrNull { it.item.id == item.id } ?: return
        if (progress.pendingInCents <= 0L) {
            message.value = "Este concepto ya está pagado."
            return
        }
        mutablePaymentDraft.value = FortnightPaymentDraft(
            itemId = item.id,
            itemDescription = item.description,
            amountText = MoneyFormatter.formatToInput(progress.pendingInCents),
        )
    }

    fun updatePaymentDraft(update: (FortnightPaymentDraft) -> FortnightPaymentDraft) {
        mutablePaymentDraft.value = mutablePaymentDraft.value?.let(update)
    }

    fun cancelPayment() {
        mutablePaymentDraft.value = null
        mutableOverpayment.value = null
    }

    fun submitPayment(allowOverpayment: Boolean = false) {
        if (isSaving.value) return
        val uiState = state.value
        if (!uiState.canEditItems) {
            message.value = periodIsClosed()
            cancelPayment()
            return
        }
        val draft = if (allowOverpayment) mutableOverpayment.value else mutablePaymentDraft.value
        val amount = draft?.amountInCents
        if (draft == null || amount == null) {
            message.value = "Indica el monto del abono."
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            runCatching {
                repository.registerPayment(
                    itemId = draft.itemId,
                    amountInCents = amount,
                    date = draft.date,
                    allowOverpayment = allowOverpayment,
                )
            }.onSuccess { result ->
                when (result) {
                    is FortnightPaymentResult.Registered -> {
                        message.value = "Abono registrado."
                        cancelPayment()
                        runCatching { updateAllFinanceWidgets(context) }
                    }
                    is FortnightPaymentResult.Overpayment -> mutableOverpayment.value = draft
                    FortnightPaymentResult.InvalidAmount -> message.value = "El monto del abono debe ser mayor que cero."
                    FortnightPaymentResult.ClosedPlan -> {
                        message.value = periodIsClosed()
                        cancelPayment()
                    }
                    FortnightPaymentResult.InvalidCategory ->
                        message.value = "La categoría del concepto ya no está disponible."
                    FortnightPaymentResult.NotFound -> {
                        message.value = "El concepto ya no existe."
                        cancelPayment()
                    }
                }
            }.onFailure { message.value = "No se pudo registrar el abono." }
            isSaving.value = false
        }
    }

    fun confirmOverpayment() {
        submitPayment(allowOverpayment = true)
    }

    fun cancelOverpayment() { mutableOverpayment.value = null }

    fun requestClosePlan() {
        if (!state.value.canEditItems) return
        mutablePendingClose.value = true
    }

    fun cancelClosePlan() { mutablePendingClose.value = false }

    fun confirmClosePlan() {
        val planId = state.value.details?.plan?.id ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.closePlan(planId) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> periodClosed()
                        else -> periodUnavailable()
                    }
                }
                .onFailure { message.value = couldNotClosePeriod() }
            mutablePendingClose.value = false
            isSaving.value = false
        }
    }

    fun reopenPlan() {
        val planId = state.value.details?.plan?.id ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.reopenPlan(planId) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> periodReopened()
                        else -> periodUnavailable()
                    }
                }
                .onFailure { message.value = couldNotReopenPeriod() }
            isSaving.value = false
        }
    }

    fun requestDeletePlan() {
        if (!state.value.hasPlan) return
        mutablePendingDeletePlan.value = true
    }

    fun cancelDeletePlan() { mutablePendingDeletePlan.value = false }

    fun confirmDeletePlan() {
        val planId = state.value.details?.plan?.id ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.deletePlan(planId) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> "Plan eliminado."
                        FortnightMutationResult.ClosedPlan -> reopenPeriodBeforeDelete()
                        FortnightMutationResult.HasPayments ->
                            "Revierte los abonos antes de eliminar el plan."
                        else -> "El plan ya no existe."
                    }
                }
                .onFailure { message.value = "No se pudo eliminar el plan." }
            mutablePendingDeletePlan.value = false
            isSaving.value = false
        }
    }

    fun requestDeletePayment(paymentId: Long) { mutablePendingDeletePayment.value = paymentId }
    fun cancelDeletePayment() { mutablePendingDeletePayment.value = null }

    fun confirmDeletePayment() {
        val paymentId = mutablePendingDeletePayment.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.deletePayment(paymentId) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> "Abono revertido."
                        FortnightMutationResult.ClosedPlan -> periodIsClosed()
                        else -> "El abono ya no existe."
                    }
                    if (result is FortnightMutationResult.Success) {
                        runCatching { updateAllFinanceWidgets(context) }
                    }
                }
                .onFailure { message.value = "No se pudo revertir el abono." }
            mutablePendingDeletePayment.value = null
            isSaving.value = false
        }
    }

    companion object {
        const val PERIOD_START_KEY = "fortnightPeriodStart"
        const val PERIOD_END_KEY = "fortnightPeriodEnd"
    }
}
