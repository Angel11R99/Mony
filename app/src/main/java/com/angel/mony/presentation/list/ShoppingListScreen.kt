package com.angel.mony.presentation.list

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.core.showToast
import com.angel.mony.domain.model.ListOcrMoneyParser
import com.angel.mony.domain.model.ListTicketParser
import com.angel.mony.domain.model.OcrMoneyCandidate
import com.angel.mony.domain.model.ShoppingAdjustment
import com.angel.mony.domain.model.ShoppingListDetails
import com.angel.mony.domain.model.ShoppingListItem
import com.angel.mony.domain.model.ShoppingListStatus
import com.angel.mony.domain.model.ShoppingPaymentMethod
import com.angel.mony.domain.model.RecognitionConfidence
import com.angel.mony.domain.model.TicketAmountKind
import com.angel.mony.domain.model.TicketOcrLine
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceCard
import com.angel.mony.presentation.components.FinanceDetailRow
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.FormState
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DocumentScanPurpose { PRICE, TICKET }

private data class ItemEditorData(
    val item: ShoppingListItem?,
    val barcode: String = "",
    val name: String = "",
    val actual: String = "",
    val initialPurchased: Boolean = false,
    val editorKey: Long = System.nanoTime(),
)
private data class TicketAdjustmentUi(val draft: AdjustmentDraft, val included: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val barcodeMatch by viewModel.pendingBarcodeMatch.collectAsStateWithLifecycle()
    val scannedProduct by viewModel.newScannedProduct.collectAsStateWithLifecycle()
    val matchedScannedProduct by viewModel.matchedScannedProduct.collectAsStateWithLifecycle()
    val missingPrices by viewModel.missingPriceItemIds.collectAsStateWithLifecycle()
    val purchaseCompleted by viewModel.purchaseCompleted.collectAsStateWithLifecycle()
    val remoteLookup by viewModel.remoteLookupResult.collectAsStateWithLifecycle()
    val isLookingUp by viewModel.isLookingUp.collectAsStateWithLifecycle()
    val ticketReview by viewModel.ticketReview.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var itemEditor by remember { mutableStateOf<ItemEditorData?>(null) }
    var adjustmentEditor by remember { mutableStateOf<ShoppingAdjustment?>(null) }
    var showAdjustmentEditor by remember { mutableStateOf(false) }
    var showListEditor by remember { mutableStateOf(false) }
    var showFinalize by remember { mutableStateOf(false) }
    var priceTarget by remember { mutableStateOf<ShoppingListItem?>(null) }
    var scanPurpose by remember { mutableStateOf<DocumentScanPurpose?>(null) }
    var priceCandidates by remember { mutableStateOf<List<OcrMoneyCandidate>>(emptyList()) }
    var ocrExtractedName by remember { mutableStateOf("") }
    var reviewingMissingPrices by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<ShoppingListItem?>(null) }
    var pendingDeleteAdjustment by remember { mutableStateOf<ShoppingAdjustment?>(null) }
    var lastNewItemPurchased by rememberSaveable { mutableStateOf(false) }

    fun notify(text: String) = context.showToast(text)

    fun processDocument(uri: Uri, purpose: DocumentScanPurpose) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        runCatching { InputImage.fromFilePath(context, uri) }
            .onFailure { recognizer.close(); notify("No se pudo leer la imagen seleccionada.") }
            .onSuccess { image ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        when (purpose) {
                            DocumentScanPurpose.PRICE -> {
                                priceCandidates = ListOcrMoneyParser.extractCandidates(result.text)
                                ocrExtractedName = extractProductNameFromOcr(result.text)
                                if (priceCandidates.isEmpty()) notify("No se encontraron precios en la imagen.")
                            }
                            DocumentScanPurpose.TICKET -> {
                                val parsed = ListTicketParser.parse(result.text, result.toTicketOcrLines()).candidates
                                viewModel.prepareTicketReview(parsed)
                                if (parsed.isEmpty()) notify("No se encontraron montos en el ticket.")
                            }
                        }
                    }
                    .addOnFailureListener { notify("No se pudo reconocer el texto de la imagen.") }
                    .addOnCompleteListener { recognizer.close() }
            }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val purpose = scanPurpose
        scanPurpose = null
        if (uri == null || purpose == null) notify("Selección de imagen cancelada.")
        else processDocument(uri, purpose)
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val purpose = scanPurpose
        scanPurpose = null
        if (result.resultCode != Activity.RESULT_OK) {
            notify("Escaneo cancelado.")
        } else {
            val page = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()
            if (page == null || purpose == null) notify("El escáner no devolvió una imagen.")
            else processDocument(page.imageUri, purpose)
        }
    }

    fun launchDocumentScanner(purpose: DocumentScanPurpose) {
        val activity = context.findActivity()
        if (activity == null) { notify("No se pudo abrir el escáner."); return }
        scanPurpose = purpose
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        runCatching { GmsDocumentScanning.getClient(options) }
            .onFailure {
                notify("El escáner no está disponible. Selecciona una imagen.")
                imagePicker.launch("image/*")
            }
            .onSuccess { scanner ->
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { sender ->
                        documentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                    .addOnFailureListener {
                        notify("Google Play Services no pudo iniciar el escáner. Selecciona una imagen.")
                        imagePicker.launch("image/*")
                    }
            }
    }

    fun launchBarcodeScanner() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE,
        ).build()
        runCatching { GmsBarcodeScanning.getClient(context, options) }
            .onFailure { notify("El escáner de códigos no está disponible.") }
            .onSuccess { scanner -> scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.takeIf(String::isNotBlank)?.let(viewModel::onBarcodeScanned)
                        ?: notify("El escáner no devolvió un código.")
                }
                .addOnCanceledListener { notify("Escaneo cancelado.") }
                .addOnFailureListener { notify("No se pudo escanear el código.") }
            }
    }

    LaunchedEffect(message) { message?.let { notify(it); viewModel.consumeMessage() } }
    LaunchedEffect(scannedProduct) {
        scannedProduct?.let {
            itemEditor = ItemEditorData(
                item = null,
                barcode = it.barcode,
                name = it.suggestedName,
                actual = it.suggestedPriceInCents?.let(::centsInput).orEmpty(),
                initialPurchased = lastNewItemPurchased,
            )
            viewModel.clearScannedProduct()
        }
    }
    LaunchedEffect(purchaseCompleted) {
        if (purchaseCompleted) {
            showFinalize = false
            reviewingMissingPrices = false
            viewModel.consumePurchaseCompleted()
        }
    }
    LaunchedEffect(missingPrices, itemEditor, reviewingMissingPrices) {
        if (reviewingMissingPrices && itemEditor == null) {
            val item = state.details?.items?.firstOrNull { it.id in missingPrices }
            if (item == null) {
                reviewingMissingPrices = false
                if (missingPrices.isEmpty()) viewModel.forceFinalizePurchase()
            } else {
                itemEditor = ItemEditorData(item)
            }
        }
    }
    LaunchedEffect(matchedScannedProduct) {
        matchedScannedProduct?.let {
            itemEditor = ItemEditorData(
                item = it.item,
                barcode = it.barcode,
                name = it.suggestedName,
                actual = it.suggestedPriceInCents?.let(::centsInput).orEmpty(),
            )
            viewModel.clearMatchedScannedProduct()
        }
    }
    LaunchedEffect(remoteLookup) {
        remoteLookup?.let { lookup ->
            val current = itemEditor
            if (current != null && current.barcode == lookup.barcode && current.name.isBlank()) {
                itemEditor = current.copy(name = lookup.name)
            }
            viewModel.consumeRemoteLookupResult()
        }
    }

    val details = state.details
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onBack, Modifier.size(54.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
                title = { Text(details?.list?.name ?: "Lista de compra", maxLines = 1) },
                actions = {
                    if (state.editable && details != null) {
                        IconButton({ showListEditor = true }, enabled = !isSaving, modifier = Modifier.size(54.dp)) {
                            Icon(Icons.Outlined.Edit, "Editar lista")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (details?.list?.status == ShoppingListStatus.SHOPPING) {
                ShoppingBottomSummary(details, isSaving) { showFinalize = true }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally) {
                CircularProgressIndicator(); Text("Cargando lista…", Modifier.padding(top = 12.dp))
            }
            state.hasError -> CenterMessage("No se pudo cargar la lista.", Modifier.padding(padding))
            details == null -> CenterMessage("La lista ya no existe.", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ListHeaderCard(details) }
                if (state.editable && details.list.status != ShoppingListStatus.COMPLETED) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickAction("Producto", Icons.Outlined.Add, Modifier.weight(1f)) {
                                itemEditor = ItemEditorData(null, initialPurchased = lastNewItemPurchased)
                            }
                            QuickAction("Código", Icons.Outlined.QrCodeScanner, Modifier.weight(1f), onClick = ::launchBarcodeScanner)
                            QuickAction("Ticket", Icons.Outlined.DocumentScanner, Modifier.weight(1f)) { launchDocumentScanner(DocumentScanPurpose.TICKET) }
                        }
                    }
                }
                if (details.items.isEmpty()) item { CenterMessage("Agrega los productos que necesitas comprar.") }
                else {
                    item { Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                    items(details.items, key = { it.id }) { item ->
                        ShoppingItemCard(
                            item, state.editable, isSaving,
                            onEdit = { itemEditor = ItemEditorData(item) },
                            onMinus = { viewModel.changeQuantity(item, -1) },
                            onPlus = { viewModel.changeQuantity(item, 1) },
                            onToggle = { viewModel.togglePurchased(item) },
                            onDelete = { pendingDeleteItem = item },
                            onOcr = { priceTarget = item; launchDocumentScanner(DocumentScanPurpose.PRICE) },
                        )
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ajustes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (state.editable) IconButton({ adjustmentEditor = null; showAdjustmentEditor = true }) { Icon(Icons.Outlined.Add, "Agregar ajuste") }
                    }
                }
                if (details.adjustments.isEmpty()) item { Text("Sin impuestos, descuentos u otros ajustes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(details.adjustments, key = { it.id }) { adjustment ->
                    AdjustmentCard(adjustment, state.editable, {
                        adjustmentEditor = adjustment; showAdjustmentEditor = true
                    }, { pendingDeleteAdjustment = adjustment })
                }
                item { TotalsCard(details) }
                if (details.list.status == ShoppingListStatus.PENDING && state.editable) {
                    item { PrimaryButton("Iniciar compra", viewModel::startShopping, Modifier.fillMaxWidth(), enabled = !isSaving) }
                }
                if (details.list.status == ShoppingListStatus.COMPLETED) {
                    item { SecondaryButton("Editar fecha y método de pago", { if (!isSaving) showFinalize = true }, Modifier.fillMaxWidth()) }
                }
            }
        }
    }

    if (showListEditor && details != null) ListEditorDialog(details, isSaving, { showListEditor = false }) { name, budget ->
        viewModel.updateList(name, budget) { showListEditor = false }
    }
    itemEditor?.let { data -> ItemEditorDialog(
        data, isSaving, isLookingUp, viewModel::lookupRemote, { itemEditor = null },
    ) { name, quantity, estimated, actual, barcode, notes, purchased ->
        viewModel.saveItem(data.item, name, quantity, estimated, actual, barcode, notes, purchased) {
            if (data.item == null) lastNewItemPurchased = purchased
            data.item?.let { viewModel.markMissingPriceReviewed(it.id) }
            itemEditor = null
        }
    } }
    if (showAdjustmentEditor) AdjustmentEditorDialog(adjustmentEditor, isSaving, { showAdjustmentEditor = false }) { name, positive, amount ->
        viewModel.saveAdjustment(adjustmentEditor, name, positive, amount) { showAdjustmentEditor = false }
    }
    barcodeMatch?.let { match -> AlertDialog(
        onDismissRequest = viewModel::cancelBarcodeMatch,
        title = { Text("Posible coincidencia") },
        text = { Text("¿Este producto corresponde a “${match.itemName}” de tu lista?") },
        confirmButton = { TextButton(viewModel::confirmBarcodeMatch) { Text("Sí, vincular") } },
        dismissButton = { TextButton(viewModel::rejectBarcodeMatch) { Text("Es diferente") } },
    ) }
    if (priceCandidates.isNotEmpty()) PriceCandidatesDialog(priceCandidates, { priceCandidates = emptyList() }) { candidate ->
        val item = priceTarget
        if (item != null) {
            val itemName = item.name.ifBlank { ocrExtractedName }
            itemEditor = ItemEditorData(item, name = itemName, actual = centsInput(candidate.amountInCents))
        }
        priceCandidates = emptyList(); priceTarget = null; ocrExtractedName = ""
    }
    ticketReview?.let { review -> TicketReviewDialog(
        review = review,
        existingItems = details?.items.orEmpty(),
        saving = isSaving,
        dismiss = viewModel::clearTicketReview,
        save = viewModel::applyTicketReview,
    ) }
    if (showFinalize && details != null) FinalizeDialog(
        details, state.categories, state.defaultCategoryId, isSaving, { showFinalize = false },
    ) { categoryId, date, method ->
        if (details.list.status == ShoppingListStatus.COMPLETED) {
            viewModel.updatePurchaseSettings(categoryId, date, method)
            showFinalize = false
        } else {
            viewModel.finalizePurchase(categoryId, date, method)
        }
    }
    if (missingPrices.isNotEmpty()) MissingPricesDialog(
        count = missingPrices.size,
        onReview = {
            reviewingMissingPrices = true
            itemEditor = details?.items?.firstOrNull { it.id in missingPrices }?.let { ItemEditorData(it) }
        },
        onForce = {
            reviewingMissingPrices = false
            viewModel.forceFinalizePurchase()
        },
        onDismiss = {
            reviewingMissingPrices = false
            viewModel.clearMissingPrices()
        },
    )
    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar “${item.name}” de la lista?") },
            confirmButton = { TextButton({ viewModel.deleteItem(item); pendingDeleteItem = null }) { Text("Eliminar") } },
            dismissButton = { TextButton({ pendingDeleteItem = null }) { Text("Cancelar") } },
        )
    }
    pendingDeleteAdjustment?.let { adjustment ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAdjustment = null },
            title = { Text("Eliminar ajuste") },
            text = { Text("¿Eliminar el ajuste “${adjustment.name}”?") },
            confirmButton = { TextButton({ viewModel.deleteAdjustment(adjustment); pendingDeleteAdjustment = null }) { Text("Eliminar") } },
            dismissButton = { TextButton({ pendingDeleteAdjustment = null }) { Text("Cancelar") } },
        )
    }
}

