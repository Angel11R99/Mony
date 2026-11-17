package com.angel.mony.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.OcrMoneyCandidate
import com.angel.mony.domain.model.ShoppingAdjustment
import com.angel.mony.domain.model.ShoppingListDetails
import com.angel.mony.domain.model.ShoppingListItem
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.ShoppingPaymentMethod
import com.angel.mony.domain.model.RecognitionConfidence
import com.angel.mony.domain.model.TicketAmountKind
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TicketAdjustmentUi(val draft: AdjustmentDraft, val included: Boolean = true)

@Composable internal fun ListEditorDialog(details: ShoppingListDetails, saving: Boolean, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by remember(details.list.id) { mutableStateOf(details.list.name) }
    var budget by remember(details.list.id) { mutableStateOf(details.list.budgetInCents?.let(::centsInput).orEmpty()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Editar lista") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
        MoneyField(budget, { budget = it }, "Presupuesto (opcional)")
    } }, confirmButton = { TextButton({ save(name, budget) }, enabled = !saving) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable internal fun ItemEditorDialog(
    data: ItemEditorData, saving: Boolean, isLookingUp: Boolean,
    onLookup: (String) -> Unit, dismiss: () -> Unit,
    save: (String, Int, String, String, String, String, Boolean) -> Unit,
) {
    val item = data.item
    var name by remember(data) { mutableStateOf(data.name.ifBlank { item?.name.orEmpty() }) }
    var quantity by remember(data) { mutableStateOf(item?.quantity ?: 1) }
    var estimated by remember(data) { mutableStateOf(item?.estimatedUnitPriceInCents?.let(::centsInput).orEmpty()) }
    var actual by remember(data) { mutableStateOf(data.actual.ifBlank { item?.actualUnitPriceInCents?.let(::centsInput).orEmpty() }) }
    var barcode by remember(data) { mutableStateOf(data.barcode.ifBlank { item?.barcode.orEmpty() }) }
    var notes by remember(data) { mutableStateOf(item?.notes.orEmpty()) }
    var purchased by remember(data) { mutableStateOf(item?.isPurchased ?: data.initialPurchased) }
    val formState = remember { FormState() }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (item == null) "Agregar producto" else "Editar producto") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 520.dp)) {
            item {
                FinanceTextField(
                    name,
                    { name = it; formState.clearError("name") },
                    "Nombre",
                    singleLine = true,
                    isError = formState.hasError("name"),
                    errorMessage = formState["name"],
                )
            }
             item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Cantidad", Modifier.weight(1f)); IconButton({ quantity = (quantity - 1).coerceAtLeast(1) }) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Remove, "Reducir") }; Text(quantity.toString()); IconButton({ if (quantity < Int.MAX_VALUE) quantity++ }) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Add, "Aumentar") } } }
            item { MoneyField(estimated, { estimated = it }, "Precio estimado (opcional)") }
            item { MoneyField(actual, { actual = it }, "Precio real (opcional)") }
            item { FinanceTextField(barcode, { barcode = it.filter(Char::isLetterOrDigit) }, "Código de barras (opcional)", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)) }
            item { FinanceTextField(notes, { notes = it }, "Notas (opcional)") }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Comprado", Modifier.weight(1f)); Switch(purchased, { purchased = it }) } }
            if (data.barcode.isNotBlank() && data.name.isBlank()) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLookingUp) {
                        SecondaryButton("Buscando…", {}, Modifier.weight(1f))
                    } else {
                        SecondaryButton("Buscar en Internet", { onLookup(barcode) }, Modifier.weight(1f))
                    }
                } }
                item { Text("Código no reconocido. Escribe el nombre o busca en Internet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }, confirmButton = { TextButton({
        formState.clearAll()
        if (name.isBlank()) formState.setError("name", "Escribe un nombre")
        if (!formState.isValid()) return@TextButton
        save(name, quantity, estimated, actual, barcode, notes, purchased)
    }, enabled = !saving) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable internal fun AdjustmentEditorDialog(existing: ShoppingAdjustment?, saving: Boolean, dismiss: () -> Unit, save: (String, Boolean, String) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var positive by remember(existing?.id) { mutableStateOf(existing?.isPositive ?: true) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.amountInCents?.let(::centsInput).orEmpty()) }
    val formState = remember { FormState() }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) "Agregar ajuste" else "Editar ajuste") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceTextField(
            name,
            { name = it; formState.clearError("name") },
            "Nombre",
            singleLine = true,
            isError = formState.hasError("name"),
            errorMessage = formState["name"],
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(positive, { positive = true }, { Text("Sumar +") }); FilterChip(!positive, { positive = false }, { Text("Restar −") }) }
        MoneyField(amount, { amount = it }, "Monto")
    } }, confirmButton = { TextButton({
        formState.clearAll()
        if (name.isBlank()) formState.setError("name", "Escribe un nombre")
        if (!formState.isValid()) return@TextButton
        save(name, positive, amount)
    }, enabled = !saving) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable private fun MoneyField(value: String, changed: (String) -> Unit, label: String) = FinanceTextField(
    value, { changed(sanitizeAmountInput(it)) }, label, singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = AmountVisualTransformation,
)

@Composable internal fun PriceCandidatesDialog(candidates: List<OcrMoneyCandidate>, dismiss: () -> Unit, select: (OcrMoneyCandidate) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text("Selecciona el precio") }, text = { LazyColumn {
        items(candidates) { candidate -> Row(Modifier.fillMaxWidth().clickable { select(candidate) }.padding(vertical = 12.dp)) {
            Text(candidate.rawValue, Modifier.weight(1f)); Text(MoneyFormatter.format(candidate.amountInCents), fontWeight = FontWeight.SemiBold)
        } }
    } }, confirmButton = {}, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable internal fun TicketReviewDialog(
    review: TicketReview,
    existingItems: List<ShoppingListItem>,
    saving: Boolean,
    dismiss: () -> Unit,
    save: (List<TicketProductDraft>, List<AdjustmentDraft>) -> Unit,
) {
    var products by remember(review) { mutableStateOf(review.products) }
    var adjustments by remember(review) {
        mutableStateOf(review.adjustments.map { TicketAdjustmentUi(AdjustmentDraft(it.kind.spanishLabel(), it.kind != TicketAmountKind.DISCOUNT, it.amountInCents)) })
    }
    val calculated = calculateTicketReviewTotal(
        products = products,
        adjustments = adjustments.filter { it.included }.map { it.draft },
    )
    val discrepancy = review.ticketTotalInCents?.let { total -> calculated?.let { kotlin.math.abs(total - it) } }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Revisar ticket") },
        text = { LazyColumn(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("PRODUCTOS DETECTADOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            items(products.size) { index ->
                val product = products[index]
                var expanded by remember(product.occurrenceId) { mutableStateOf(false) }
                FinanceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(product.included, { products = products.updated(index, product.copy(included = it)) })
                        Text(product.confidence.spanishLabel(), style = MaterialTheme.typography.labelSmall, color = when (product.confidence) {
                            RecognitionConfidence.HIGH -> MaterialTheme.colorScheme.primary
                            RecognitionConfidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            RecognitionConfidence.LOW -> MaterialTheme.colorScheme.error
                        })
                    }
                    if (product.included) {
                        FinanceTextField(product.name, { products = products.updated(index, product.copy(name = it)) }, "Nombre", singleLine = true)
                        val selectedName = existingItems.firstOrNull { it.id == product.selectedItemId }?.name
                        SecondaryButton(selectedName?.let { "Coincidencia: $it" } ?: "Agregar como producto nuevo", { expanded = true }, Modifier.fillMaxWidth())
                        DropdownMenu(expanded, { expanded = false }) {
                            DropdownMenuItem({ Text("Producto nuevo") }, { products = products.updated(index, product.copy(selectedItemId = null)); expanded = false })
                            val usedIds = products.mapNotNull { it.selectedItemId }.toSet() - product.selectedItemId
                            existingItems.filterNot { it.isPurchased || it.id in usedIds }.forEach { item ->
                                DropdownMenuItem({ Text(item.name) }, { products = products.updated(index, product.copy(selectedItemId = item.id, name = item.name)); expanded = false })
                            }
                        }
                        product.suggestedItemId?.takeIf { product.selectedItemId == null }?.let { suggested ->
                            existingItems.firstOrNull { it.id == suggested }?.let { Text("Sugerencia: ${it.name}. Confirma manualmente.", style = MaterialTheme.typography.bodySmall) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cantidad", Modifier.weight(1f))
                            IconButton({ if (product.quantity > 1) products = products.updated(index, product.copy(quantity = product.quantity - 1)) }) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Remove, "Reducir") }
                            Text(product.quantity.toString())
                            IconButton({ if (product.quantity < Int.MAX_VALUE) products = products.updated(index, product.copy(quantity = product.quantity + 1)) }) { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Add, "Aumentar") }
                        }
                        MoneyField(centsInput(product.unitPriceInCents), { raw -> MoneyFormatter.parseToCents(raw)?.let { products = products.updated(index, product.copy(unitPriceInCents = it)) } }, "Precio unitario")
                        Text("Subtotal: ${MoneyFormatter.format(product.quantity.toLong() * product.unitPriceInCents)}", fontWeight = FontWeight.SemiBold)
                    }
                } }
            }
            item { Text("RESUMEN / AJUSTES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            items(adjustments.size) { index ->
                val adjustment = adjustments[index]
                FinanceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(adjustment.included, { adjustments = adjustments.updated(index, adjustment.copy(included = it)) })
                        Text(adjustment.draft.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    }
                    if (adjustment.included) {
                        FinanceTextField(adjustment.draft.name, { name -> adjustments = adjustments.updated(index, adjustment.copy(draft = adjustment.draft.copy(name = name))) }, "Nombre", singleLine = true)
                        Row {
                            FilterChip(adjustment.draft.isPositive, { adjustments = adjustments.updated(index, adjustment.copy(draft = adjustment.draft.copy(isPositive = true))) }, { Text("Sumar +") })
                            Spacer(Modifier.width(8.dp))
                            FilterChip(!adjustment.draft.isPositive, { adjustments = adjustments.updated(index, adjustment.copy(draft = adjustment.draft.copy(isPositive = false))) }, { Text("Restar −") })
                        }
                        MoneyField(centsInput(adjustment.draft.amountInCents), { raw -> MoneyFormatter.parseToCents(raw)?.let { amount -> adjustments = adjustments.updated(index, adjustment.copy(draft = adjustment.draft.copy(amountInCents = amount))) } }, "Monto")
                    }
                } }
            }
            item {
                FinanceDetailRow(
                    "Total calculado",
                    calculated?.let(MoneyFormatter::format) ?: "No disponible",
                )
            }
            review.ticketTotalInCents?.let { total -> item { FinanceDetailRow("Total del ticket", MoneyFormatter.format(total)) } }
            if (calculated == null) {
                item {
                    Text(
                        "No se pudo calcular el total por la cantidad de productos o ajustes. Revisa los importes antes de aplicar.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (discrepancy != null && discrepancy > maxOf(100L, (review.ticketTotalInCents ?: 0L) / 100)) {
                item { Text("Advertencia: el total calculado difiere del total reconocido en el ticket.", color = MaterialTheme.colorScheme.error) }
            }
        } },
        confirmButton = { TextButton({ save(products, adjustments.filter { it.included }.map { it.draft }) }, enabled = !saving && calculated != null && (products.any { it.included } || adjustments.any { it.included }) ) { Text("Aplicar") } },
        dismissButton = { TextButton(dismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun FinalizeDialog(details: ShoppingListDetails, categories: List<com.angel.mony.domain.model.Category>, initialCategory: Long?, saving: Boolean, dismiss: () -> Unit, finalize: (Long?, LocalDate, ShoppingPaymentMethod) -> Unit) {
    var categoryId by remember { mutableStateOf(details.list.expenseCategoryId ?: initialCategory) }
    var date by remember { mutableStateOf(details.list.purchaseDate ?: LocalDate.now()) }
    var paymentMethod by remember { mutableStateOf(details.list.paymentMethod ?: ShoppingPaymentMethod.DEBIT) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (details.list.status == ShoppingListStatus.COMPLETED) "Editar compra" else "Finalizar compra") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceDetailRow("Productos a finalizar", details.items.size.toString())
        FinanceDetailRow("Subtotal", MoneyFormatter.format(details.finalizableSubtotalInCents))
        if (details.items.any { !it.isPurchased }) {
            Text("Los productos pendientes se marcarán como comprados. Se solicitarán los precios reales que falten.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FinanceDetailRow("Ajustes", MoneyFormatter.format(details.adjustmentTotalInCents))
        FinanceDetailRow("Total del gasto", MoneyFormatter.format(details.finalizableTotalInCents), valueColor = MaterialTheme.colorScheme.primary)
        SecondaryButton("Fecha: ${date.format(shoppingDateFormatter)}", { showDatePicker = true }, Modifier.fillMaxWidth())
        Text("Método de pago", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShoppingPaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method },
                    label = { Text(method.shortLabel()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        if (paymentMethod == ShoppingPaymentMethod.CREDIT) Text("Se creará una obligación en Por pagar; no se registrará gasto hasta marcarla como pagada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column { SecondaryButton(categories.firstOrNull { it.id == categoryId }?.name ?: "Seleccionar categoría", { expanded = true }, Modifier.fillMaxWidth()); DropdownMenu(expanded, { expanded = false }) { categories.forEach { category -> DropdownMenuItem({ Text(category.name) }, { categoryId = category.id; expanded = false }) } } }
        if (categories.isEmpty()) Text("No hay categorías de gasto activas.", color = MaterialTheme.colorScheme.error)
    } }, confirmButton = { TextButton({ finalize(categoryId, date, paymentMethod) }, enabled = !saving && categoryId != null) { Text(if (details.list.status == ShoppingListStatus.COMPLETED) "Guardar" else "Finalizar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton({ picker.selectedDateMillis?.let { date = LocalDate.ofEpochDay(it / 86_400_000L) }; showDatePicker = false }) { Text("Aceptar") } },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(picker) }
    }
}

@Composable internal fun MissingPricesDialog(count: Int, onReview: () -> Unit, onForce: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Faltan precios reales") }, text = { Text("$count producto(s) comprado(s) no tienen precio real. El gasto no puede calcularse con precisión.") }, confirmButton = { TextButton(onReview) { Text("Revisar") } }, dismissButton = { Row { TextButton(onForce) { Text("Finalizar de todos modos") }; TextButton(onDismiss) { Text("Cancelar") } } })
}

internal fun centsInput(cents: Long): String = BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()

internal val shoppingDateFormatter = DateTimeFormatter.ofPattern(
    "d 'de' MMMM 'de' yyyy",
    Locale.forLanguageTag("es-DO"),
)

private fun TicketAmountKind.spanishLabel(): String = when (this) {
    TicketAmountKind.SUBTOTAL -> "Subtotal"
    TicketAmountKind.TAX -> "Impuesto"
    TicketAmountKind.DISCOUNT -> "Descuento"
    TicketAmountKind.SHIPPING -> "Envío"
    TicketAmountKind.SERVICE -> "Servicio o propina"
    TicketAmountKind.TOTAL -> "Total"
    TicketAmountKind.MONTO_DETECTADO -> "Monto detectado"
    TicketAmountKind.PRODUCTO -> "Producto"
}

private fun RecognitionConfidence.spanishLabel(): String = when (this) {
    RecognitionConfidence.HIGH -> "Coincidencia alta"
    RecognitionConfidence.MEDIUM -> "Revisar sugerencia"
    RecognitionConfidence.LOW -> "Requiere revisión"
}

internal fun ShoppingPaymentMethod.spanishLabel(): String = when (this) {
    ShoppingPaymentMethod.CASH -> "Efectivo"
    ShoppingPaymentMethod.DEBIT -> "Débito"
    ShoppingPaymentMethod.CREDIT -> "Crédito"
}

private fun ShoppingPaymentMethod.shortLabel(): String = when (this) {
    ShoppingPaymentMethod.CASH -> "Efectivo"
    ShoppingPaymentMethod.DEBIT -> "Débito"
    ShoppingPaymentMethod.CREDIT -> "Crédito"
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

