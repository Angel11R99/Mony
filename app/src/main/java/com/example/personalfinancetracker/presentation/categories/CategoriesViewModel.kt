package com.example.personalfinancetracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.CategoryValidator
import com.example.personalfinancetracker.domain.model.TransactionType
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val usedCategoryIds: Set<Long> = emptySet(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    val state = combine(
        categoryRepository.observeAll(),
        categoryRepository.observeUsedCategoryIds(),
    ) { categories, usedIds ->
        CategoriesUiState(categories, usedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    val pendingDelete = MutableStateFlow<Category?>(null)

    fun consumeMessage() {
        message.value = null
    }

    fun create(rawName: String, type: TransactionType, onSaved: () -> Unit) =
        persist(rawName, type, existingId = null, successMessage = "Categoría creada correctamente.", onSaved) {
            categoryRepository.create(it, type)
        }

    fun rename(category: Category, rawName: String, onSaved: () -> Unit) =
        persist(
            rawName,
            category.type,
            existingId = category.id,
            successMessage = "Categoría actualizada.",
            onSaved,
        ) { categoryRepository.rename(category.id, it) }

    fun toggleActive(category: Category) {
        if (isSaving.value) return
        val target = !category.isActive
        if (!target) {
            CategoryValidator.lastActiveIncomeError(category, state.value.categories)?.let {
                message.value = it
                return
            }
        }
        viewModelScope.launch {
            runCatching { categoryRepository.setActive(category.id, target) }
                .onFailure { message.value = "No se pudo actualizar la categoría" }
        }
    }

    fun requestDelete(category: Category) {
        if (category.isActive) {
            CategoryValidator.lastActiveIncomeError(category, state.value.categories)?.let {
                message.value = it
                return
            }
        }
        pendingDelete.value = category
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val category = pendingDelete.value ?: return
        viewModelScope.launch {
            runCatching { categoryRepository.deleteIfUnused(category.id) }
                .onSuccess { deleted ->
                    message.value = if (deleted) "Categoría eliminada."
                    else "No se puede eliminar porque está en uso"
                }
                .onFailure { message.value = "No se pudo eliminar la categoría" }
            pendingDelete.value = null
        }
    }

    private fun persist(
        rawName: String,
        type: TransactionType,
        existingId: Long?,
        successMessage: String,
        onSaved: () -> Unit,
        action: suspend (String) -> Unit,
    ) {
        if (isSaving.value) return
        val validationError = CategoryValidator.validateName(
            rawName,
            type,
            state.value.categories,
            existingId,
        )
        if (validationError != null) {
            message.value = validationError
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            runCatching { action(rawName.trim()) }
                .onSuccess {
                    message.value = successMessage
                    onSaved()
                }
                .onFailure { message.value = "No se pudo guardar la categoría" }
            isSaving.value = false
        }
    }
}
