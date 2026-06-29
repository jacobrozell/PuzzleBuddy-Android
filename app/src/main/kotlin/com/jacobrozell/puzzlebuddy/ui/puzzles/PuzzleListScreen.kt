package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.ui.barcode.BarcodeScannerSheet
import com.jacobrozell.puzzlebuddy.ui.barcode.ShoppingModeScreen
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveLayout
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListSortOption
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListStatusFilter
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.ui.components.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.ui.components.StarRatingSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleListScreen(
    onOpenPuzzle: (String) -> Unit,
    onAddPuzzle: (String?) -> Unit,
    onQuickAdd: (QuickAddRequest) -> Unit,
    viewModel: PuzzleListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showShopping by remember { mutableStateOf(false) }
    var pendingScanAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingScanAction?.invoke()
        pendingScanAction = null
    }

    fun requestCameraAndRun(action: () -> Unit) {
        pendingScanAction = action
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    state.scanAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = viewModel::dismissScanAlert,
            title = { Text(alert.title) },
            text = { Text(alert.message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissScanAlert) { Text("OK") }
            },
        )
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
                    viewModel.handleScannedBarcode(raw, onQuickAdd = onQuickAdd)
                },
            )
        }
    }
    if (showShopping) {
        Dialog(
            onDismissRequest = { showShopping = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ShoppingModeScreen(
                onDismiss = { showShopping = false },
                onOpenPuzzle = { id ->
                    showShopping = false
                    onOpenPuzzle(id)
                },
                onAddPuzzle = { barcode ->
                    showShopping = false
                    onAddPuzzle(barcode)
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Buddy") },
                actions = {
                    if (ProductSurface.isBarcodeScanEnabled) {
                        IconButton(
                            onClick = { requestCameraAndRun { showScanner = true } },
                            modifier = Modifier.semantics { contentDescription = "Scan barcode" },
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        }
                    }
                    if (ProductSurface.isShoppingModeEnabled) {
                        IconButton(
                            onClick = { requestCameraAndRun { showShopping = true } },
                            modifier = Modifier.semantics { contentDescription = "Shopping mode" },
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddPuzzle(null) },
                modifier = Modifier.semantics { contentDescription = "Add puzzle" },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        ReadableContentWidth(modifier = Modifier.padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::setSearchText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search") },
                singleLine = true,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PuzzleListStatusFilter.entries.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = state.statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index, PuzzleListStatusFilter.entries.size),
                    ) {
                        Text(filter.title, maxLines = 1)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.resultCountLabel.isNotEmpty()) {
                    Text(state.resultCountLabel, style = MaterialTheme.typography.labelMedium)
                }
                Row {
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text("Sort: ${state.sortOption.title}")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        PuzzleListSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.title) },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                    TextButton(onClick = { filterMenuExpanded = true }) {
                        Text("Filters")
                    }
                    DropdownMenu(expanded = filterMenuExpanded, onDismissRequest = { filterMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (state.missingPiecesOnly) "✓ Missing pieces" else "Missing pieces") },
                            onClick = viewModel::toggleMissingPiecesOnly,
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.needsPhotoOnly) "✓ Needs photo" else "Needs photo") },
                            onClick = viewModel::toggleNeedsPhotoOnly,
                        )
                        PuzzleListPieceCountFilter.entries.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.title) },
                                onClick = { viewModel.setPieceCountFilter(filter) },
                            )
                        }
                    }
                }
            }

            if (state.displayedPuzzles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useGrid = maxWidth >= AdaptiveLayout.expandedWidthBreakpoint
                    if (useGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(AdaptiveLayout.listGridMinCellWidth),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.displayedPuzzles, key = { it.id }) { puzzle ->
                                PuzzleRowCard(
                                    puzzle = puzzle,
                                    repository = viewModel.repository,
                                    onClick = { onOpenPuzzle(puzzle.id) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.displayedPuzzles, key = { it.id }) { puzzle ->
                                SwipeablePuzzleRow(
                                    puzzle = puzzle,
                                    repository = viewModel.repository,
                                    onOpen = { onOpenPuzzle(puzzle.id) },
                                    onDelete = { viewModel.deletePuzzle(puzzle.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
        if (state.isLookingUpBarcode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePuzzleRow(
    puzzle: Puzzle,
    repository: PuzzleRepository,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        PuzzleRowCard(puzzle = puzzle, repository = repository, onClick = onOpen)
    }
}

@Composable
private fun PuzzleRowCard(
    puzzle: Puzzle,
    repository: PuzzleRepository,
    onClick: () -> Unit,
) {
    val dateLabel = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .format(puzzle.completionDate.atZone(ZoneId.systemDefault()))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(puzzle.name)
                    puzzle.pieces?.let { append(", $it pieces") }
                    append(", ${puzzle.status.raw}")
                    append(", $dateLabel")
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PuzzlePhoto(
                    puzzleId = if (puzzle.hasImage) puzzle.id else null,
                    imageData = null,
                    repository = repository,
                    modifier = Modifier.fillMaxSize(),
                    placeholderSize = 28.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(puzzle.name, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                puzzle.source?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    puzzle.pieces?.let { Text("$it pc", style = MaterialTheme.typography.labelMedium) }
                    Text(dateLabel, style = MaterialTheme.typography.labelMedium)
                }
                StarRatingSummary(rating = puzzle.rating)
            }
            FilterChip(
                selected = false,
                onClick = {},
                enabled = false,
                label = { Text(puzzle.status.raw) },
            )
        }
    }
}
