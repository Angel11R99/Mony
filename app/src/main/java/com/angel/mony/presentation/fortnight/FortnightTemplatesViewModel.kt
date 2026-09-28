package com.angel.mony.presentation.fortnight

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FortnightItemType
import com.angel.mony.domain.model.FortnightPeriodStyle
import com.angel.mony.domain.model.FortnightSlot
import com.angel.mony.domain.model.FortnightTemplate
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.fortnightPeriodStyle
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.FortnightMutationResult
import com.angel.mony.domain.repository.FortnightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FortnightTemplatesUiState(
    val templates: List<FortnightTemplate> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isMonthly: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

data class FortnightTemplateDraft(
    val templateId: Long? = null,
    val description: String = "",
    val firstAmountText: String = "",
    val secondAmountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
) {
    val isEditing: Boolean get() = templateId != null
}

@HiltViewModel
class FortnightTemplatesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: FortnightRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
) : ViewModel() {

    val state: StateFlow<FortnightTemplatesUiState> = combine(
        repository.observeTemplates(),
        categoryRepository.observeActive(TransactionType.EXPENSE),
        budgetRepository.observe(),
    ) { templates, categories, budget ->
        FortnightTemplatesUiState(
            templates = templates,
            categories = categories,
            isMonthly = fortnightPeriodStyle(budget) == FortnightPeriodStyle.MONTHLY,
            isLoading = false,
        )
    }
        .catch { emit(FortnightTemplatesUiState(isLoading = false, hasError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FortnightTemplatesUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)

    private val mutableDraft = MutableStateFlow<FortnightTemplateDraft?>(null)
    val draft: StateFlow<FortnightTemplateDraft?> = mutableDraft

    private val mutablePendingDelete = MutableStateFlow<FortnightTemplate?>(null)
    val pendingDelete: StateFlow<FortnightTemplate?> = mutablePendingDelete

    fun consumeMessage() { message.value = null }

    fun startCreate() {
        mutableDraft.value = FortnightTemplateDraft(categoryId = state.value.categories.firstOrNull()?.id)
    }

    fun startEdit(template: FortnightTemplate) {
        mutableDraft.value = FortnightTemplateDraft(
            templateId = template.id,
            description = template.description,
            firstAmountText = template.firstFortnightAmountInCents?.let(MoneyFormatter::formatToInput).orEmpty(),
            secondAmountText = template.secondFortnightAmountInCents?.let(MoneyFormatter::formatToInput).orEmpty(),
            categoryId = template.categoryId,
            note = template.note.orEmpty(),
        )
    }

    fun updateDraft(update: (FortnightTemplateDraft) -> FortnightTemplateDraft) {
        mutableDraft.value = mutableDraft.value?.let(update)
    }

    fun cancelDraft() { mutableDraft.value = null }

    fun save() {
        if (isSaving.value) return
        val current = mutableDraft.value ?: return
        val description = current.description.trim()
        val categoryId = current.categoryId
        val first = current.firstAmountText.takeIf { it.isNotBlank() }?.let(MoneyFormatter::parseToCents)
        val second = current.secondAmountText.takeIf { it.isNotBlank() }?.let(MoneyFormatter::parseToCents)
        when {
            description.isEmpty() -> message.value = "Indica la descripción de la plantilla."
            categoryId == null -> message.value = "Selecciona una categoría antes de continuar."
            (first != null && first < 0) || (second != null && second < 0) ->
                message.value = "El monto debe ser mayor que cero."
            state.value.isMonthly && first == null ->
                message.value = "Indica el monto del mes."
            first == null && second == null ->
                message.value = "Indica el monto de al menos una quincena."
            else -> {
                val now = Instant.now()
                viewModelScope.launch {
                    isSaving.value = true
                    val existing = current.templateId?.let { id ->
                        state.value.templates.firstOrNull { it.id == id }
                    }
                    val template = FortnightTemplate(
                        id = current.templateId ?: 0,
                        description = description,
                        firstFortnightAmountInCents = first,
                        secondFortnightAmountInCents = second,
                        categoryId = categoryId,
                        type = FortnightItemType.EXPENSE,
                        note = current.note.trim().ifEmpty { null },
                        isActive = existing?.isActive ?: true,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                    runCatching { repository.saveTemplate(template) }
                        .onSuccess { result ->
                            message.value = when (result) {
                                is FortnightMutationResult.Success ->
                                    if (current.templateId == null) "Plantilla creada." else "Plantilla actualizada."
                                FortnightMutationResult.NotFound ->
                                    "La categoría ya no está disponible."
                                else -> "No se pudo guardar la plantilla."
                            }
                            if (result is FortnightMutationResult.Success) mutableDraft.value = null
                        }
                        .onFailure { message.value = "No se pudo guardar la plantilla." }
                    isSaving.value = false
                }
            }
        }
    }

    fun setActive(template: FortnightTemplate, isActive: Boolean) {
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.setTemplateActive(template.id, isActive) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success ->
                            if (isActive) "Plantilla activada." else "Plantilla pausada."
                        else -> "La plantilla ya no existe."
                    }
                }
                .onFailure { message.value = "No se pudo actualizar la plantilla." }
            isSaving.value = false
        }
    }

    fun requestDelete(template: FortnightTemplate) { mutablePendingDelete.value = template }
    fun cancelDelete() { mutablePendingDelete.value = null }

    fun confirmDelete() {
        val template = mutablePendingDelete.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.deleteTemplate(template.id) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is FortnightMutationResult.Success -> "Plantilla eliminada."
                        FortnightMutationResult.TemplateInUse ->
                            "La plantilla se usa en planes existentes. Puedes pausarla en su lugar."
                        else -> "La plantilla ya no existe."
                    }
                }
                .onFailure { message.value = "No se pudo eliminar la plantilla." }
            mutablePendingDelete.value = null
            isSaving.value = false
        }
    }

    fun templateAmount(template: FortnightTemplate, slot: FortnightSlot): String =
        template.amountFor(slot)?.let(MoneyFormatter::format) ?: "—"
}