@Composable private fun CenterMessage(text: String, modifier: Modifier = Modifier) {
    FinanceCard(modifier.fillMaxWidth()) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable private fun QuickAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.heightIn(min = 54.dp).clickable(onClick = onClick), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(22.dp)); Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun ListHeaderCard(details: ShoppingListDetails) {
    FinanceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        FinanceDetailRow("Estado", details.list.status.spanishLabel())
        FinanceDetailRow(
            "Creada",
            details.list.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(shoppingDateFormatter),
        )
        FinanceDetailRow("Productos", details.items.size.toString())
        details.list.budgetInCents?.let { FinanceDetailRow("Presupuesto", MoneyFormatter.format(it)) }
        details.list.purchaseDate?.let { FinanceDetailRow("Fecha de compra", it.format(shoppingDateFormatter)) }
        details.list.paymentMethod?.let { FinanceDetailRow("Método de pago", it.spanishLabel()) }
    } }
}

@Composable private fun ShoppingItemCard(
    item: ShoppingListItem, editable: Boolean, isSaving: Boolean, onEdit: () -> Unit, onMinus: () -> Unit,
    onPlus: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit, onOcr: () -> Unit,
) {
    FinanceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(item.isPurchased, onCheckedChange = if (editable) {{ onToggle() }} else null, enabled = !isSaving)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Cantidad: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (editable) {
                IconButton(onEdit, enabled = !isSaving) { Icon(Icons.Outlined.Edit, "Editar producto") }
                IconButton(onDelete, enabled = !isSaving) { Icon(Icons.Outlined.Delete, "Eliminar producto") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editable) {
                IconButton(onMinus, enabled = !isSaving, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Remove, "Reducir cantidad") }
                IconButton(onPlus, enabled = !isSaving, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Add, "Aumentar cantidad") }
            }
            Column(Modifier.weight(1f)) {
                item.estimatedUnitPriceInCents?.let { Text("Estimado: ${MoneyFormatter.format(it)} c/u", style = MaterialTheme.typography.bodySmall) }
                item.actualUnitPriceInCents?.let { Text("Real: ${MoneyFormatter.format(it)} c/u", style = MaterialTheme.typography.bodySmall) }
                    ?: if (item.isPurchased) Text("Precio real pendiente", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) else Unit
            }
            item.actualUnitPriceInCents?.let {
                Text("Subtotal: ${MoneyFormatter.format(Math.multiplyExact(item.quantity.toLong(), it))}", fontWeight = FontWeight.SemiBold)
            }
            if (editable) IconButton(onOcr, enabled = !isSaving, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.PointOfSale, "Leer precio") }
        }
        item.barcode?.let { Text("Código: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    } }
}

@Composable private fun AdjustmentCard(adjustment: ShoppingAdjustment, editable: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    FinanceCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(adjustment.name, Modifier.weight(1f))
        Text((if (adjustment.isPositive) "+" else "−") + MoneyFormatter.format(adjustment.amountInCents), fontWeight = FontWeight.SemiBold)
        if (editable) {
            IconButton(onEdit) { Icon(Icons.Outlined.Edit, "Editar ajuste") }
            IconButton(onDelete) { Icon(Icons.Outlined.Delete, "Eliminar ajuste") }
        }
    } }
}

