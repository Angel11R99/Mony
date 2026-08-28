package com.example.personalfinancetracker.presentation.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.core.EntryDisplayPreferences
import com.example.personalfinancetracker.core.MoneyFormatter
import com.example.personalfinancetracker.domain.model.EntryCardSize
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListOverview
import com.example.personalfinancetracker.domain.model.ShoppingListStatus
import com.example.personalfinancetracker.domain.repository.ShoppingListRepository
import com.example.personalfinancetracker.domain.repository.ShoppingMutationResult
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingListsUiState(
    val lists: List<ShoppingListOverview> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val activeLists get() = lists.filter { it.list.status != ShoppingListStatus.COMPLETED }
    val completedLists get() = lists.filter { it.list.status == ShoppingListStatus.COMPLETED }
}

@HiltViewModel
class ShoppingListsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: ShoppingListRepository,
) : ViewModel() {
    private val displayPreferences = EntryDisplayPreferences(context)

    val state: StateFlow<ShoppingListsUiState> = repository.observeListOverviews()
        .map { ShoppingListsUiState(lists = it, isLoading = false) }
        .catch { emit(ShoppingListsUiState(isLoading = false, hasError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListsUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    val cardSize: StateFlow<EntryCardSize> = displayPreferences.listCardSize
    private val mutablePendingDelete = MutableStateFlow<ShoppingList?>(null)
    val pendingDelete: StateFlow<ShoppingList?> = mutablePendingDelete
    private val mutablePendingReopen = MutableStateFlow<ShoppingList?>(null)
    val pendingReopen: StateFlow<ShoppingList?> = mutablePendingReopen

    fun consumeMessage() { message.value = null }
    fun setCardSize(size: EntryCardSize) = displayPreferences.setListCardSize(size)

    private val mutablePendingDuplicate = MutableStateFlow<ShoppingList?>(null)
    val pendingDuplicate: StateFlow<ShoppingList?> = mutablePendingDuplicate

    fun requestDuplicate(list: ShoppingList) { mutablePendingDuplicate.value = list }
    fun cancelDuplicate() { mutablePendingDuplicate.value = null }
    fun confirmDuplicate() {
        val list = mutablePendingDuplicate.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.duplicate(list.id) }
                .onSuccess { id ->
                    if (id == null) message.value = "La lista ya no existe."
                    else message.value = "Lista duplicada."
                }.onFailure { message.value = "No se pudo duplicar la lista." }
            mutablePendingDuplicate.value = null
            isSaving.value = false
        }
    }

    fun create(rawName: String, rawBudget: String, onCreated: (Long) -> Unit) {
        if (isSaving.value) return
        val name = rawName.trim()
        val budget = rawBudget.takeIf { it.isNotBlank() }?.let(MoneyFormatter::parseToCents)
        when {
            name.isEmpty() -> message.value = "Escribe un nombre para la lista."
            rawBudget.isNotBlank() && (budget == null || budget < 0) ->
                message.value = "Introduce un presupuesto válido."
            else -> viewModelScope.launch {
                isSaving.value = true
                runCatching {
                    val now = Instant.now()
                    repository.create(
                        ShoppingList(name = name, budgetInCents = budget, createdAt = now, updatedAt = now),
                    )
                }.onSuccess {
                    message.value = "Lista creada correctamente."
                    onCreated(it)
                }.onFailure { message.value = "No se pudo crear la lista." }
                isSaving.value = false
            }
        }
    }

    fun requestDelete(list: ShoppingList) {
        if (list.status == ShoppingListStatus.COMPLETED) {
            message.value = "Las listas completadas no se pueden eliminar."
        } else mutablePendingDelete.value = list
    }

    fun cancelDelete() { mutablePendingDelete.value = null }

    fun confirmDelete() {
        val list = mutablePendingDelete.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.delete(list.id) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is ShoppingMutationResult.Success -> "Lista eliminada."
                        ShoppingMutationResult.CompletedList -> "Las listas completadas no se pueden eliminar."
                        ShoppingMutationResult.NotFound -> "La lista ya no existe."
                    }
                }.onFailure { message.value = "No se pudo eliminar la lista." }
            mutablePendingDelete.value = null
            isSaving.value = false
        }
    }

    fun requestReopen(list: ShoppingList) { mutablePendingReopen.value = list }

    fun cancelReopen() { mutablePendingReopen.value = null }

    fun confirmReopen() {
        val list = mutablePendingReopen.value ?: return
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.reopen(list.id) }
                .onSuccess { result ->
                    message.value = when (result) {
                        is ShoppingMutationResult.Success -> "Lista reabierta."
                        ShoppingMutationResult.NotFound -> "La lista ya no existe."
                        ShoppingMutationResult.CompletedList -> "La lista ya no está completada."
                    }
                    if (result is ShoppingMutationResult.Success) runCatching { updateAllFinanceWidgets(context) }
                }.onFailure { message.value = "No se pudo reabrir la lista." }
            mutablePendingReopen.value = null
            isSaving.value = false
        }
    }
}
