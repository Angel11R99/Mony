package com.angel.mony.presentation.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.KnownProduct
import com.angel.mony.domain.model.ListProductMatcher
import com.angel.mony.domain.model.ProductMatchCandidate
import com.angel.mony.domain.model.ProductMatchResult
import com.angel.mony.domain.model.ShoppingAdjustment
import com.angel.mony.domain.model.ShoppingListDetails
import com.angel.mony.domain.model.ShoppingListItem
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.ShoppingPaymentMethod
import com.angel.mony.domain.model.TicketAmountCandidate
import com.angel.mony.domain.model.TicketAmountKind
import com.angel.mony.domain.model.RecognitionConfidence
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.repository.CategoryRepository
import com.angel.mony.domain.repository.FinalizePurchaseResult
import com.angel.mony.domain.repository.ProductCatalogRepository
import com.angel.mony.domain.repository.ProductCatalogResult
import com.angel.mony.domain.repository.ShoppingListRepository
import com.angel.mony.domain.repository.ShoppingMutationResult
import com.angel.mony.domain.repository.TicketProductUpdate
import com.angel.mony.widget.updateAllFinanceWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val details: ShoppingListDetails? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val editable get() = details?.let { value ->
        value.list.status != ShoppingListStatus.COMPLETED ||
            value.list.purchaseDate != null && value.list.paymentMethod != null && value.list.expenseCategoryId != null
    } == true
    val defaultCategoryId get() = categories.firstOrNull { it.name.equals("Compras", true) }?.id
        ?: categories.firstOrNull()?.id
}

data class PendingBarcodeMatch(
    val barcode: String,
    val knownProduct: KnownProduct,
    val itemId: Long,
    val itemName: String,
)

data class NewScannedProduct(
    val barcode: String,
    val suggestedName: String = "",
    val suggestedPriceInCents: Long? = null,
)

data class RemoteLookupResult(
    val barcode: String,
    val name: String,
    val brand: String?,
    val source: String,
)

data class MatchedScannedProduct(
    val item: ShoppingListItem,
    val barcode: String,
    val suggestedName: String,
    val suggestedPriceInCents: Long?,
)

data class AdjustmentDraft(val name: String, val isPositive: Boolean, val amountInCents: Long)

data class TicketProductDraft(
    val occurrenceId: String,
    val detectedText: String,
    val name: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val selectedItemId: Long?,
    val suggestedItemId: Long?,
    val confidence: RecognitionConfidence,
    val included: Boolean = true,
)

