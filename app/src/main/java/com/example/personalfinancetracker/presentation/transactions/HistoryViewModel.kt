package com.example.personalfinancetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.personalfinancetracker.core.CsvExporter
import com.example.personalfinancetracker.core.HistoryPdfMeta
import com.example.personalfinancetracker.core.HistoryPdfWriter
import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.repository.CategoryRepository
import com.example.personalfinancetracker.domain.repository.ShoppingListRepository
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
    val shoppingListIdsByExpenseTransactionId: Map<Long, Long> = emptyMap(),
    val isReady: Boolean = false,
)

data class RestorePreview(
    val movementsCount: Int,
    val firstDate: java.time.LocalDate?,
    val lastDate: java.time.LocalDate?,
    val movements: List<com.example.personalfinancetracker.domain.model.BackupMovement>,
)

data class HistoryPdfRequest(
    val transactions: List<FinanceTransaction>,
    val meta: HistoryPdfMeta,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionsRepository: TransactionRepository,
    categories: CategoryRepository,
    shoppingLists: ShoppingListRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val state = combine(
        transactionsRepository.observeAll(),
        categories.observeAll(),
        shoppingLists.observeLists(),
    ) { items, cats, lists ->
        HistoryUiState(
            transactions = items,
            categories = cats.associateBy(Category::id),
            shoppingListIdsByExpenseTransactionId = lists.mapNotNull { list ->
                list.expenseTransactionId?.let { transactionId -> transactionId to list.id }
            }.toMap(),
            isReady = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    val message = MutableStateFlow<String?>(null)
    val restorePreview = MutableStateFlow<RestorePreview?>(null)
    val isRestoring = MutableStateFlow(false)
    private var isExporting = false

    fun consumeMessage() {
        message.value = null
    }

    fun delete(id: Long) = viewModelScope.launch {
        transactionsRepository.delete(id)
        runCatching { updateAllFinanceWidgets(context) }
    }

    fun duplicate(id: Long) = viewModelScope.launch {
        runCatching { transactionsRepository.duplicate(id) }
            .onSuccess { created ->
                if (created != null) {
                    message.value = "Movimiento duplicado."
                    updateAllFinanceWidgets(context)
                } else {
                    message.value = "No se encontró el movimiento"
                }
            }
            .onFailure { message.value = "No se pudo duplicar el movimiento" }
    }

    fun exportTo(uri: Uri) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                val snapshot = state.value
                if (snapshot.transactions.isEmpty()) error("No hay movimientos para exportar")
                val csv = CsvExporter.buildCsv(snapshot.transactions, snapshot.categories)
                writeCsvTo(uri, csv)
            }.onSuccess {
                message.value = "Historial completo exportado correctamente."
            }.onFailure {
                message.value = it.message ?: "No se pudo exportar el historial"
            }
            isExporting = false
        }
    }

    /** Lee el archivo seleccionado, valida su contenido y prepara la restauración sin tocar la base de datos. */
    fun prepareImportFrom(uri: Uri) {
        if (isRestoring.value) return
        viewModelScope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("No se pudo abrir el archivo seleccionado")
                // Decodificación explícita en UTF-8, tolerando archivos con o sin BOM.
                val content = bytes.toString(Charsets.UTF_8).removePrefix(CsvExporter.UTF8_BOM)
                val movements = CsvExporter.parseBackup(content)
                if (movements.isEmpty()) error("El archivo no contiene movimientos para restaurar")
                RestorePreview(
                    movementsCount = movements.size,
                    firstDate = movements.minOfOrNull { it.date },
                    lastDate = movements.maxOfOrNull { it.date },
                    movements = movements,
                )
            }.onSuccess { preview ->
                restorePreview.value = preview
            }.onFailure {
                message.value = it.message ?: "No se pudo leer el archivo de respaldo"
            }
        }
    }

    fun cancelRestore() {
        if (!isRestoring.value) restorePreview.value = null
    }

    fun confirmRestore() {
        val preview = restorePreview.value ?: return
        if (isRestoring.value) return
        viewModelScope.launch {
            isRestoring.value = true
            runCatching {
                transactionsRepository.restoreBackup(preview.movements)
            }.onSuccess { inserted ->
                updateAllFinanceWidgets(context)
                val skipped = preview.movementsCount - inserted
                message.value = if (skipped > 0) {
                    "Se restauraron $inserted movimientos. $skipped omitidos por duplicados."
                } else {
                    "Se restauraron $inserted movimientos correctamente."
                }
                restorePreview.value = null
            }.onFailure {
                message.value = "No se pudo restaurar el respaldo"
            }
            isRestoring.value = false
        }
    }

    fun exportPdfTo(uri: Uri, request: HistoryPdfRequest) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                if (request.transactions.isEmpty()) error("No hay movimientos para exportar")
                writePdf(request, context.contentResolver.openOutputStream(uri)
                    ?: error("No se pudo abrir el archivo seleccionado"))
            }.onSuccess {
                message.value = "PDF generado correctamente."
            }.onFailure {
                message.value = it.message ?: "No se pudo generar el PDF"
            }
            isExporting = false
        }
    }

    /** Genera el PDF en la caché y entrega el URI compartible para abrirlo con otras aplicaciones. */
    fun sharePdf(request: HistoryPdfRequest, onReady: (android.net.Uri) -> Unit) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                if (request.transactions.isEmpty()) error("No hay movimientos para exportar")
                val sharedDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                val file = java.io.File(sharedDir, "historial-${java.time.LocalDate.now()}.pdf")
                file.outputStream().use { writePdf(request, it) }
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }.onSuccess { uri ->
                onReady(uri)
            }.onFailure {
                message.value = it.message ?: "No se pudo generar el PDF"
            }
            isExporting = false
        }
    }

    private fun writePdf(request: HistoryPdfRequest, stream: java.io.OutputStream) {
        stream.use {
            HistoryPdfWriter.writeTo(it, request.transactions, state.value.categories, request.meta)
        }
    }

    private fun writeCsvTo(uri: Uri, csv: String) {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("No se pudo abrir el archivo seleccionado")
        // Escritura explícita en UTF-8 con BOM para máxima compatibilidad.
        stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
    }
}
