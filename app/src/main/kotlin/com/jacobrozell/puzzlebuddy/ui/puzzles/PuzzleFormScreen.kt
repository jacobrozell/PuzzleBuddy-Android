package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDateSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzlePieceCount
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.MAX_PHOTOS
import com.jacobrozell.puzzlebuddy.domain.surface.LegalCopy
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import com.jacobrozell.puzzlebuddy.ui.barcode.BarcodeScannerSheet
import com.jacobrozell.puzzlebuddy.ui.components.DateField
import com.jacobrozell.puzzlebuddy.ui.components.FormSection
import com.jacobrozell.puzzlebuddy.ui.components.HalfStarRatingRow
import com.jacobrozell.puzzlebuddy.ui.components.PickerField
import com.jacobrozell.puzzlebuddy.ui.components.PuzzlePhotoGalleryEditor
import com.jacobrozell.puzzlebuddy.ui.components.PuzzleTagsField
import com.jacobrozell.puzzlebuddy.ui.components.SwitchRow
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.Instant
import java.time.Year

/**
 * Add / edit form in the iOS 1.1.0 `PuzzleForm` section order. Close with unsaved changes asks
 * before discarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleFormScreen(
    puzzleId: String?,
    onFinished: () -> Unit,
    viewModel: PuzzleFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isNew = puzzleId == null
    var showScanner by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true
    }

    fun close() {
        if (state.isDirty) confirmDiscard = true else onFinished()
    }

    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text(if (isNew) "This puzzle has not been saved." else "Your edits to this puzzle will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onFinished()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }

    state.saveError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveError,
            title = { Text(if (isNew) "Couldn't add puzzle" else "Couldn't save changes") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissSaveError) { Text("OK") } },
        )
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerSheet(
                onDismiss = { showScanner = false },
                onBarcodeScanned = { raw ->
                    showScanner = false
                    viewModel.applyScannedBarcode(raw, context as? Activity)
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isQuickAdd) "Add from scan" else if (isNew) "Add Puzzle" else "Edit Puzzle") },
                navigationIcon = {
                    IconButton(onClick = ::close) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                },
            )
        },
        bottomBar = {
            if (!state.missingPuzzle) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.imePadding()) {
                    Button(
                        onClick = { viewModel.save(context as? Activity, onFinished) },
                        enabled = state.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = "Save puzzle"
                                if (!state.canSave && !state.isSaving) stateDescription = "Enter a puzzle name to enable saving"
                            },
                    ) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp)) else Text("Save")
                    }
                }
            }
        },
    ) { padding ->
        when {
            !state.isLoaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.missingPuzzle -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("That puzzle is no longer in your collection.", style = MaterialTheme.typography.bodyLarge)
            }
            else -> ReadableContentWidth(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = imeBottom),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    FormBody(
                        state = state,
                        viewModel = viewModel,
                        onScanBarcode = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormBody(state: PuzzleFormUiState, viewModel: PuzzleFormViewModel, onScanBarcode: () -> Unit) {
    val puzzle = state.puzzle
    val brand = BrandTokens.colors

    if (state.isQuickAdd) ScanSuggestions(state)

    FormSection(
        header = "Photos",
        footer = "Add up to $MAX_PHOTOS photos at once. Use the arrows to reorder — the first photo is the cover.",
    ) {
        PuzzlePhotoGalleryEditor(
            photos = puzzle.photos,
            loadBytes = viewModel::photoBytes,
            onAddPhotos = viewModel::addPhotos,
            onRemove = viewModel::removePhoto,
            onMove = viewModel::movePhoto,
            onSetCover = viewModel::setCoverPhoto,
        )
    }

    FormSection(header = "Puzzle Info") {
        OutlinedTextField(
            value = puzzle.name,
            onValueChange = viewModel::updateName,
            label = { Text("Name") },
            placeholder = { Text("Puzzle Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        PiecesField(puzzle.pieces, viewModel::updatePieces)
        PickerField("Status", PuzzleStatus.entries, puzzle.status, { it.raw }, viewModel::updateStatus)
    }

    FormSection(header = "Brand") {
        OutlinedTextField(
            value = puzzle.source.orEmpty(),
            onValueChange = viewModel::updateSource,
            label = { Text("Brand") },
            placeholder = { Text("Ravensburger, Galison, Buffalo Games…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
    }

    FormSection(
        header = "About this puzzle",
        footer = "All fields here are optional. Bought at is the store or seller, not the puzzle brand. " +
            "For finished size, use inches or centimeters — match what’s on the box.",
    ) {
        OutlinedTextField(
            value = puzzle.purchaseLocation.orEmpty(),
            onValueChange = viewModel::updatePurchaseLocation,
            label = { Text("Bought at") },
            placeholder = { Text("Amazon, Goodwill, puzzle swap") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            value = state.priceText,
            onValueChange = viewModel::updatePrice,
            label = { Text("Amount paid") },
            placeholder = { Text("19.99") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        )
        val thisYear = Year.now().value
        PickerField(
            "Release year",
            listOf<Int?>(null) + (0 until 40).map { thisYear - it },
            puzzle.releaseYear,
            { it?.toString() ?: "Unknown" },
            viewModel::updateReleaseYear,
        )
        PickerField("Puzzle type", PuzzleType.entries, puzzle.puzzleType, { it.displayLabel }, viewModel::updateType)
        PickerField("Material", PuzzleMaterial.entries, puzzle.material, { it.displayLabel }, viewModel::updateMaterial)
        PickerField("Shape", PuzzleShape.entries, puzzle.puzzleShape, { it.displayLabel }, viewModel::updateShape)
        PickerField("Cut type", PuzzleCutType.entries, puzzle.cutType, { it.displayLabel }, viewModel::updateCutType)
        OutlinedTextField(
            value = puzzle.dimensionsText.orEmpty(),
            onValueChange = viewModel::updateDimensions,
            label = { Text("Finished size") },
            placeholder = { Text("27 × 19 in or 68 × 49 cm") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }

    FormSection(
        header = "Tags",
        footer = "Organize your collection with custom labels. Type to search existing tags or add a new one.",
    ) {
        PuzzleTagsField(tags = puzzle.tags, catalog = state.tagCatalog, onTagsChange = viewModel::updateTags)
    }

    if (ProductSurface.isBarcodeScanEnabled) {
        FormSection(
            header = "Barcode",
            footer = "Used to check for duplicates when shopping. Enter 6 to 14 digits, or scan from the box.",
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = puzzle.barcode.orEmpty(),
                    onValueChange = viewModel::updateBarcode,
                    label = { Text("Barcode") },
                    placeholder = { Text("UPC / EAN from box") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                IconButton(onClick = onScanBarcode) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                }
            }
            state.barcodeDuplicate?.let { duplicate ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "This barcode matches ${duplicate.name} in your collection"
                    },
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = brand.accentWarmText)
                    Text("Matches ${duplicate.name} in your collection.", color = brand.accentWarmText)
                }
            }
        }
    }

    FormSection(header = "Progress", footer = "Slide to track how far along you are, like Goodreads for puzzles.") {
        ProgressSlider(puzzle.progressPercent, puzzle.status, viewModel::updateProgress)
    }

    FormSection(header = "How did you like it?") {
        Text("Rating", style = MaterialTheme.typography.labelLarge, color = brand.textSecondary)
        HalfStarRatingRow(rating = puzzle.rating, onRatingChange = viewModel::updateRating)
        PickerField(
            "Difficulty",
            PuzzleDifficulty.entries,
            puzzle.difficulty,
            { if (it == PuzzleDifficulty.NONE) "N/A" else it.raw },
            viewModel::updateDifficulty,
        )
    }

    FormSection(
        header = "How long did it take?",
        footer = "Optional. Enter hours and minutes separately — for example, 3 hr and 45 min. Minutes roll over at 60.",
    ) {
        TimeSection(puzzle, viewModel)
    }

    FormSection(header = PuzzleDateSemantics.formDateSectionTitle(puzzle.status, puzzle.name)) {
        DateField(PuzzleDateSemantics.detailDateLabel(puzzle.status), puzzle.completionDate, viewModel::updateCompletionDate)
    }

    if (PuzzleDateSemantics.showsStartDatePicker(puzzle.status)) {
        FormSection(header = null, footer = "Used for days puzzling on the detail screen.") {
            DateField("Started on", puzzle.startDate ?: puzzle.completionDate, viewModel::updateStartDate)
        }
    }

    if (puzzle.status == PuzzleStatus.COMPLETED) {
        FormSection(header = "After finishing") {
            PickerField("After finishing", PuzzleDisposition.entries, puzzle.disposition, { it.displayLabel }, viewModel::updateDisposition)
        }
    }

    FormSection(header = "Condition") {
        SwitchRow("Missing pieces", puzzle.hasMissingPieces, viewModel::updateHasMissingPieces)
        SwitchRow("On loan", puzzle.isOnLoan, viewModel::updateOnLoan)
        if (puzzle.isOnLoan) {
            OutlinedTextField(
                value = puzzle.loanedToDisplayName.orEmpty(),
                onValueChange = viewModel::updateLoanedTo,
                label = { Text("Loaned to (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            DateField("Due back", puzzle.dueBackDate ?: Instant.now(), viewModel::updateDueBack)
        }
        OutlinedTextField(
            value = puzzle.notes.orEmpty(),
            onValueChange = viewModel::updateNotes,
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

/** Quick-add sections from iOS `QuickAddPuzzleSheet`: what the scan suggested, and look-alikes. */
@Composable
private fun ScanSuggestions(state: PuzzleFormUiState) {
    val brand = BrandTokens.colors
    val metadata = state.scanMetadata
    if (metadata != null && (metadata.suggestedName != null || metadata.brand != null)) {
        FormSection(
            header = "Suggested details",
            footer = LegalCopy.BARCODE_SCAN_DISCLAIMER +
                if (metadata.brand != null) "\n\n" + LegalCopy.BRAND_TRADEMARK_DISCLAIMER else "",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = brand.textSecondary)
                Text(metadata.lookupSourceLabel, color = brand.textSecondary)
            }
            state.scanSourcePuzzle?.let { source ->
                Text(
                    source.name + (source.pieces?.let { " · $it pieces" } ?: ""),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            metadata.brand?.let { Text("Brand: $it") }
            metadata.suggestedName?.let { Text("Suggested name: $it") }
        }
    } else {
        FormSection(header = "New barcode", footer = LegalCopy.BARCODE_SCAN_DISCLAIMER) {
            Text("First time scanning this barcode. Enter the details below.")
            Text("Next time you scan it, Puzzle Buddy can suggest what you saved on this device.", color = brand.textSecondary)
        }
    }
    val similar = state.similarMatches
    if (similar.isNotEmpty()) {
        FormSection(
            header = "Looks similar",
            footer = "You may already own a puzzle with a matching name, brand, or piece count.",
        ) {
            similar.forEach { match ->
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = listOfNotNull(
                            "Looks similar",
                            match.name,
                            match.pieces?.let { "$it pieces" },
                            match.source?.takeIf { it.isNotEmpty() },
                        ).joinToString(", ")
                    },
                ) {
                    Text(match.name, fontWeight = FontWeight.SemiBold)
                    match.pieces?.let { Text("$it pieces", color = brand.textSecondary) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PiecesField(pieces: Int?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = pieces?.toString().orEmpty(),
            onValueChange = { raw -> onChange(raw.filter(Char::isDigit).take(5).toIntOrNull()) },
            label = { Text("Pieces") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuzzlePieceCount.commonValues.forEach { value ->
                FilterChip(
                    selected = pieces == value,
                    onClick = { onChange(value) },
                    label = { Text(PuzzlePieceCount.formatted(value)) },
                    modifier = Modifier.semantics { contentDescription = "$value pieces" },
                )
            }
        }
    }
}

@Composable
private fun ProgressSlider(progress: Int, status: PuzzleStatus, onChange: (Int) -> Unit) {
    val clamped = PuzzleProgressSemantics.clamped(progress)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$clamped%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(PuzzleDateSemantics.statusPillLabel(status), color = BrandTokens.colors.textSecondary)
        Slider(
            value = clamped.toFloat(),
            onValueChange = { onChange((it / 5).toInt() * 5) },
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Progress"
                    stateDescription = PuzzleProgressSemantics.displayLabel(clamped)
                },
        )
    }
}

@Composable
private fun TimeSection(puzzle: Puzzle, viewModel: PuzzleFormViewModel) {
    val brand = BrandTokens.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = puzzle.estimatedTimeSpent?.hours?.toString().orEmpty(),
            onValueChange = { raw -> viewModel.updateHours(raw.filter(Char::isDigit).take(4).toIntOrNull()) },
            label = { Text("Hours") },
            suffix = { Text("hr") },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Hours spent on puzzle" },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = puzzle.estimatedTimeSpent?.minutes?.toString().orEmpty(),
            onValueChange = { raw -> viewModel.updateMinutes(raw.filter(Char::isDigit).take(4).toIntOrNull()) },
            label = { Text("Minutes") },
            suffix = { Text("min") },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Minutes spent on puzzle" },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
    }
    puzzle.estimatedTimeSpent?.displayLabel()?.let { preview ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Total time: $preview" },
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = brand.accentText)
            Text(preview, color = brand.accentText, fontWeight = FontWeight.Medium)
        }
    }
    val timerTime = PuzzleTimerSemantics.formSuggestion(
        PuzzleTimerSemantics.elapsedSeconds(puzzle, Instant.now()),
        puzzle.estimatedTimeSpent,
    )
    val timerLabel = timerTime?.displayLabel()
    if (timerTime != null && timerLabel != null) {
        Text("Timer recorded $timerLabel", color = brand.textSecondary)
        TextButton(
            onClick = { viewModel.useTimerTime(timerTime) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Use timer time", color = brand.accentText) }
    }
}
