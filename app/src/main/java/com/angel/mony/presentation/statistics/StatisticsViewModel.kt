package com.angel.mony.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import com.angel.mony.core.FinanceDataCache
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.BudgetConfig
import com.angel.mony.domain.model.BudgetCycleSchedule
import com.angel.mony.domain.model.BudgetPeriod
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.FixedEntry
import com.angel.mony.domain.model.PendingEntry
import com.angel.mony.domain.model.SavingsGoalProgress
import com.angel.mony.domain.model.ShoppingList
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.FortnightPlanSummary
import com.angel.mony.domain.model.LocalLanguageModelManager
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.activeBudgetPeriod
import com.angel.mony.domain.model.budgetPeriodForSchedule
import com.angel.mony.domain.model.previousBudgetPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StatisticsUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val budget: BudgetConfig? = null,
    val isReady: Boolean = false,
    val fixedEntries: List<FixedEntry> = emptyList(),
    val pendingEntries: List<PendingEntry> = emptyList(),
    val savingsGoals: List<SavingsGoalProgress> = emptyList(),
    val shoppingLists: List<ShoppingList> = emptyList(),
    val fortnightPlans: List<FortnightPlanSummary> = emptyList(),
)

data class RecommendationHistoryEntry(
    val createdAt: Long,
    val scope: String,
    val response: String,
)

data class RecommendationUiState(
    val isGenerating: Boolean = false,
    val text: String? = null,
    val error: String? = null,
    val scope: String? = null,
    val requestId: Long? = null,
)

data class CategoryStatistic(
    val category: Category,
    val amountInCents: Long,
)

internal enum class StatisticsRange(val label: String) {
    CURRENT_BUDGET("Ciclo actual"),
    CURRENT_MONTH("Este mes"),
    CURRENT_YEAR("Este año"),
    ALL_TIME("Todo"),
    CUSTOM("Personalizado"),
}

