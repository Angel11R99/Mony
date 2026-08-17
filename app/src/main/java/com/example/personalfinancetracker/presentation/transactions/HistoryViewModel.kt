package com.example.personalfinancetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.content.Context
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

data class HistoryUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(transactions.observeAll(), categories.observeAll()) { items, cats ->
        HistoryUiState(items, cats.associateBy(Category::id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun delete(id: Long) = viewModelScope.launch {
        transactions.delete(id)
        runCatching { updateAllFinanceWidgets(context) }
    }
}
