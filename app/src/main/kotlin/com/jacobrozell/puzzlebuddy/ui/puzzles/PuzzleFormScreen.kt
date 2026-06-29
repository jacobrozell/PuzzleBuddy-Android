package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.QrCodeScanner
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.ui.barcode.BarcodeScannerSheet
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.ui.components.HalfStarRatingRow
import com.jacobrozell.puzzlebuddy.ui.components.PuzzlePhotoPicker
import com.jacobrozell.puzzlebuddy.ui.components.PuzzleTagsField
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveTwoPane
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleFormScreen(
    puzzleId: String?,
    onFinished: () -> Unit,
    viewModel: PuzzleFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val puzzle = state.puzzle
    val isNew = puzzleId == null
    var showScanner by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showScanner = true
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            BarcodeScannerSheet(
                onDismiss = { showScanner = false },
                onBarcodeScanned = { raw ->
                    showScanner = false
                    viewModel.updateBarcode(raw)
                },
            )
        }
    }
    state.lookupNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLookupNotice,
            title = { Text("Product lookup") },
            text = { Text(notice) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::dismissLookupNotice) {
                    Text("OK")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add puzzle" else "Edit puzzle") },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        ReadableContentWidth(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AdaptiveTwoPane(
                modifier = Modifier.fillMaxWidth(),
                firstPane = {
                    PuzzlePhotoPicker(
                        puzzleId = puzzle.id,
                        imageData = state.imageData,
                        repository = viewModel.repository,
                        onImageSelected = viewModel::updateImage,
                    )
                },
                secondPane = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = puzzle.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = puzzle.pieces?.toString().orEmpty(),
                onValueChange = { raw -> viewModel.updatePieces(raw.toIntOrNull()) },
                label = { Text("Pieces") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = puzzle.source.orEmpty(),
                onValueChange = viewModel::updateSource,
                label = { Text("Brand / source") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (ProductSurface.isBarcodeScanEnabled) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = puzzle.barcode.orEmpty(),
                        onValueChange = viewModel::updateBarcode,
                        label = { Text("Barcode") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    IconButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                }
            }
            EnumPicker(
                label = "Status",
                options = PuzzleStatus.entries,
                selected = puzzle.status,
                labelFor = { it.raw },
                onSelected = viewModel::updateStatus,
            )
            if (puzzle.status == PuzzleStatus.IN_PROGRESS) {
                Text(PuzzleProgressSemantics.displayLabel(puzzle.progressPercent))
                Slider(
                    value = puzzle.progressPercent.toFloat(),
                    onValueChange = { viewModel.updateProgress(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Rating", style = MaterialTheme.typography.labelMedium)
            HalfStarRatingRow(rating = puzzle.rating, onRatingChange = viewModel::updateRating)
            EnumPicker(
                label = "Difficulty",
                options = PuzzleDifficulty.entries,
                selected = puzzle.difficulty,
                labelFor = { if (it == PuzzleDifficulty.NONE) "N/A" else it.raw },
                onSelected = viewModel::updateDifficulty,
            )
            OutlinedTextField(
                value = puzzle.estimatedTimeSpent?.hours?.toString().orEmpty(),
                onValueChange = { viewModel.updateHours(it.toIntOrNull()) },
                label = { Text("Hours spent") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = puzzle.estimatedTimeSpent?.minutes?.toString().orEmpty(),
                onValueChange = { viewModel.updateMinutes(it.toIntOrNull()) },
                label = { Text("Minutes spent") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            PuzzleTagsField(
                tags = puzzle.tags,
                catalog = state.tagCatalog,
                onTagsChange = viewModel::updateTags,
            )
            OutlinedTextField(
                value = puzzle.notes.orEmpty(),
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            RowWithSwitch(
                label = "Missing pieces",
                checked = puzzle.hasMissingPieces,
                onCheckedChange = viewModel::updateHasMissingPieces,
            )
            Button(
                onClick = { viewModel.save(onFinished) },
                enabled = state.canSave && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
                    }
                },
            )
        }
    }
}

@Composable
private fun RowWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> EnumPicker(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Text(labelFor(selected))
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