internal data class StatisticsPeriod(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

internal fun statisticsPeriod(
    range: StatisticsRange,
    today: LocalDate = LocalDate.now(),
    budget: BudgetConfig? = null,
    customStart: LocalDate? = null,
    customEnd: LocalDate? = null,
): StatisticsPeriod = when (range) {
    StatisticsRange.CURRENT_BUDGET -> activeBudgetPeriod(budget, today).let {
        StatisticsPeriod(startDate = it.start, endDate = it.endInclusive)
    }
    StatisticsRange.CURRENT_MONTH -> StatisticsPeriod(
        startDate = YearMonth.from(today).atDay(1),
        endDate = YearMonth.from(today).atEndOfMonth(),
    )
    StatisticsRange.CURRENT_YEAR -> StatisticsPeriod(
        startDate = today.withDayOfYear(1),
        endDate = today,
    )
    StatisticsRange.ALL_TIME -> StatisticsPeriod(startDate = null, endDate = null)
    StatisticsRange.CUSTOM -> StatisticsPeriod(startDate = customStart, endDate = customEnd)
}

internal fun previousStatisticsPeriod(
    range: StatisticsRange,
    selectedCycle: BudgetCycleSchedule?,
    budget: BudgetConfig?,
    current: StatisticsPeriod,
    today: LocalDate = LocalDate.now(),
): StatisticsPeriod? = when {
    range == StatisticsRange.ALL_TIME || range == StatisticsRange.CUSTOM -> null
    selectedCycle != null -> budgetPeriodForSchedule(
        selectedCycle,
        (current.startDate ?: today).minusMonths(1),
    ).let { StatisticsPeriod(it.start, it.endInclusive) }
    range == StatisticsRange.CURRENT_BUDGET -> previousBudgetPeriod(budget, today).let {
        StatisticsPeriod(it.start, it.endInclusive)
    }
    range == StatisticsRange.CURRENT_MONTH -> YearMonth.from(today).minusMonths(1).let {
        StatisticsPeriod(it.atDay(1), it.atEndOfMonth())
    }
    else -> today.minusYears(1).let {
        StatisticsPeriod(it.withDayOfYear(1), it.withDayOfYear(it.lengthOfYear()))
    }
}

internal enum class TrendDirection { UP, DOWN, FLAT, NEW }

internal data class TrendDelta(val direction: TrendDirection, val percent: Int?)

internal fun trendDelta(current: Long, previous: Long): TrendDelta {
    if (previous <= 0L) {
        return if (current <= 0L) TrendDelta(TrendDirection.FLAT, null)
        else TrendDelta(TrendDirection.NEW, null)
    }
    val percent = ((current - previous) * 100 / previous).toInt()
    val direction = when {
        percent > 0 -> TrendDirection.UP
        percent < 0 -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }
    return TrendDelta(direction, percent)
}

internal fun statisticsPeriod(
    schedule: BudgetCycleSchedule,
    today: LocalDate = LocalDate.now(),
): StatisticsPeriod = budgetPeriodForSchedule(schedule, today).let {
    StatisticsPeriod(startDate = it.start, endDate = it.endInclusive)
}

data class StatisticsReport(
    val incomeInCents: Long,
    val expenseInCents: Long,
    val transactionCount: Int,
    val averageExpenseInCents: Long,
    val expenseByCategory: List<CategoryStatistic>,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
    val expenseRatio: Float get() =
        if (incomeInCents <= 0) 0f else (expenseInCents.toFloat() / incomeInCents).coerceAtLeast(0f)
}

internal fun statisticsRecommendationPrompt(
    periodLabel: String,
    report: StatisticsReport,
    selectedCategory: Category?,
    previousReport: StatisticsReport?,
    moduleContext: List<String> = emptyList(),
): String {
    val categoryStatistic = selectedCategory?.let { category ->
        report.expenseByCategory.firstOrNull { it.category.id == category.id }
    }
    val visibleCategories = if (selectedCategory != null) listOfNotNull(categoryStatistic) else report.expenseByCategory
    val categorySummary = visibleCategories.joinToString("\n") {
        "- ${it.category.name}: ${MoneyFormatter.format(it.amountInCents)}"
    }.ifBlank { "No se registraron gastos por categoría en el periodo." }
    val scopedMetrics = if (selectedCategory != null) {
        "Filtro activo: categoría ${selectedCategory.name}. Gasto registrado en esa categoría: ${MoneyFormatter.format(categoryStatistic?.amountInCents ?: 0)}. No uses los totales generales de otras categorías."
    } else {
        "Ingresos del periodo: ${MoneyFormatter.format(report.incomeInCents)}. Gastos del periodo: ${MoneyFormatter.format(report.expenseInCents)}. Balance: ${MoneyFormatter.format(report.balanceInCents)}. Movimientos: ${report.transactionCount}. Gasto promedio por movimiento: ${MoneyFormatter.format(report.averageExpenseInCents)}."
    }
    val comparison = if (selectedCategory == null && previousReport != null) {
        "En el periodo anterior hubo ${MoneyFormatter.format(previousReport.expenseInCents)} en gastos y ${MoneyFormatter.format(previousReport.incomeInCents)} en ingresos."
    } else ""
    val modules = moduleContext.filter(String::isNotBlank).joinToString("\n").ifBlank { "No hay datos adicionales disponibles en otros módulos." }
    return """Eres un asistente de finanzas personales. Habla de forma llana, breve y natural en español. Analiza SOLO los datos visibles del periodo "$periodLabel". $scopedMetrics $comparison
Gastos por categoría visibles:
$categorySummary
Datos relevantes de otros módulos:
$modules
Da como máximo 3 recomendaciones concretas, priorizando las que conecten resultados entre módulos. Usa solo los datos entregados y cita la cifra o estado que justifica cada recomendación. No inventes causas, hábitos, objetivos ni datos. Solo recomienda sobre un módulo si tiene datos relevantes; no sugieras gastos cuando no hay gastos registrados. Evita consejos genéricos y listas de módulos sin hallazgos. Sin Markdown, sin introducciones largas."""
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    dataCache: FinanceDataCache,
    private val languageModels: LocalLanguageModelManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val historyPreferences = context.getSharedPreferences("statistics_recommendations", Context.MODE_PRIVATE)
    private val _recommendationHistory = MutableStateFlow(readRecommendationHistory())
    val recommendationHistory: StateFlow<List<RecommendationHistoryEntry>> = _recommendationHistory
    private val _recommendationState = MutableStateFlow(
        _recommendationHistory.value.firstOrNull()?.let { RecommendationUiState(text = it.response, scope = it.scope) }
            ?: RecommendationUiState(),
    )
    val recommendationState: StateFlow<RecommendationUiState> = _recommendationState
    private var recommendationGenerationId = 0L

    fun generateRecommendation(prompt: String, scope: String) {
        if (_recommendationState.value.isGenerating) return
        if (prompt.isBlank()) {
            _recommendationState.value = RecommendationUiState()
            return
        }
        val requestId = ++recommendationGenerationId
        _recommendationState.value = RecommendationUiState(isGenerating = true, scope = scope, requestId = requestId)
        viewModelScope.launch {
            try {
                val result = languageModels.generateText(prompt).trim()
                if (result.isNotBlank()) {
                    _recommendationHistory.value = (listOf(RecommendationHistoryEntry(System.currentTimeMillis(), scope, result)) + _recommendationHistory.value).take(30)
                    writeRecommendationHistory(_recommendationHistory.value)
                    _recommendationState.value = RecommendationUiState(text = result, scope = scope, requestId = requestId)
                    context.showToast("La recomendación terminó correctamente. Puedes verla en el historial.")
                } else {
                    _recommendationState.value = RecommendationUiState(
                        error = "El modelo no generó una recomendación. Inténtalo otra vez.",
                        scope = scope,
                        requestId = requestId,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val message = failure.message
                    ?.takeIf { it.startsWith("Descarga") || it.startsWith("Gemini") || it.startsWith("No hay") }
                    ?: "No se pudieron generar recomendaciones. Comprueba que haya un modelo local disponible e inténtalo de nuevo."
                _recommendationState.value = RecommendationUiState(error = message, scope = scope, requestId = requestId)
            } finally {
                if (_recommendationState.value.requestId == requestId && _recommendationState.value.isGenerating) {
                    _recommendationState.value = RecommendationUiState()
                }
            }
        }
    }

    private fun readRecommendationHistory(): List<RecommendationHistoryEntry> = try {
        val json = JSONArray(historyPreferences.getString("entries", "[]"))
        (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let { item ->
                RecommendationHistoryEntry(
                    createdAt = item.optLong("createdAt"),
                    scope = item.optString("scope"),
                    response = item.optString("response"),
                ).takeIf { it.response.isNotBlank() }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeRecommendationHistory(entries: List<RecommendationHistoryEntry>) {
        val json = JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject().put("createdAt", entry.createdAt).put("scope", entry.scope).put("response", entry.response))
            }
        }
        historyPreferences.edit().putString("entries", json.toString()).apply()
    }
    val state = combine(
        dataCache.transactions,
        dataCache.categories,
        dataCache.budget,
        dataCache.fixedEntries,
        dataCache.pendingEntries,
        dataCache.savingsGoals,
        dataCache.shoppingLists,
        dataCache.fortnightPlans,
    ) { values ->
        StatisticsUiState(
            transactions = values[0] as List<FinanceTransaction>,
            categories = (values[1] as List<Category>).associateBy(Category::id),
            budget = values[2] as BudgetConfig?,
            isReady = true,
            fixedEntries = values[3] as List<FixedEntry>,
            pendingEntries = values[4] as List<PendingEntry>,
            savingsGoals = values[5] as List<SavingsGoalProgress>,
            shoppingLists = values[6] as List<ShoppingList>,
            fortnightPlans = values[7] as List<FortnightPlanSummary>,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatisticsUiState(
            transactions = dataCache.transactions.value,
            categories = dataCache.categories.value.associateBy(Category::id),
            budget = dataCache.budget.value,
            isReady = true,
        ),
    )
}

internal fun StatisticsRange.displayLabel(budget: BudgetConfig?): String =
    if (this != StatisticsRange.CURRENT_BUDGET) label
    else if (budget?.period == BudgetPeriod.MONTHLY) "Este ciclo mensual" else "Este ciclo"

private val customRangeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun customRangeLabel(start: LocalDate?, end: LocalDate?): String {
    val formattedStart = start?.format(customRangeFormatter)
    val formattedEnd = end?.format(customRangeFormatter)
    return when {
        formattedStart != null && formattedEnd != null -> "$formattedStart – $formattedEnd"
        formattedStart != null -> "Desde $formattedStart"
        formattedEnd != null -> "Hasta $formattedEnd"
        else -> "Personalizado"
    }
}

internal fun BudgetCycleSchedule.displayLabel(index: Int): String =
    "Ciclo ${index + 1} · días $openingDay-$closingDay"

internal fun calculateStatistics(
    transactions: List<FinanceTransaction>,
    categories: Map<Long, Category>,
    startDate: LocalDate?,
    endDate: LocalDate? = null,
): StatisticsReport {
    var income = 0L
    var expense = 0L
    var transactionCount = 0
    var expenseCount = 0
    val expenseByCategory = HashMap<Long, Long>()

    transactions.forEach { transaction ->
        if ((startDate != null && transaction.date.isBefore(startDate)) ||
            (endDate != null && transaction.date.isAfter(endDate))
        ) return@forEach

        transactionCount++
        when (transaction.type) {
            TransactionType.INCOME -> income += transaction.amountInCents
            TransactionType.EXPENSE -> {
                expense += transaction.amountInCents
                expenseCount++
                expenseByCategory[transaction.categoryId] =
                    (expenseByCategory[transaction.categoryId] ?: 0L) + transaction.amountInCents
            }
        }
    }

    val breakdown = expenseByCategory.mapNotNull { (categoryId, amount) ->
        categories[categoryId]?.let { category -> CategoryStatistic(category, amount) }
    }
        .sortedByDescending(CategoryStatistic::amountInCents)
    return StatisticsReport(
        incomeInCents = income,
        expenseInCents = expense,
        transactionCount = transactionCount,
        averageExpenseInCents = if (expenseCount == 0) 0 else expense / expenseCount,
        expenseByCategory = breakdown,
    )
}
