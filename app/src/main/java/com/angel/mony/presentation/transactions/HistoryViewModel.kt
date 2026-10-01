package com.angel.mony.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.angel.mony.core.FinanceDataCache
import com.angel.mony.core.CsvExporter
import com.angel.mony.core.HistoryPdfMeta
import com.angel.mony.core.HistoryPdfWriter
import com.angel.mony.core.HistorySpreadsheetWriter
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycle
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.repository.BackupPreview
import com.angel.mony.domain.repository.BackupRepository
import com.angel.mony.domain.repository.TransactionRepository
import com.angel.mony.widget.updateAllFinanceWidgets
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
    val budget: BudgetConfig? = null,
    val cycleHistory: List<BudgetCycle> = emptyList(),
    val isReady: Boolean = false,
)

data class RestorePreview(
    val movementsCount: Int,
    val firstDate: java.time.LocalDate?,
    val lastDate: java.time.LocalDate?,
    val movements: List<com.angel.mony.domain.model.BackupMovement>,
    val backupPreview: BackupPreview? = null,
    val rawContent: String? = null,
)

data class HistoryExportRequest(
    val transactions: List<FinanceTransaction>,
    val meta: HistoryPdfMeta,
)

enum class HistoryExportFormat(
    val label: String,
    val mimeType: String,
    val extension: String,
) {
    PDF("PDF", "application/pdf", "pdf"),
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    CSV("CSV", "text/csv", "csv"),
}

