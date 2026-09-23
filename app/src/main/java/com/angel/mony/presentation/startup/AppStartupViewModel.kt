package com.angel.mony.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.domain.repository.BudgetRepository
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.FixedEntryRepository
import com.angel.mony.domain.repository.PendingEntryRepository
import com.angel.mony.domain.repository.SavingsRepository
import com.angel.mony.domain.repository.ShoppingListRepository
import com.angel.mony.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface AppStartupState {
    data object Loading : AppStartupState
    data object Ready : AppStartupState
    data class Error(val message: String) : AppStartupState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val budgets: BudgetRepository,
    private val fixedEntries: FixedEntryRepository,
    private val pendingEntries: PendingEntryRepository,
    private val savings: SavingsRepository,
    private val shoppingLists: ShoppingListRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val state: StateFlow<AppStartupState> = _state.asStateFlow()

    init {
        prepare()
    }

    fun retry() {
        if (_state.value == AppStartupState.Loading) return
        prepare()
    }

    private fun prepare() {
        _state.value = AppStartupState.Loading
        viewModelScope.launch {
            runCatching { preloadLocalData() }
                .onSuccess { _state.value = AppStartupState.Ready }
                .onFailure {
                    _state.value = AppStartupState.Error(
                        "No pudimos preparar tus datos. Revisa el almacenamiento e inténtalo de nuevo.",
                    )
                }
        }
    }

    private suspend fun preloadLocalData() = coroutineScope {
        listOf(
            async { transactions.observeAll().first() },
            async { categories.observeAll().first() },
            async { budgets.observe().first() },
            async { budgets.observeHistory().first() },
            async { fixedEntries.observeAll().first() },
            async { pendingEntries.observeAll().first() },
            async { savings.observeGoals().first() },
            async { shoppingLists.observeListOverviews().first() },
            async { delay(1_500L) },
        ).awaitAll()
    }
}