@Composable private fun TotalsCard(details: ShoppingListDetails) {
    FinanceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FinanceDetailRow("Subtotal comprado", MoneyFormatter.format(details.purchasedSubtotalInCents))
        FinanceDetailRow("Ajustes", MoneyFormatter.format(details.adjustmentTotalInCents))
        FinanceDetailRow("Total", MoneyFormatter.format(details.actualTotalInCents), valueColor = MaterialTheme.colorScheme.primary)
        details.list.budgetInCents?.let { budget ->
            FinanceDetailRow("Gastado", MoneyFormatter.format(details.actualTotalInCents))
            val remaining = budget - details.actualTotalInCents
            FinanceDetailRow(if (remaining >= 0) "Disponible" else "Exceso", MoneyFormatter.format(kotlin.math.abs(remaining)), valueColor = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    } }
}

@Composable private fun ShoppingBottomSummary(details: ShoppingListDetails, isSaving: Boolean, onFinalize: () -> Unit) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            val purchased = details.items.count { it.isPurchased }
            Row { Text("${details.items.size} producto${if (details.items.size == 1) "" else "s"}", Modifier.weight(1f)); Text(MoneyFormatter.format(details.finalizableTotalInCents), fontWeight = FontWeight.Bold) }
            if (purchased < details.items.size) {
                Text("Al finalizar, los productos pendientes se marcarán como comprados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal ${MoneyFormatter.format(details.finalizableSubtotalInCents)}", style = MaterialTheme.typography.labelSmall)
                Text("Ajustes ${MoneyFormatter.format(details.adjustmentTotalInCents)}", style = MaterialTheme.typography.labelSmall)
                Text("Total ${MoneyFormatter.format(details.finalizableTotalInCents)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            details.list.budgetInCents?.let {
                val remaining = it - details.actualTotalInCents
                Text(if (remaining >= 0) "Disponible: ${MoneyFormatter.format(remaining)}" else "Exceso: ${MoneyFormatter.format(-remaining)}", color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            PrimaryButton("Finalizar compra", onFinalize, Modifier.fillMaxWidth(), enabled = !isSaving && details.items.isNotEmpty())
        }
    }
}