private fun initialHistoryState(dataCache: FinanceDataCache): HistoryUiState {
    val pendingById = dataCache.pendingEntries.value.associateBy { it.id }
    val links = dataCache.shoppingLists.value.mapNotNull { list ->
        list.expenseTransactionId?.let { it to list.id }
            ?: list.payableId?.let(pendingById::get)?.transactionId?.let { it to list.id }
    }.toMap()
    return HistoryUiState(
        transactions = dataCache.transactions.value,
        categories = dataCache.categories.value.associateBy(Category::id),
        shoppingListIdsByExpenseTransactionId = links,
        budget = dataCache.budget.value,
        cycleHistory = dataCache.budgetHistory.value,
        isReady = true,
    )
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionsRepository: TransactionRepository,
    dataCache: FinanceDataCache,
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val shoppingLinks = combine(
        dataCache.shoppingLists,
        dataCache.pendingEntries,
    ) { lists, pending ->
        val pendingById = pending.associateBy { it.id }
        lists.mapNotNull { list ->
            list.expenseTransactionId?.let { it to list.id }
                ?: list.payableId?.let(pendingById::get)?.transactionId?.let { it to list.id }
        }.toMap()
    }

    val state = combine(
        dataCache.transactions,
        dataCache.categories,
        shoppingLinks,
        dataCache.budget,
        dataCache.budgetHistory,
    ) { items, cats, links, budget, history ->
        HistoryUiState(
            transactions = items,
            categories = cats.associateBy(Category::id),
            shoppingListIdsByExpenseTransactionId = links,
            budget = budget,
            cycleHistory = history,
            isReady = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        initialHistoryState(dataCache),
    )

    val message = MutableStateFlow<String?>(null)
    val restorePreview = MutableStateFlow<RestorePreview?>(null)
    val isRestoring = MutableStateFlow(false)
    private var isExporting = false

    fun consumeMessage() {
        message.value = null
    }

    fun delete(id: Long) = viewModelScope.launch {
        runCatching { transactionsRepository.delete(id) }
            .onSuccess {
                message.value = "Movimiento eliminado."
                runCatching { updateAllFinanceWidgets(context) }
            }
            .onFailure { message.value = it.message ?: "No se pudo eliminar el movimiento." }
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
                val json = backupRepository.buildFullBackupJson()
                // Validar que haya algo para exportar (al menos un dato)
                val preview = backupRepository.parsePreview(json)
                if (preview.transactionsCount == 0 && preview.categoriesCount <= 21 && preview.fixedEntriesCount == 0 && preview.pendingEntriesCount == 0 && preview.shoppingListsCount == 0 && preview.savingsGoalsCount == 0) {
                    // Si solo hay categorías por defecto y nada más, considerar vacío
                    // Pero permitir exportar incluso vacío para no bloquear; solo validar JSON no vacío
                }
                writeBackupTo(uri, json)
            }.onSuccess {
                message.value = "Respaldo completo exportado correctamente."
            }.onFailure {
                message.value = it.message ?: "No se pudo exportar el respaldo"
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
                val content = bytes.toString(Charsets.UTF_8).removePrefix(CsvExporter.UTF8_BOM)
                val preview = backupRepository.parsePreview(content)
                if (preview.isLegacyCsv) {
                    val movements = com.angel.mony.core.CsvExporter.parseBackup(content.removePrefix(CsvExporter.UTF8_BOM))
                    if (movements.isEmpty()) error("El archivo no contiene movimientos para restaurar")
                    RestorePreview(
                        movementsCount = movements.size,
                        firstDate = movements.minOfOrNull { it.date },
                        lastDate = movements.maxOfOrNull { it.date },
                        movements = movements,
                        backupPreview = preview,
                        rawContent = content,
                    )
                } else {
                    if (preview.transactionsCount == 0 && preview.fixedEntriesCount == 0 && preview.pendingEntriesCount == 0 && preview.shoppingListsCount == 0 && preview.savingsGoalsCount == 0 && preview.budgetCyclesCount == 0) {
                        error("El archivo no contiene datos para restaurar")
                    }
                    RestorePreview(
                        movementsCount = preview.transactionsCount,
                        firstDate = preview.firstDate,
                        lastDate = preview.lastDate,
                        movements = emptyList(),
                        backupPreview = preview,
                        rawContent = content,
                    )
                }
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
                val content = preview.rawContent ?: error("Contenido no disponible")
                backupRepository.restoreBackup(content)
            }.onSuccess { result ->
                updateAllFinanceWidgets(context)
                message.value = when {
                    result.isLegacyCsv -> {
                        val skipped = result.skippedTransactions
                        if (skipped > 0) "Se restauraron ${result.insertedTransactions} movimientos. $skipped omitidos por duplicados."
                        else "Se restauraron ${result.insertedTransactions} movimientos correctamente."
                    }
                    result.totalInserted == 0 -> "No se insertaron datos nuevos. Todo ya existía."
                    else -> buildString {
                        append("Respaldo restaurado: ")
                        val parts = mutableListOf<String>()
                        if (result.insertedTransactions > 0) parts.add("${result.insertedTransactions} movimientos")
                        if (result.insertedFixedEntries > 0) parts.add("${result.insertedFixedEntries} fijos")
                        if (result.insertedPendingEntries > 0) parts.add("${result.insertedPendingEntries} pendientes")
                        if (result.insertedShoppingLists > 0) parts.add("${result.insertedShoppingLists} listas")
                        if (result.insertedSavingsGoals > 0) parts.add("${result.insertedSavingsGoals} metas")
                        if (result.insertedBudgetCycles > 0) parts.add("${result.insertedBudgetCycles} ciclos")
                        if (result.insertedFortnightPlans > 0) parts.add("${result.insertedFortnightPlans} quincenas")
                        if (result.insertedFortnightPayments > 0) parts.add("${result.insertedFortnightPayments} abonos quincenales")
                        if (parts.isEmpty()) parts.add("datos actualizados")
                        append(parts.joinToString(", "))
                        append(".")
                        if (result.skippedTransactions > 0) append(" ${result.skippedTransactions} movimientos omitidos por duplicados.")
                    }
                }
                restorePreview.value = null
            }.onFailure {
                message.value = it.message ?: "No se pudo restaurar el respaldo"
            }
            isRestoring.value = false
        }
    }

    fun exportHistoryTo(uri: Uri, request: HistoryExportRequest, format: HistoryExportFormat) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                if (request.transactions.isEmpty()) error("No hay movimientos para exportar")
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("No se pudo abrir el archivo seleccionado")
                writeHistory(request, format, stream)
            }.onSuccess {
                message.value = "Archivo ${format.label} guardado correctamente."
            }.onFailure {
                message.value = it.message ?: "No se pudo exportar el historial"
            }
            isExporting = false
        }
    }

    /** Genera el archivo en caché y entrega un URI temporal seguro para compartir. */
    fun shareHistory(
        request: HistoryExportRequest,
        format: HistoryExportFormat,
        onReady: (android.net.Uri) -> Unit,
    ) {
        if (isExporting) return
        viewModelScope.launch {
            isExporting = true
            runCatching {
                if (request.transactions.isEmpty()) error("No hay movimientos para exportar")
                val sharedDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                val file = java.io.File(
                    sharedDir,
                    "historial-${java.time.LocalDate.now()}.${format.extension}",
                )
                file.outputStream().use { writeHistory(request, format, it) }
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }.onSuccess { uri ->
                message.value = "Archivo listo para compartir."
                onReady(uri)
            }.onFailure {
                message.value = it.message ?: "No se pudo preparar el archivo"
            }
            isExporting = false
        }
    }

    private fun writeHistory(
        request: HistoryExportRequest,
        format: HistoryExportFormat,
        stream: java.io.OutputStream,
    ) {
        stream.use {
            when (format) {
                HistoryExportFormat.PDF ->
                    HistoryPdfWriter.writeTo(it, request.transactions, state.value.categories, request.meta)
                HistoryExportFormat.EXCEL ->
                    HistorySpreadsheetWriter.writeTo(it, request.transactions, state.value.categories, request.meta)
                HistoryExportFormat.CSV ->
                    it.write(CsvExporter.buildCsv(request.transactions, state.value.categories).toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun writeBackupTo(uri: Uri, json: String) {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("No se pudo abrir el archivo seleccionado")
        stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }
}