data class TicketReview(
    val products: List<TicketProductDraft>,
    val adjustments: List<TicketAmountCandidate>,
    val ticketTotalInCents: Long?,
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingListRepository,
    private val catalogRepository: ProductCatalogRepository,
    categories: CategoryRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val listId: Long = checkNotNull(savedStateHandle["listId"])
    private val flowError = MutableStateFlow(false)

    val state: StateFlow<ShoppingListUiState> = combine(
        repository.observeDetails(listId),
        categories.observeActive(TransactionType.EXPENSE),
        flowError,
    ) { details, expenseCategories, failed ->
        ShoppingListUiState(details, expenseCategories, isLoading = false, hasError = failed)
    }.catch {
        flowError.value = true
        emit(ShoppingListUiState(isLoading = false, hasError = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListUiState())

    val message = MutableStateFlow<String?>(null)
    val isSaving = MutableStateFlow(false)
    val pendingBarcodeMatch = MutableStateFlow<PendingBarcodeMatch?>(null)
    val newScannedProduct = MutableStateFlow<NewScannedProduct?>(null)
    val matchedScannedProduct = MutableStateFlow<MatchedScannedProduct?>(null)
    val missingPriceItemIds = MutableStateFlow<List<Long>>(emptyList())
    val purchaseCompleted = MutableStateFlow(false)
    val remoteLookupResult = MutableStateFlow<RemoteLookupResult?>(null)
    val isLookingUp = MutableStateFlow(false)
    val ticketReview = MutableStateFlow<TicketReview?>(null)
    private var pendingFinalizeCategoryId: Long? = null
    private var pendingFinalizeDate: LocalDate? = null
    private var pendingFinalizePaymentMethod: ShoppingPaymentMethod? = null

    fun consumeMessage() { message.value = null }
    fun clearScannedProduct() { newScannedProduct.value = null }
    fun clearMatchedScannedProduct() { matchedScannedProduct.value = null }
    fun clearMissingPrices() { missingPriceItemIds.value = emptyList() }
    fun consumePurchaseCompleted() { purchaseCompleted.value = false }
    fun consumeRemoteLookupResult() { remoteLookupResult.value = null }
    fun cancelBarcodeMatch() { pendingBarcodeMatch.value = null }
    fun clearTicketReview() { ticketReview.value = null }
    fun markMissingPriceReviewed(itemId: Long) {
        missingPriceItemIds.value = missingPriceItemIds.value - itemId
    }

    fun updateList(rawName: String, rawBudget: String, onSaved: () -> Unit = {}) {
        val details = state.value.details ?: return
        if (!state.value.editable || isSaving.value) return
        val name = rawName.trim()
        val budget = rawBudget.takeIf(String::isNotBlank)?.let(MoneyFormatter::parseToCents)
        when {
            name.isEmpty() -> message.value = "El nombre de la lista no puede estar vacío."
            rawBudget.isNotBlank() && (budget == null || budget < 0) -> message.value = "Presupuesto no válido."
            else -> mutate("Lista actualizada.", onSaved) {
                repository.update(details.list.copy(name = name, budgetInCents = budget, updatedAt = Instant.now()))
            }
        }
    }

    fun startShopping() {
        val details = state.value.details ?: return
        if (details.list.status != ShoppingListStatus.PENDING) return
        mutate("Compra iniciada.") {
            repository.update(details.list.copy(status = ShoppingListStatus.SHOPPING, updatedAt = Instant.now()))
        }
    }

    fun saveItem(
        existing: ShoppingListItem?, rawName: String, quantity: Int, rawEstimated: String,
        rawActual: String, barcode: String, notes: String, purchased: Boolean, onSaved: () -> Unit = {},
    ) {
        if (!state.value.editable || isSaving.value) return
        val name = rawName.trim()
        val estimated = parseOptionalPrice(rawEstimated)
        val actual = parseOptionalPrice(rawActual)
        when {
            name.isEmpty() -> message.value = "Escribe el nombre del producto."
            quantity < 1 -> message.value = "La cantidad debe ser al menos uno."
            rawEstimated.isNotBlank() && estimated == null -> message.value = "El precio estimado no es válido."
            rawActual.isNotBlank() && actual == null -> message.value = "El precio real no es válido."
            else -> {
                val now = Instant.now()
                val candidate = ShoppingListItem(
                            id = existing?.id ?: 0,
                            shoppingListId = listId,
                            name = name,
                            quantity = quantity,
                            estimatedUnitPriceInCents = estimated,
                            actualUnitPriceInCents = actual,
                            barcode = barcode.trim().ifEmpty { null },
                            isPurchased = purchased,
                            isIdentified = barcode.isNotBlank(),
                            notes = notes.trim().ifEmpty { null },
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                        )
                if (!hasSafeTotals(candidate)) {
                    message.value = "El precio o la cantidad son demasiado grandes."
                    return
                }
                mutate(if (existing == null) "Producto agregado." else "Producto actualizado.", onSaved) {
                    repository.saveItem(candidate)
                }
            }
        }
    }

    fun changeQuantity(item: ShoppingListItem, delta: Int) {
        val quantity = item.quantity.toLong().plus(delta).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        if (quantity == item.quantity) return
        saveItemCopy(item.copy(quantity = quantity, updatedAt = Instant.now()), null)
    }

    fun togglePurchased(item: ShoppingListItem) = saveItemCopy(
        item.copy(isPurchased = !item.isPurchased, updatedAt = Instant.now()),
        if (item.isPurchased) "Producto marcado como pendiente." else "Producto marcado como comprado.",
    )

    fun deleteItem(item: ShoppingListItem) = mutate("Producto eliminado.") { repository.deleteItem(item.id) }

    fun saveAdjustment(
        existing: ShoppingAdjustment?, rawName: String, positive: Boolean, rawAmount: String,
        onSaved: () -> Unit = {},
    ) {
        val name = rawName.trim()
        val amount = MoneyFormatter.parseToCents(rawAmount)
        when {
            name.isEmpty() -> message.value = "Escribe el nombre del ajuste."
            amount == null || amount <= 0 -> message.value = "El monto del ajuste debe ser mayor que cero."
            else -> {
                val candidate = ShoppingAdjustment(
                    existing?.id ?: 0,
                    listId,
                    name,
                    positive,
                    amount,
                    existing?.createdAt ?: Instant.now(),
                )
                if (!hasSafeAdjustments(listOf(candidate), replaceExisting = true)) {
                    message.value = "El total de ajustes es demasiado grande."
                    return
                }
                mutate(if (existing == null) "Ajuste agregado." else "Ajuste actualizado.", onSaved) {
                    repository.saveAdjustment(candidate)
                }
            }
        }
    }

    fun deleteAdjustment(adjustment: ShoppingAdjustment) =
        mutate("Ajuste eliminado.") { repository.deleteAdjustment(adjustment.id) }

    fun prepareTicketReview(candidates: List<TicketAmountCandidate>) {
        val details = state.value.details ?: return
        viewModelScope.launch {
            val automaticallyAssigned = mutableSetOf<Long>()
            val products = candidates.filter { it.kind == TicketAmountKind.PRODUCTO }.map { candidate ->
                val learnedName = runCatching { repository.findLearnedNames(candidate.productName.orEmpty()).firstOrNull() }.getOrNull()
                val detectedName = learnedName ?: candidate.productName.orEmpty()
                val match = ListProductMatcher.match(
                    detectedName,
                    null,
                    details.items.map { ProductMatchCandidate(it.id, it.name, it.barcode, it.isPurchased, false) },
                )
                val suggestedId = when (match) {
                    is ProductMatchResult.Clear -> match.itemId
                    is ProductMatchResult.Ambiguous -> match.itemId
                    ProductMatchResult.None -> null
                }
                var confidence = when {
                    learnedName != null -> RecognitionConfidence.HIGH
                    match is ProductMatchResult.Clear && candidate.confidence != RecognitionConfidence.LOW -> RecognitionConfidence.HIGH
                    suggestedId != null || candidate.confidence == RecognitionConfidence.MEDIUM -> RecognitionConfidence.MEDIUM
                    else -> RecognitionConfidence.LOW
                }
                val selectedId = if (
                    suggestedId != null && confidence == RecognitionConfidence.HIGH && automaticallyAssigned.add(suggestedId)
                ) suggestedId else null
                if (suggestedId != null && selectedId == null && confidence == RecognitionConfidence.HIGH) {
                    confidence = RecognitionConfidence.MEDIUM
                }
                TicketProductDraft(
                    occurrenceId = candidate.occurrenceId,
                    detectedText = candidate.productName.orEmpty(),
                    name = detectedName,
                    quantity = candidate.quantity,
                    unitPriceInCents = candidate.amountInCents,
                    selectedItemId = selectedId,
                    suggestedItemId = suggestedId,
                    confidence = confidence,
                )
            }
            ticketReview.value = TicketReview(
                products = products,
                adjustments = candidates.filter { it.kind in setOf(TicketAmountKind.TAX, TicketAmountKind.DISCOUNT, TicketAmountKind.SHIPPING, TicketAmountKind.SERVICE) },
                ticketTotalInCents = candidates.lastOrNull { it.kind == TicketAmountKind.TOTAL }?.amountInCents,
            )
        }
    }

    fun applyTicketReview(products: List<TicketProductDraft>, adjustmentDrafts: List<AdjustmentDraft>) {
        if (isSaving.value) return
        val included = products.filter { it.included }
        val totalsAreSafe = runCatching {
            val productTotal = included.fold(0L) { total, product ->
                Math.addExact(total, Math.multiplyExact(product.quantity.toLong(), product.unitPriceInCents))
            }
            val adjustmentTotal = adjustmentDrafts.fold(0L) { total, adjustment ->
                Math.addExact(total, if (adjustment.isPositive) adjustment.amountInCents else Math.negateExact(adjustment.amountInCents))
            }
            Math.addExact(productTotal, adjustmentTotal)
        }.isSuccess
        when {
            included.any { it.name.isBlank() } -> message.value = "Revisa el nombre de los productos detectados."
            included.any { it.quantity < 1 } -> message.value = "La cantidad debe ser al menos uno."
            included.any { it.unitPriceInCents < 0 } -> message.value = "Revisa los precios detectados."
            adjustmentDrafts.any { it.name.isBlank() || it.amountInCents <= 0 } ->
                message.value = "Revisa el nombre y monto de los ajustes."
            included.mapNotNull { it.selectedItemId }.let { it.size != it.distinct().size } ->
                message.value = "Cada producto de la lista solo puede asociarse una vez por ticket."
            !totalsAreSafe -> message.value = "El precio o la cantidad son demasiado grandes."
            else -> viewModelScope.launch {
                isSaving.value = true
                val adjustments = adjustmentDrafts.map {
                    ShoppingAdjustment(0, listId, it.name, it.isPositive, it.amountInCents, Instant.now())
                }
                runCatching {
                    repository.applyTicketReview(
                        listId,
                        included.map { TicketProductUpdate(it.detectedText, it.selectedItemId, it.name, it.quantity, it.unitPriceInCents) },
                        adjustments,
                    )
                }.onSuccess { result ->
                    message.value = when (result) {
                        is ShoppingMutationResult.Success -> "Ticket aplicado correctamente."
                        ShoppingMutationResult.CompletedList -> "La compra finalizada no admite otro ticket."
                        ShoppingMutationResult.NotFound -> "La lista ya no existe."
                    }
                    if (result is ShoppingMutationResult.Success) ticketReview.value = null
                }.onFailure { message.value = "No se pudo aplicar la revisión del ticket." }
                isSaving.value = false
            }
        }
    }

    fun onBarcodeScanned(rawBarcode: String) {
        val barcode = rawBarcode.trim()
        val details = state.value.details ?: return
        if (barcode.isEmpty()) { message.value = "El escáner no devolvió un código."; return }
        if (isSaving.value) return
        val existing = details.items.firstOrNull { it.barcode == barcode }
        if (existing != null) {
            if (existing.quantity == Int.MAX_VALUE) {
                message.value = "La cantidad del producto es demasiado grande."
                return
            }
            saveItemCopy(existing.copy(quantity = existing.quantity + 1, updatedAt = Instant.now()), "Cantidad incrementada.")
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.findKnownProduct(barcode) }
                .onSuccess { known ->
                    isSaving.value = false
                    handleKnownProduct(barcode, known, details)
                }
                .onFailure { message.value = "No se pudo consultar el producto escaneado." }
            isSaving.value = false
        }
    }

    private fun handleKnownProduct(barcode: String, known: KnownProduct?, details: ShoppingListDetails) {
        if (known == null) {
            newScannedProduct.value = NewScannedProduct(barcode)
            viewModelScope.launch { lookupRemote(barcode) }
            return
        }
        val match = ListProductMatcher.match(
            known.name, barcode,
            details.items.map { ProductMatchCandidate(it.id, it.name, it.barcode, it.isPurchased, it.isIdentified) },
        )
        when (match) {
            is ProductMatchResult.Clear -> linkKnownProduct(match.itemId, barcode, known)
            is ProductMatchResult.Ambiguous -> pendingBarcodeMatch.value = PendingBarcodeMatch(
                barcode, known, match.itemId, match.candidateName,
            )
            ProductMatchResult.None -> newScannedProduct.value = NewScannedProduct(barcode, known.name, known.lastPriceInCents)
        }
    }

    fun lookupRemote(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty() || isLookingUp.value) return
        viewModelScope.launch {
            isLookingUp.value = true
            runCatching { catalogRepository.lookup(trimmed) }
                .onSuccess { result ->
                    when (result) {
                        is ProductCatalogResult.Found -> {
                            remoteLookupResult.value = RemoteLookupResult(
                                barcode = trimmed,
                                name = result.name,
                                brand = result.brand,
                                source = result.source,
                            )
                            message.value = "Producto encontrado en ${result.source}."
                        }
                        ProductCatalogResult.NotFound -> message.value = "Producto no encontrado en el catálogo."
                        ProductCatalogResult.Unavailable -> message.value = "Sin conexión. Puedes continuar manualmente."
                    }
                }
                .onFailure { message.value = "No se pudo buscar el producto." }
            isLookingUp.value = false
        }
    }

    fun confirmBarcodeMatch() {
        val pending = pendingBarcodeMatch.value ?: return
        linkKnownProduct(pending.itemId, pending.barcode, pending.knownProduct)
        pendingBarcodeMatch.value = null
    }

    fun rejectBarcodeMatch() {
        val pending = pendingBarcodeMatch.value ?: return
        newScannedProduct.value = NewScannedProduct(
            pending.barcode, pending.knownProduct.name, pending.knownProduct.lastPriceInCents,
        )
        pendingBarcodeMatch.value = null
    }

    private fun linkKnownProduct(itemId: Long, barcode: String, known: KnownProduct) {
        val item = state.value.details?.items?.firstOrNull { it.id == itemId } ?: return
        matchedScannedProduct.value = MatchedScannedProduct(
            item = item,
            barcode = barcode,
            suggestedName = known.name,
            suggestedPriceInCents = item.actualUnitPriceInCents ?: known.lastPriceInCents,
        )
    }

    fun finalizePurchase(
        categoryId: Long?,
        date: LocalDate,
        paymentMethod: ShoppingPaymentMethod,
        allowMissingPrices: Boolean = false,
    ) {
        if (categoryId == null) { message.value = "Selecciona una categoría de gasto."; return }
        if (isSaving.value) return
        pendingFinalizeCategoryId = categoryId
        pendingFinalizeDate = date
        pendingFinalizePaymentMethod = paymentMethod
        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.finalizePurchase(listId, categoryId, date, paymentMethod, allowMissingPrices) }
                .onSuccess { result ->
                    when (result) {
                        is FinalizePurchaseResult.Completed -> {
                            message.value = if (paymentMethod == ShoppingPaymentMethod.CREDIT) {
                                "Compra finalizada y agregada a Por pagar."
                            } else {
                                "Compra finalizada y gasto registrado."
                            }
                            missingPriceItemIds.value = emptyList()
                            runCatching { updateAllFinanceWidgets(context) }
                            pendingFinalizeCategoryId = null
                            pendingFinalizeDate = null
                            pendingFinalizePaymentMethod = null
                            purchaseCompleted.value = true
                        }
                        is FinalizePurchaseResult.MissingActualPrices -> {
                            missingPriceItemIds.value = result.itemIds
                            message.value = "Hay productos comprados sin precio real."
                        }
                        is FinalizePurchaseResult.AlreadyCompleted -> message.value = "Esta compra ya fue finalizada."
                        FinalizePurchaseResult.ListNotFound -> message.value = "La lista ya no existe."
                        FinalizePurchaseResult.InvalidExpenseCategory -> message.value = "Selecciona una categoría de gasto activa."
                        FinalizePurchaseResult.TotalNotPositive -> message.value = "El total de la compra debe ser mayor que cero."
                        FinalizePurchaseResult.CalculationOverflow -> message.value = "El total es demasiado grande para procesarlo."
                    }
                }.onFailure { message.value = "No se pudo finalizar la compra." }
            isSaving.value = false
        }
    }

    fun forceFinalizePurchase() {
        val categoryId = pendingFinalizeCategoryId
        if (categoryId == null) {
            message.value = "Selecciona nuevamente la categoría del gasto."
            return
        }
        missingPriceItemIds.value = emptyList()
        finalizePurchase(
            categoryId,
            pendingFinalizeDate ?: LocalDate.now(),
            pendingFinalizePaymentMethod ?: ShoppingPaymentMethod.DEBIT,
            allowMissingPrices = true,
        )
    }

    fun updatePurchaseSettings(categoryId: Long?, date: LocalDate, paymentMethod: ShoppingPaymentMethod) {
        if (categoryId == null) {
            message.value = "Selecciona una categoría de gasto."
            return
        }
        mutate("Compra y registro financiero actualizados.") {
            repository.updatePurchaseSettings(listId, categoryId, date, paymentMethod)
        }
    }

    private fun saveItemCopy(item: ShoppingListItem, success: String?) =
        if (hasSafeTotals(item)) mutate(success) { repository.saveItem(item) }
        else Unit.also { message.value = "El precio o la cantidad son demasiado grandes." }

    private fun hasSafeTotals(candidate: ShoppingListItem): Boolean {
        val details = state.value.details ?: return false
        val items = details.items.filterNot { it.id != 0L && it.id == candidate.id } + candidate
        return runCatching {
            details.copy(items = items).estimatedSubtotalInCents
            details.copy(items = items).actualTotalInCents
        }.isSuccess
    }

    private fun hasSafeAdjustments(
        candidates: List<ShoppingAdjustment>,
        replaceExisting: Boolean,
    ): Boolean {
        val details = state.value.details ?: return false
        val candidateIds = candidates.mapTo(mutableSetOf()) { it.id }.apply { remove(0L) }
        val existing = if (replaceExisting) {
            details.adjustments.filterNot { it.id in candidateIds }
        } else {
            details.adjustments
        }
        return runCatching { details.copy(adjustments = existing + candidates).actualTotalInCents }.isSuccess
    }

    private fun mutate(success: String?, onSuccess: () -> Unit = {}, operation: suspend () -> ShoppingMutationResult) {
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            runCatching { operation() }.onSuccess { result ->
                message.value = when (result) {
                    is ShoppingMutationResult.Success -> success
                    ShoppingMutationResult.NotFound -> "El elemento ya no existe."
                    ShoppingMutationResult.CompletedList -> "La lista completada es de solo lectura."
                }
                if (result is ShoppingMutationResult.Success) onSuccess()
                if (result is ShoppingMutationResult.Success) runCatching { updateAllFinanceWidgets(context) }
            }.onFailure { message.value = "No se pudo guardar el cambio." }
            isSaving.value = false
        }
    }

    private fun parseOptionalPrice(raw: String): Long? = when {
        raw.isBlank() -> null
        else -> MoneyFormatter.parseToCents(raw)?.takeIf { it >= 0 }
    }
}
