package com.example.personalfinancetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.personalfinancetracker.core.CsvExporter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.TransactionRepository
import com.example.personalfinancetracker.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(transactions.observeAll(), categories.observeAll()) { items, cats ->
        HistoryUiState(items, cats.associateBy(Category::id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    val message = MutableStateFlow<String?>(null)
    private var isExporting = false

    fun consumeMessage() {
        message.value = null
    }

    fun delete(id: Long) = viewModelScope.launch {
        transactions.delete(id)
        runCatching { updateAllFinanceWidgets(context) }
    }

    fun exportTo(uri: Uri) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                val snapshot = state.value
                if (snapshot.transactions.isEmpty()) error("No hay movimientos para exportar")
                val csv = CsvExporter.buildCsv(snapshot.transactions, snapshot.categories)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo seleccionado")
            }.onSuccess {
                message.value = "Historial completo exportado correctamente."
            }.onFailure {
                message.value = it.message ?: "No se pudo exportar el historial"
            }
            isExporting = false
        }
    }
}
