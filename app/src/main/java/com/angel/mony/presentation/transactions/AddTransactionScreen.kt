package com.angel.mony.presentation.transactions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceDraftField
import com.angel.mony.domain.model.VoiceTransactionCommand
import com.angel.mony.domain.model.VoiceTransactionDraft
import com.angel.mony.core.showToast
import com.angel.mony.presentation.components.AmountVisualTransformation
import com.angel.mony.presentation.components.FinanceTextField
import com.angel.mony.presentation.components.GlobalSaveButton
import com.angel.mony.presentation.components.GlobalSettingsButton
import com.angel.mony.presentation.components.FinanceModuleTopAppBar
import com.angel.mony.presentation.components.ExpenseFundingDialog
import com.angel.mony.presentation.components.PrimaryButton
import com.angel.mony.presentation.components.SecondaryButton
import com.angel.mony.presentation.components.sanitizeAmountInput
import com.angel.mony.core.MoneyFormatter
import com.angel.mony.ui.iconography.MonyIcon
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onManageModels: () -> Unit = onSettings,
    voiceEntry: Boolean = false,
    voiceOpeningId: String? = null,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val fieldErrors by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val editing by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val suggestedCategoryId by viewModel.suggestedCategoryId.collectAsStateWithLifecycle()
    val suggestedDate by viewModel.suggestedDate.collectAsStateWithLifecycle()
    val activePeriod by viewModel.activePeriod.collectAsStateWithLifecycle()
    val fundingRequest by viewModel.showFundingDialog.collectAsStateWithLifecycle()
    val possibleDuplicate by viewModel.possibleDuplicate.collectAsStateWithLifecycle()
    val skipConventionalNotice by viewModel.skipConventionalNotice.collectAsStateWithLifecycle()
    val widgetVoiceExample by viewModel.widgetVoiceExample.collectAsStateWithLifecycle()
    val localAiReadiness by viewModel.localAiReadiness.collectAsStateWithLifecycle()
    val periodDateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-DO"))
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.refreshVoiceModelState()
                Lifecycle.Event.ON_PAUSE -> viewModel.releaseVoiceModel()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshVoiceModelState()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var dateSuggestionApplied by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transactionTypeName by rememberSaveable {
        mutableStateOf(if (voiceEntry) null else viewModel.type.name)
    }
    var typeNeedsConfirmation by rememberSaveable { mutableStateOf(false) }
    val transactionType = transactionTypeName?.let(TransactionType::valueOf)
    val categories = allCategories.filter { it.type == transactionType }
    var voiceStatus by rememberSaveable { mutableStateOf(if (voiceEntry) "Pulsa el micrófono y di el movimiento." else "") }
    val voicePanelVisible = true
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    var interpretingText by remember { mutableStateOf(false) }
    var voiceTextInput by rememberSaveable { mutableStateOf("") }
    var saveRequested by rememberSaveable { mutableStateOf(false) }
    var saveDispatched by rememberSaveable { mutableStateOf(false) }
    var fundingSource by rememberSaveable { mutableStateOf("") }
    var categoryOptionIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var showRemoteConsent by remember { mutableStateOf(false) }
    var skipConventionalNoticeChoice by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var pendingTypeChange by remember { mutableStateOf<TransactionType?>(null) }
    var pendingTypeCommand by remember { mutableStateOf<VoiceTransactionCommand.Apply?>(null) }
    var handledVoiceOpeningId by rememberSaveable { mutableStateOf<String?>(null) }
    var autoStartedVoiceOpeningId by rememberSaveable { mutableStateOf<String?>(null) }
    var startRecognitionToken by remember { mutableStateOf(0) }
    var startConventionalRecognitionToken by remember { mutableStateOf(0) }
    var undoAmount by rememberSaveable { mutableStateOf<String?>(null) }
    var undoNote by rememberSaveable { mutableStateOf<String?>(null) }
    var undoDate by rememberSaveable { mutableStateOf<String?>(null) }
    var undoCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var undoTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var undoTypeNeedsConfirmation by rememberSaveable { mutableStateOf(false) }
    var hasUndo by rememberSaveable { mutableStateOf(false) }

    val eventHandler by rememberUpdatedState<(VoiceRecognitionEvent) -> Unit> { event ->
        when (event) {
            VoiceRecognitionEvent.Listening -> voiceStatus = "Escuchando…"
            VoiceRecognitionEvent.Processing -> voiceStatus = "Procesando voz…"
            is VoiceRecognitionEvent.Partial -> voiceTextInput = event.transcript
            is VoiceRecognitionEvent.Result -> {
                voiceTextInput = event.transcript
                pendingTranscript = event.transcript
            }
            is VoiceRecognitionEvent.Error -> voiceStatus = event.message
            VoiceRecognitionEvent.ConventionalRecognitionRequired -> {
                voiceStatus = "El dictado local no está disponible."
                if (skipConventionalNotice) {
                    startConventionalRecognitionToken++
                } else {
                    skipConventionalNoticeChoice = false
                    showRemoteConsent = true
                }
            }
            VoiceRecognitionEvent.Stopped -> if (voiceStatus == "Escuchando…") voiceStatus = "Escucha detenida."
        }
    }
    val voiceController = remember(context) {
        VoiceRecognitionController(context.applicationContext) { eventHandler(it) }
    }
    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }
    LaunchedEffect(startConventionalRecognitionToken) {
        if (startConventionalRecognitionToken > 0 && voicePanelVisible) {
            voiceController.start(VoiceRecognitionMode.CONVENTIONAL)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && voicePanelVisible) {
            startRecognitionToken++
        } else if (!granted) {
            voiceStatus = "Sin permiso de micrófono. Puedes escribir la descripción."
        }
    }

    fun requestVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        when (voiceController.availableMode()) {
            VoiceRecognitionMode.ON_DEVICE -> voiceController.start(VoiceRecognitionMode.ON_DEVICE)
            VoiceRecognitionMode.CONVENTIONAL -> if (skipConventionalNotice) {
                voiceController.start(VoiceRecognitionMode.CONVENTIONAL)
            } else {
                skipConventionalNoticeChoice = false
                showRemoteConsent = true
            }
            VoiceRecognitionMode.UNAVAILABLE -> voiceStatus = "El dictado no está disponible. Puedes escribir la descripción."
        }
    }

    fun currentDraft() = VoiceTransactionDraft(
        type = transactionType,
        amountInCents = MoneyFormatter.parseToCents(amount),
        categoryId = categoryId,
        date = runCatching { LocalDate.parse(date) }.getOrNull(),
        note = note.takeIf(String::isNotBlank),
        fundingSource = fundingSource.takeIf(String::isNotBlank),
        saveRequested = saveRequested,
    )

    fun missingFields(): String {
        val missing = buildList {
            if (transactionType == null || typeNeedsConfirmation) add("tipo")
            if (MoneyFormatter.parseToCents(amount)?.let { it > 0 } != true) add("monto")
            if (categoryId == null) add("categoría")
            if (runCatching { LocalDate.parse(date) }.getOrNull() == null) add("fecha")
        }
        return if (missing.isEmpty()) "El movimiento está listo para guardar." else "Falta: ${missing.joinToString(", ")}."
    }

    fun rememberUndo() {
        undoAmount = amount
        undoNote = note
        undoDate = date
        undoCategoryId = categoryId
        undoTypeName = transactionTypeName
        undoTypeNeedsConfirmation = typeNeedsConfirmation
        hasUndo = true
    }

    fun applyVoiceDraft(command: VoiceTransactionCommand.Apply, allowTypeChange: Boolean = false) {
        val incomingType = command.draft.type
        if (!voiceEntry && !allowTypeChange && incomingType != null && incomingType != transactionType) {
            pendingTypeChange = incomingType
            pendingTypeCommand = command
            voiceStatus = "La frase indica ${if (incomingType == TransactionType.EXPENSE) "gasto" else "ingreso"}. Confirma el cambio de tipo."
            return
        }
        rememberUndo()
        if (VoiceDraftField.TYPE in command.mentioned) {
            transactionTypeName = incomingType?.name
            typeNeedsConfirmation = false
            if (VoiceDraftField.CATEGORY !in command.mentioned) {
                categoryId = validCategoryIdForType(categoryId, incomingType, allCategories)
            }
        }
        if (VoiceDraftField.AMOUNT in command.mentioned) amount = command.draft.amountInCents?.let(MoneyFormatter::formatToInput).orEmpty()
        if (VoiceDraftField.CATEGORY in command.mentioned) {
            categoryId = validCategoryIdForType(
                command.draft.categoryId,
                incomingType ?: transactionType,
                allCategories,
            )
        }
        if (VoiceDraftField.DATE in command.mentioned) {
            date = command.draft.date?.toString().orEmpty()
            dateSuggestionApplied = true
        }
        if (VoiceDraftField.NOTE in command.mentioned) note = command.draft.note.orEmpty()
        if (VoiceDraftField.FUNDING_SOURCE in command.mentioned) fundingSource = command.draft.fundingSource.orEmpty()
        saveRequested = command.draft.saveRequested
        saveDispatched = false
        categoryOptionIds = command.categoryOptions.map(Category::id)
        voiceStatus = command.messages.firstOrNull() ?: if (command.draft.type == null) {
            "¿Gasto o ingreso?"
        } else voiceDraftSummary(
            command.draft.type,
            command.draft.amountInCents?.let(MoneyFormatter::formatToInput).orEmpty(),
            command.draft.categoryId,
            command.draft.date?.toString().orEmpty(),
            command.draft.note.orEmpty(),
            allCategories,
        )
    }

    val amountFocusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(editing?.id) {
        editing?.let {
            amount = java.math.BigDecimal.valueOf(it.amountInCents, 2).stripTrailingZeros().toPlainString()
            note = it.description.orEmpty()
            date = it.date.toString()
            categoryId = it.categoryId
        }
    }
    LaunchedEffect(suggestedCategoryId, categories, voiceEntry) {
        if (!voiceEntry && !viewModel.isEditing && categoryId == null) {
            categoryId = suggestedCategoryId?.takeIf { suggested ->
                categories.any { it.id == suggested }
            }
        }
    }
    LaunchedEffect(suggestedDate, voiceEntry) {
        if (!voiceEntry && !viewModel.isEditing && !dateSuggestionApplied && suggestedDate != null) {
            date = suggestedDate.toString()
            dateSuggestionApplied = true
        }
    }
    LaunchedEffect(error) {
        error?.let { message ->
            context.showToast(message)
            viewModel.consumeError()
            saveDispatched = false
        }
    }
    LaunchedEffect(startRecognitionToken) {
        if (startRecognitionToken > 0 && voicePanelVisible) requestVoiceRecognition()
    }
    LaunchedEffect(voiceEntry, voiceOpeningId) {
        if (!voiceEntry) return@LaunchedEffect
        val currentOpeningId = voiceOpeningId ?: "voice-entry"
        if (handledVoiceOpeningId != null && handledVoiceOpeningId != currentOpeningId) {
            amount = ""
            note = ""
            date = LocalDate.now().toString()
            dateSuggestionApplied = false
            categoryId = null
            transactionTypeName = null
            typeNeedsConfirmation = false
            saveRequested = false
            saveDispatched = false
            fundingSource = ""
            categoryOptionIds = emptyList()
            hasUndo = false
            pendingTranscript = null
            voiceTextInput = ""
            voiceStatus = ""
        }
        handledVoiceOpeningId = currentOpeningId
        if (autoStartedVoiceOpeningId != currentOpeningId) {
            autoStartedVoiceOpeningId = currentOpeningId
            requestVoiceRecognition()
        }
    }
    LaunchedEffect(voiceEntry, voiceOpeningId, allCategories) {
        if (voiceEntry && voiceOpeningId != null) {
            viewModel.updateWidgetVoiceExample(voiceOpeningId, allCategories)
        }
    }
    LaunchedEffect(pendingTranscript, localAiReadiness) {
        val transcript = pendingTranscript ?: return@LaunchedEffect
        if (localAiReadiness.phase == LocalAiPhase.PREPARING) return@LaunchedEffect
        pendingTranscript = null
        voiceTextInput = transcript
        if (localAiReadiness.phase != LocalAiPhase.READY) {
            voiceStatus = when (localAiReadiness.phase) {
                LocalAiPhase.NOT_INSTALLED -> "Descarga un modelo para interpretar la descripción."
                LocalAiPhase.ERROR -> "No se pudo preparar la IA. Revisa la configuración del modelo."
                LocalAiPhase.PREPARING -> "Preparando IA…"
                LocalAiPhase.READY -> ""
            }
            return@LaunchedEffect
        }
        val draftAtStart = currentDraft()
        val isTextInput = transcript == voiceTextInput.trim()
        interpretingText = isTextInput
        voiceStatus = "Comprendiendo el movimiento…"
        val interpretation = try {
            viewModel.interpretVoice(transcript, allCategories, draftAtStart)
        } catch (_: Exception) {
            voiceStatus = "No se pudo interpretar. Revisa el texto o completa los campos manualmente."
            interpretingText = false
            return@LaunchedEffect
        } finally {
            interpretingText = false
        }
        if (currentDraft() != draftAtStart || (isTextInput && voiceTextInput.trim() != transcript)) {
            voiceStatus = "El formulario cambió. Vuelve a interpretar la descripción."
            return@LaunchedEffect
        }
        if (interpretation.modelFailure != null) {
            voiceStatus = when (interpretation.modelFailure) {
                com.angel.mony.domain.model.LocalModelFailure.INSUFFICIENT_MEMORY -> "El teléfono no tiene memoria suficiente para usar este modelo."
                com.angel.mony.domain.model.LocalModelFailure.INCOMPATIBLE -> "No se pudo cargar este modelo en el teléfono."
                else -> "La IA no pudo procesar la descripción. Revisa el formulario manualmente."
            }
            return@LaunchedEffect
        }
        when (val command = interpretation.command) {
            is VoiceTransactionCommand.Apply -> {
                applyVoiceDraft(command)
                voiceStatus = command.messages.firstOrNull() ?: "Movimiento interpretado. Revisa los campos."
            }
            VoiceTransactionCommand.Save -> {
                if (typeNeedsConfirmation) {
                    saveRequested = false
                    voiceStatus = "¿Gasto o ingreso? Confirma el tipo antes de guardar."
                } else {
                    saveRequested = true
                    saveDispatched = false
                    voiceStatus = missingFields()
                }
            }
            VoiceTransactionCommand.Review -> voiceStatus = voiceDraftSummary(transactionType, amount, categoryId, date, note, allCategories)
            VoiceTransactionCommand.MissingFields -> voiceStatus = missingFields()
            VoiceTransactionCommand.ShowCategories -> voiceStatus = categories.joinToString(prefix = "Categorías: ") { it.name }
            VoiceTransactionCommand.Undo -> if (hasUndo) {
                amount = undoAmount.orEmpty(); note = undoNote.orEmpty(); date = undoDate.orEmpty()
                categoryId = undoCategoryId; transactionTypeName = undoTypeName
                typeNeedsConfirmation = undoTypeNeedsConfirmation; hasUndo = false
                voiceStatus = "Se deshizo el último cambio del borrador."
            } else voiceStatus = "No hay cambios del dictado para deshacer."
            VoiceTransactionCommand.Cancel -> { voiceController.cancel(); saveRequested = false; saveDispatched = false; voiceStatus = "Dictado cancelado." }
            VoiceTransactionCommand.Clear -> showClearConfirmation = true
            VoiceTransactionCommand.Exit -> if (hasTransactionData(amount, categoryId, note)) showExitConfirmation = true else onBack()
            VoiceTransactionCommand.ConfirmFunding -> if (fundingRequest != null && fundingSource.isNotBlank()) viewModel.confirmFundingSource(fundingSource)
                else voiceStatus = "Indica primero la fuente del dinero."
            VoiceTransactionCommand.AmbiguousType -> {
                typeNeedsConfirmation = true
                saveRequested = false
                saveDispatched = false
                voiceStatus = "La frase menciona gasto e ingreso. ¿Cuál deseas registrar?"
            }
            is VoiceTransactionCommand.SelectCategoryOption -> {
                categoryOptionIds.getOrNull(command.index)?.let { categoryId = it; categoryOptionIds = emptyList(); voiceStatus = missingFields() }
                    ?: run { voiceStatus = "Esa opción no está disponible." }
            }
            is VoiceTransactionCommand.Invalid -> voiceStatus =
                if (voiceEntry && transactionType == null) "¿Gasto o ingreso?" else command.message
        }
    }
    LaunchedEffect(saveRequested, typeNeedsConfirmation, transactionType, amount, categoryId, date, fundingRequest, fundingSource, saving) {
        if (!saveRequested || typeNeedsConfirmation || saveDispatched || saving) return@LaunchedEffect
        if (fundingRequest != null) {
            if (fundingSource.isNotBlank()) {
                saveDispatched = true
                viewModel.confirmFundingSource(fundingSource)
            }
            return@LaunchedEffect
        }
        val typeToSave = transactionType ?: return@LaunchedEffect
        if (MoneyFormatter.parseToCents(amount)?.let { it > 0 } == true && categoryId != null && runCatching { LocalDate.parse(date) }.isSuccess) {
            saveDispatched = true
            viewModel.save(amount, categoryId, note, date, onBack, typeToSave)
        } else {
            voiceStatus = missingFields()
        }
    }

    fun requestExit() {
        if (hasTransactionData(amount, categoryId, note)) showExitConfirmation = true else onBack()
    }
    BackHandler(onBack = ::requestExit)
    LaunchedEffect(fieldErrors) {
        if (fieldErrors.isNotEmpty()) {
            val firstErrorField = when {
                TransactionField.AMOUNT in fieldErrors -> 0
                TransactionField.CATEGORY in fieldErrors -> 1
                TransactionField.DATE in fieldErrors -> 3
                else -> 0
            }
            lazyListState.animateScrollToItem(firstErrorField)
            if (TransactionField.AMOUNT in fieldErrors) {
                amountFocusRequester.requestFocus()
            }
        }
    }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
        FinanceModuleTopAppBar(
            title =
                    if (editing == null) {
                        when (transactionType) {
                            TransactionType.EXPENSE -> "Registrar gasto"
                            TransactionType.INCOME -> "Registrar ingreso"
                            null -> "Registrar por voz"
                        }
                    } else "Editar movimiento",
            onBack = ::requestExit,
            actions = {
                GlobalSaveButton(
                    onClick = {
                        val typeToSave = transactionType
                        if (typeToSave == null || typeNeedsConfirmation) {
                            voiceStatus = "¿Gasto o ingreso? Confirma el tipo antes de guardar."
                        } else {
                            saveRequested = false
                            saveDispatched = true
                            viewModel.save(amount, categoryId, note, date, onBack, typeToSave)
                        }
                    },
                    enabled = !saving,
                    size = 48.dp,
                )
                Spacer(Modifier.width(8.dp))
                GlobalSettingsButton(onClick = onSettings, size = 48.dp)
            },
        )
        },
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (voiceEntry) {
                item {
                    VoiceTypeBadge(
                        state = voiceTypeBadgeState(transactionType, typeNeedsConfirmation),
                    )
                }
            }
            if (voiceEntry) item {
                VoiceDraftPanel(
                    status = voiceStatus,
                    example = if (voiceEntry) {
                        widgetVoiceExample.takeIf(String::isNotBlank)
                            ?: "Di cuánto fue, en qué lo usaste y cuándo"
                    } else when (transactionType) {
                        TransactionType.INCOME -> "Recibí 15,000 pesos por un trabajo"
                        else -> "Gasté 850 pesos comprando comida para la casa"
                    },
                    listening = voiceStatus in setOf("Escuchando…", "Procesando voz…"),
                    saving = saving,
                    readiness = localAiReadiness,
                    interpreting = interpretingText,
                    onManageModels = onManageModels,
                    textInput = voiceTextInput,
                    onTextInputChange = { voiceTextInput = it; voiceStatus = "" },
                    onInterpretText = {
                        if (!interpretingText) voiceTextInput.trim().takeIf(String::isNotBlank)?.let {
                            when (localAiReadiness.phase) {
                                LocalAiPhase.READY -> pendingTranscript = it
                                LocalAiPhase.NOT_INSTALLED, LocalAiPhase.ERROR -> onManageModels()
                                LocalAiPhase.PREPARING -> Unit
                            }
                        }
                    },
                    onListen = ::requestVoiceRecognition,
                    onStop = voiceController::stop,
                )
            }
            item {
                val amountError = TransactionField.AMOUNT in fieldErrors
                FinanceTextField(
                    value = amount,
                    onValueChange = {
                        amount = sanitizeAmountInput(it)
                        if (amount.isNotBlank() && amountError) {
                            viewModel.clearFieldError(TransactionField.AMOUNT)
                        }
                    },
                    label = "Monto en RD$",
                    placeholder = "0.00",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = AmountVisualTransformation,
                    isError = amountError,
                    errorMessage = fieldErrors[TransactionField.AMOUNT],
                )
            }
            item {
                val categoryError = TransactionField.CATEGORY in fieldErrors
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Categoría",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    CategorySearchSelect(
                        categories = categories,
                        selectedCategoryId = categoryId,
                        onCategoryChange = {
                            categoryId = it
                            if (it != null && categoryError) {
                                viewModel.clearFieldError(TransactionField.CATEGORY)
                            }
                        },
                        isError = categoryError,
                        errorMessage = fieldErrors[TransactionField.CATEGORY],
                    )
                }
            }
            item { Text("Detalles", style = MaterialTheme.typography.titleMedium) }
            item {
                val dateError = TransactionField.DATE in fieldErrors
                Box {
                    FinanceTextField(
                        value = date,
                        onValueChange = {},
                        label = "Fecha",
                        placeholder = "AAAA-MM-DD",
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            com.angel.mony.ui.iconography.MonyIcon(
                                icon = com.angel.mony.ui.iconography.MonyIcon.Calendar,
                                contentDescription = null,
                                tint = if (dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
                            )
                        },
                        isError = dateError,
                        errorMessage = fieldErrors[TransactionField.DATE],
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Seleccionar fecha",
                            ) { showDatePicker = true },
                    )
                }
            }
            val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
            val outsidePeriod = !viewModel.isEditing && parsedDate != null &&
                (parsedDate.isBefore(activePeriod.start) || parsedDate.isAfter(activePeriod.endInclusive))
            if (outsidePeriod) {
                item {
                    Text(
                        "Esta fecha está fuera del periodo actual (${activePeriod.start.format(periodDateFormatter)} / ${activePeriod.endInclusive.format(periodDateFormatter)}). El movimiento se registrará en el periodo correspondiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                FinanceTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Nota",
                    placeholder = "Añade un detalle opcional",
                )
            }
        }
    }

    if (showDatePicker) {
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY).toString()
                        }
                        showDatePicker = false
                        if (TransactionField.DATE in fieldErrors) {
                            viewModel.clearFieldError(TransactionField.DATE)
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fundingRequest?.let { request ->
        ExpenseFundingDialog(
            overflowAmountInCents = request.overflowInCents,
            availableBeforeExpenseInCents = request.availableBeforeExpenseInCents,
            expenseAmountInCents = request.expenseAmountInCents,
            sourceDescription = fundingSource,
            onSourceDescriptionChange = { fundingSource = it; saveDispatched = false },
            onVoice = ::requestVoiceRecognition,
            onConfirm = {
                fundingSource = it
                saveDispatched = true
                viewModel.confirmFundingSource(it)
            },
            onDismiss = {
                saveRequested = false
                saveDispatched = false
                fundingSource = ""
                viewModel.cancelFundingDialog()
            },
        )
    }

    if (showRemoteConsent) {
        AlertDialog(
            onDismissRequest = { showRemoteConsent = false },
            title = { Text("Reconocimiento convencional") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("El dictado sin conexión no está disponible. Si aceptas, el audio se enviará al servicio convencional de voz.")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { skipConventionalNoticeChoice = !skipConventionalNoticeChoice },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = skipConventionalNoticeChoice,
                            onCheckedChange = { skipConventionalNoticeChoice = it },
                        )
                        Text("No volver a mostrar")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoteConsent = false
                    if (skipConventionalNoticeChoice) viewModel.setSkipConventionalNotice(true)
                    voiceController.start(VoiceRecognitionMode.CONVENTIONAL)
                }) { Text("Aceptar y escuchar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemoteConsent = false
                    voiceStatus = ""
                }) { Text("Escribir descripción") }
            },
        )
    }

    if (categoryOptionIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { categoryOptionIds = emptyList() },
            title = { Text("Elige una categoría") },
            text = {
                Column {
                    categoryOptionIds.mapNotNull { id -> allCategories.firstOrNull { it.id == id } }.forEach { category ->
                        TextButton(onClick = {
                            categoryId = category.id
                            categoryOptionIds = emptyList()
                            voiceStatus = missingFields()
                        }) { Text(category.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { categoryOptionIds = emptyList() }) { Text("Cancelar") } },
        )
    }

    possibleDuplicate?.let { duplicate ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPossibleDuplicate,
            title = { Text("Posible movimiento duplicado") },
            text = {
                Text(
                    "Ya existe un movimiento similar: ${MoneyFormatter.format(duplicate.amountInCents)} el ${duplicate.date}, en ${allCategories.firstOrNull { it.id == duplicate.categoryId }?.name ?: "la misma categoría"}. ¿Quieres registrar otro de todos modos?",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPossibleDuplicate) { Text("Registrar de todos modos") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPossibleDuplicate) { Text("Revisar") }
            },
        )
    }

    pendingTypeChange?.let { newType ->
        AlertDialog(
            onDismissRequest = { pendingTypeChange = null; pendingTypeCommand = null },
            title = { Text("Cambiar tipo de movimiento") },
            text = { Text("Estás en un formulario de ${if (viewModel.type == TransactionType.EXPENSE) "gasto" else "ingreso"}, pero la frase indica ${if (newType == TransactionType.EXPENSE) "gasto" else "ingreso"}. ¿Deseas cambiarlo?") },
            confirmButton = {
                TextButton(onClick = {
                    val command = pendingTypeCommand
                    pendingTypeChange = null
                    pendingTypeCommand = null
                    transactionTypeName = newType.name
                    categoryId = null
                    if (command != null) applyVoiceDraft(command, allowTypeChange = true)
                }) { Text("Cambiar tipo") }
            },
            dismissButton = { TextButton(onClick = { pendingTypeChange = null; pendingTypeCommand = null }) { Text("Mantener tipo") } },
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Limpiar formulario") },
            text = { Text("Se borrarán los datos del borrador actual.") },
            confirmButton = { TextButton(onClick = {
                amount = ""; note = ""; categoryId = null; date = LocalDate.now().toString()
                if (voiceEntry) transactionTypeName = null
                typeNeedsConfirmation = false
                saveRequested = false; saveDispatched = false; fundingSource = ""; hasUndo = false
                showClearConfirmation = false; voiceStatus = "Formulario limpio."
            }) { Text("Limpiar") } },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancelar") } },
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Salir sin guardar") },
            text = { Text("Se perderá el borrador actual.") },
            confirmButton = { TextButton(onClick = { voiceController.cancel(); saveRequested = false; onBack() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { showExitConfirmation = false }) { Text("Continuar editando") } },
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun hasTransactionData(amount: String, categoryId: Long?, note: String): Boolean =
    amount.isNotBlank() || categoryId != null || note.isNotBlank()

private fun voiceDraftSummary(
    type: TransactionType?,
    amount: String,
    categoryId: Long?,
    date: String,
    note: String,
    categories: List<Category>,
): String = buildString {
    append(if (type == TransactionType.EXPENSE) "Gasto" else if (type == TransactionType.INCOME) "Ingreso" else "Tipo pendiente")
    append(" · ")
    append(MoneyFormatter.parseToCents(amount)?.let(MoneyFormatter::format) ?: "monto pendiente")
    append(" · ")
    append(categories.firstOrNull { it.id == categoryId }?.name ?: "categoría pendiente")
    append(" · ")
    append(date)
    if (note.isNotBlank()) append(" · Nota: $note")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySearchSelect(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategoryChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    // Null while untouched so the field mirrors the current selection;
    // typing takes over the text until a new category is picked.
    var draftQuery by remember { mutableStateOf<String?>(null) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()
    val query = draftQuery ?: selectedName
    val matchingCategories = remember(categories, query) {
        searchCategories(categories, query)
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        FinanceTextField(
            value = query,
            onValueChange = { value ->
                draftQuery = value
                if (selectedCategoryId != null) onCategoryChange(null)
                expanded = true
            },
            label = "Buscar o seleccionar categoría",
            placeholder = "Nombre de la categoría",
            singleLine = true,
            leadingIcon = { com.angel.mony.ui.iconography.MonyIcon(com.angel.mony.ui.iconography.MonyIcon.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                draftQuery = ""
                                onCategoryChange(null)
                                expanded = true
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            com.angel.mony.ui.iconography.MonyIcon(
                                com.angel.mony.ui.iconography.MonyIcon.Close,
                                contentDescription = "Limpiar categoría",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
            isError = isError,
            errorMessage = errorMessage,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                draftQuery = null
            },
        ) {
            matchingCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    trailingIcon = if (category.id == selectedCategoryId) {
                        {
                            com.angel.mony.ui.iconography.MonyIcon(
                                com.angel.mony.ui.iconography.MonyIcon.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                role = com.angel.mony.ui.iconography.MonyIconRole.STATE,
                            )
                        }
                    } else null,
                    onClick = {
                        draftQuery = null
                        onCategoryChange(category.id)
                        expanded = false
                    },
                )
            }
            if (matchingCategories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No se encontraron categorías") },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}