@Composable private fun ListEditorDialog(details: ShoppingListDetails, saving: Boolean, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by remember(details.list.id) { mutableStateOf(details.list.name) }
    var budget by remember(details.list.id) { mutableStateOf(details.list.budgetInCents?.let(::centsInput).orEmpty()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Editar lista") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceTextField(name, { name = it }, "Nombre", singleLine = true)
        MoneyField(budget, { budget = it }, "Presupuesto (opcional)")
    } }, confirmButton = { TextButton({ save(name, budget) }, enabled = !saving) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable private fun ItemEditorDialog(
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
             item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Cantidad", Modifier.weight(1f)); IconButton({ quantity = (quantity - 1).coerceAtLeast(1) }) { Icon(Icons.Outlined.Remove, "Reducir") }; Text(quantity.toString()); IconButton({ if (quantity < Int.MAX_VALUE) quantity++ }) { Icon(Icons.Outlined.Add, "Aumentar") } } }
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

@Composable private fun AdjustmentEditorDialog(existing: ShoppingAdjustment?, saving: Boolean, dismiss: () -> Unit, save: (String, Boolean, String) -> Unit) {
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

@Composable private fun PriceCandidatesDialog(candidates: List<OcrMoneyCandidate>, dismiss: () -> Unit, select: (OcrMoneyCandidate) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text("Selecciona el precio") }, text = { LazyColumn {
        items(candidates) { candidate -> Row(Modifier.fillMaxWidth().clickable { select(candidate) }.padding(vertical = 12.dp)) {
            Text(candidate.rawValue, Modifier.weight(1f)); Text(MoneyFormatter.format(candidate.amountInCents), fontWeight = FontWeight.SemiBold)
        } }
    } }, confirmButton = {}, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@Composable private fun TicketReviewDialog(
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
    val calculated = runCatching {
        products.filter { it.included }.fold(0L) { total, product ->
            Math.addExact(total, Math.multiplyExact(product.quantity.toLong(), product.unitPriceInCents))
        } + adjustments.filter { it.included }.fold(0L) { total, adjustment ->
            Math.addExact(total, if (adjustment.draft.isPositive) adjustment.draft.amountInCents else -adjustment.draft.amountInCents)
        }
    }.getOrDefault(0L)
    val discrepancy = review.ticketTotalInCents?.let { kotlin.math.abs(it - calculated) }
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
                            IconButton({ if (product.quantity > 1) products = products.updated(index, product.copy(quantity = product.quantity - 1)) }) { Icon(Icons.Outlined.Remove, "Reducir") }
                            Text(product.quantity.toString())
                            IconButton({ if (product.quantity < Int.MAX_VALUE) products = products.updated(index, product.copy(quantity = product.quantity + 1)) }) { Icon(Icons.Outlined.Add, "Aumentar") }
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
            item { FinanceDetailRow("Total calculado", MoneyFormatter.format(calculated)) }
            review.ticketTotalInCents?.let { total -> item { FinanceDetailRow("Total del ticket", MoneyFormatter.format(total)) } }
            if (discrepancy != null && discrepancy > maxOf(100L, (review.ticketTotalInCents ?: 0L) / 100)) {
                item { Text("Advertencia: el total calculado difiere del total reconocido en el ticket.", color = MaterialTheme.colorScheme.error) }
            }
        } },
        confirmButton = { TextButton({ save(products, adjustments.filter { it.included }.map { it.draft }) }, enabled = !saving && (products.any { it.included } || adjustments.any { it.included }) ) { Text("Aplicar") } },
        dismissButton = { TextButton(dismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FinalizeDialog(details: ShoppingListDetails, categories: List<com.angel.mony.domain.model.Category>, initialCategory: Long?, saving: Boolean, dismiss: () -> Unit, finalize: (Long?, LocalDate, ShoppingPaymentMethod) -> Unit) {
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

@Composable private fun MissingPricesDialog(count: Int, onReview: () -> Unit, onForce: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Faltan precios reales") }, text = { Text("$count producto(s) comprado(s) no tienen precio real. El gasto no puede calcularse con precisión.") }, confirmButton = { TextButton(onReview) { Text("Revisar") } }, dismissButton = { Row { TextButton(onForce) { Text("Finalizar de todos modos") }; TextButton(onDismiss) { Text("Cancelar") } } })
}

private fun centsInput(cents: Long): String = BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()

private val shoppingDateFormatter = DateTimeFormatter.ofPattern(
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

private fun ShoppingPaymentMethod.spanishLabel(): String = when (this) {
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun extractProductNameFromOcr(text: String): String {
    val skipPatterns = listOf(
        Regex("(?i)^\\s*(RD\\$|\\$)"),
        Regex("^\\s*\\d+[.,]\\d{2}\\s*$"),
        Regex("^\\s*\\d+\\s*$"),
        Regex("(?i)^\\s*(compartir|añadir|carrito|agregar|share|add|cart)\\s*$"),
        Regex("(?i)^\\s*(producto|product|precio|price|total|subtotal|itbis)\\s*$"),
    )
    val meaningfulLines = text.lines()
        .map { it.trim() }
        .filter { it.length >= 3 }
        .filter { line -> skipPatterns.none { it.containsMatchIn(line) } }
    return meaningfulLines.maxByOrNull { it.length }?.trim().orEmpty()
}

private fun com.google.mlkit.vision.text.Text.toTicketOcrLines(): List<TicketOcrLine> =
    textBlocks.flatMapIndexed { blockIndex, block ->
        block.lines.mapIndexedNotNull { lineIndex, line ->
            line.boundingBox?.let { bounds ->
                TicketOcrLine(line.text, bounds.left, bounds.top, bounds.right, bounds.bottom, blockIndex, lineIndex)
            }
        }
    }
