package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.data.local.StoreNotice
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListFilters
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListEmptyState
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListSortOption
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListStatusFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagCount
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.ui.barcode.BarcodeScanResultCard
import com.jacobrozell.puzzlebuddy.ui.barcode.BarcodeScannerSheet
import com.jacobrozell.puzzlebuddy.ui.barcode.ShoppingScanResult
import com.jacobrozell.puzzlebuddy.ui.barcode.ShoppingModeScreen
import com.jacobrozell.puzzlebuddy.ui.components.BrandFilterChip
import com.jacobrozell.puzzlebuddy.ui.components.ChipTone
import com.jacobrozell.puzzlebuddy.ui.components.PickerField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveLayout
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import com.jacobrozell.puzzlebuddy.ui.picknext.PickNextSheet
import com.jacobrozell.puzzlebuddy.ui.share.PuzzleShareMenu
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleListScreen(
    onOpenPuzzle: (String) -> Unit,
    onAddPuzzle: (String?) -> Unit,
    onQuickAdd: (QuickAddRequest) -> Unit,
    viewModel: PuzzleListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val storeNotice by viewModel.storeNotice.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }
    var showPickNext by remember { mutableStateOf(false) }
    var showShopping by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var addMenuExpanded by remember { mutableStateOf(false) }
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
            confirmButton = { TextButton(onClick = viewModel::dismissScanAlert) { Text("OK") } },
        )
    }

    state.scanMatch?.let { match ->
        Dialog(onDismissRequest = viewModel::dismissScanMatch) {
            BarcodeScanResultCard(
                result = ShoppingScanResult.Match(match),
                onOpenPuzzle = { puzzle ->
                    viewModel.dismissScanMatch()
                    onOpenPuzzle(puzzle.id)
                },
                onAddPuzzle = {},
                onScanAnother = {
                    viewModel.dismissScanMatch()
                    requestCameraAndRun { showScanner = true }
                },
            )
        }
    }

    state.pendingDelete?.let { puzzle ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete ${puzzle.name}?") },
            text = { Text("This puzzle will be removed from your collection. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } },
        )
    }

    if (ProductSurface.isPickNextEnabled && showPickNext) {
        PickNextSheet(
            puzzles = state.allPuzzles,
            tags = state.availableTags,
            onOpen = onOpenPuzzle,
            onPicked = { filter -> viewModel.logPickNextSelected("list", filter) },
            onDismiss = { showPickNext = false },
        )
    }

    if (showTagSheet) {
        TagFilterSheet(
            tags = state.availableTags,
            selection = state.filters.tag,
            onSelect = { tag ->
                viewModel.updateFilters { it.copy(tag = tag) }
                showTagSheet = false
            },
            onDismiss = { showTagSheet = false },
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
                    viewModel.handleScannedBarcode(raw, context as? Activity, onQuickAdd = onQuickAdd)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Buddy") },
                actions = {
                    if (ProductSurface.isPickNextEnabled) {
                        IconButton(onClick = { showPickNext = true }) {
                            Icon(Icons.Filled.Casino, contentDescription = "Pick my next puzzle")
                        }
                    }
                    if (ProductSurface.isShoppingModeEnabled) {
                        IconButton(onClick = { requestCameraAndRun { showShopping = true } }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Check duplicate")
                        }
                    }
                    SortMenu(selected = state.sortOption, onSelect = viewModel::setSortOption)
                    PuzzleShareMenu(
                        entireCollection = state.allPuzzles,
                        visibleList = state.displayedPuzzles,
                        imageVector = Icons.Filled.MoreVert,
                        onShare = { puzzles -> viewModel.share(context, puzzles) },
                    )
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { addMenuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "Add puzzle" },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
                DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add puzzle") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            onAddPuzzle(null)
                        },
                    )
                    if (ProductSurface.isBarcodeScanEnabled) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Scan barcode")
                                    Text(
                                        "Add to your collection",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandTokens.colors.textSecondary,
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                            onClick = {
                                addMenuExpanded = false
                                requestCameraAndRun { showScanner = true }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        ReadableContentWidth(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                storeNotice?.let { notice -> StoreNoticeBanner(notice) { viewModel.dismissStoreNotice(notice) } }
                FilterHeader(
                    state = state,
                    viewModel = viewModel,
                    onOpenTags = { showTagSheet = true },
                )
                PuzzleCollection(
                    state = state,
                    repository = viewModel.repository,
                    onOpenPuzzle = onOpenPuzzle,
                    onRequestDelete = viewModel::requestDelete,
                    onAddPuzzle = { onAddPuzzle(null) },
                )
            }
        }
    }
}

/** Storage warning from iOS `PuzzleView.noticeBanner`. */
@Composable
private fun StoreNoticeBanner(notice: StoreNotice, onDismiss: () -> Unit) {
    val brand = BrandTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandTokens.accentWarm.copy(alpha = 0.12f))
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = brand.accentWarmText)
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = notice.accessibilityLabel },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(notice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = brand.textPrimary)
            Text(notice.message, style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Clear, contentDescription = notice.dismissLabel, tint = brand.textSecondary)
        }
    }
}

@Composable
private fun SortMenu(selected: PuzzleListSortOption, onSelect: (PuzzleListSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.SwapVert, contentDescription = "Sort puzzles, ${selected.title}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PuzzleListSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    leadingIcon = {
                        if (option == selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = option.accessibilityLabel
                        if (option == selected) stateDescription = "Selected"
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterHeader(
    state: PuzzleListUiState,
    viewModel: PuzzleListViewModel,
    onOpenTags: () -> Unit,
) {
    val filters = state.filters
    val brand = BrandTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Six segments truncate at large font scale; use a menu there, like iOS splitStatusMenuPicker.
        if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
            PickerField(
                label = "Filter puzzles by status",
                options = PuzzleListStatusFilter.entries,
                selected = filters.status,
                labelFor = { it.title },
                onSelected = viewModel::setStatusFilter,
            )
        } else {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Filter puzzles by status" },
            ) {
                PuzzleListStatusFilter.entries.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = filters.status == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index, PuzzleListStatusFilter.entries.size),
                        icon = {},
                    ) {
                        Text(filter.title, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        OutlinedTextField(
            value = filters.searchText,
            onValueChange = { text -> viewModel.updateFilters { it.copy(searchText = text) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search name, brand, store, tag, or barcode",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filters.hasActiveSearch) {
                    IconButton(onClick = { viewModel.updateFilters { it.copy(searchText = "") } }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
        )

        if (state.allPuzzles.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    state.resultCountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textSecondary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (filters.hasSecondaryFilters) {
                    TextButton(onClick = viewModel::clearSecondaryFilters) {
                        Text("Clear filters", color = brand.accentText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            FilterChipRow(
                filters = filters,
                hasTags = state.availableTags.isNotEmpty(),
                onUpdate = viewModel::updateFilters,
                onOpenTags = onOpenTags,
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    filters: PuzzleListFilters,
    hasTags: Boolean,
    onUpdate: ((PuzzleListFilters) -> PuzzleListFilters) -> Unit,
    onOpenTags: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MenuChip(
            title = filters.pieceCount.title,
            icon = Icons.Filled.Numbers,
            isActive = filters.pieceCount != PuzzleListPieceCountFilter.ANY,
            contentDescription = "Filter by piece count, ${filters.pieceCount.title}",
            options = PuzzleListPieceCountFilter.entries.map { it.title to it },
            selected = filters.pieceCount,
            onSelect = { value -> onUpdate { it.copy(pieceCount = value ?: PuzzleListPieceCountFilter.ANY) } },
        )
        MenuChip(
            title = filters.type?.displayLabel ?: "Type",
            icon = Icons.Filled.Category,
            isActive = filters.type != null,
            contentDescription = "Filter by type, ${filters.type?.displayLabel ?: "any"}",
            options = listOf<Pair<String, PuzzleType?>>("Any type" to null) +
                PuzzleType.selectableCases.map { it.displayLabel to it },
            selected = filters.type,
            onSelect = { value -> onUpdate { it.copy(type = value) } },
        )
        MenuChip(
            title = filters.material?.displayLabel ?: "Material",
            icon = Icons.Filled.Layers,
            isActive = filters.material != null,
            contentDescription = "Filter by material, ${filters.material?.displayLabel ?: "any"}",
            options = listOf<Pair<String, PuzzleMaterial?>>("Any material" to null) +
                PuzzleMaterial.selectableCases.map { it.displayLabel to it },
            selected = filters.material,
            onSelect = { value -> onUpdate { it.copy(material = value) } },
        )
        MenuChip(
            title = filters.disposition?.displayLabel ?: "Fate",
            icon = Icons.Outlined.AccountTree,
            isActive = filters.disposition != null,
            contentDescription = "Filter by disposition, ${filters.disposition?.displayLabel ?: "any"}",
            options = listOf<Pair<String, PuzzleDisposition?>>("Any disposition" to null) +
                PuzzleDisposition.selectableCases.map { it.displayLabel to it },
            selected = filters.disposition,
            onSelect = { value -> onUpdate { it.copy(disposition = value) } },
        )
        BrandFilterChip(
            title = filters.tag ?: "Tags",
            icon = Icons.AutoMirrored.Filled.Label,
            isActive = filters.tag != null,
            enabled = hasTags,
            onClick = onOpenTags,
            contentDescription = "Filter by tag, ${filters.tag ?: "any"}",
        )
        ToggleChip("Needs photo", "Filter needs photo", Icons.Filled.ImageNotSupported, filters.needsPhotoOnly) {
            onUpdate { it.copy(needsPhotoOnly = !it.needsPhotoOnly) }
        }
        ToggleChip("Missing pieces", "Filter missing pieces", Icons.Filled.Warning, filters.missingPiecesOnly) {
            onUpdate { it.copy(missingPiecesOnly = !it.missingPiecesOnly) }
        }
        ToggleChip("On loan", "Filter on loan", Icons.Filled.PersonPin, filters.onLoanOnly) {
            onUpdate { it.copy(onLoanOnly = !it.onLoanOnly) }
        }
        ToggleChip("Overdue", "Filter overdue loans", Icons.Filled.MoreTime, filters.overdueOnly) {
            onUpdate { it.copy(overdueOnly = !it.overdueOnly) }
        }
    }
}

@Composable
private fun ToggleChip(title: String, description: String, icon: ImageVector, isOn: Boolean, onToggle: () -> Unit) {
    BrandFilterChip(
        title = title,
        icon = if (isOn) Icons.Filled.CheckCircle else icon,
        isActive = isOn,
        tone = ChipTone.WARM,
        onClick = onToggle,
        contentDescription = description,
        stateDescription = if (isOn) "On" else "Off",
    )
}

@Composable
private fun <T> MenuChip(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    contentDescription: String,
    options: List<Pair<String, T?>>,
    selected: T?,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        BrandFilterChip(
            title = title,
            icon = icon,
            isActive = isActive,
            onClick = { expanded = true },
            contentDescription = contentDescription,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { if (value == selected) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleCollection(
    state: PuzzleListUiState,
    repository: PuzzleRepository,
    onOpenPuzzle: (String) -> Unit,
    onRequestDelete: (Puzzle) -> Unit,
    onAddPuzzle: () -> Unit,
) {
    if (state.displayedPuzzles.isEmpty()) {
        EmptyState(state = state, onAddPuzzle = onAddPuzzle)
        return
    }
    val now = Instant.now()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= AdaptiveLayout.expandedWidthBreakpoint) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(AdaptiveLayout.listGridMinCellWidth),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.displayedPuzzles, key = { it.id }) { puzzle ->
                    PuzzleRow(
                        puzzle = puzzle,
                        repository = repository,
                        onOpen = { onOpenPuzzle(puzzle.id) },
                        onRequestDelete = { onRequestDelete(puzzle) },
                        now = now,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.displayedPuzzles, key = { it.id }) { puzzle ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) onRequestDelete(puzzle)
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            if (dismissState.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                    ) {
                        PuzzleRow(
                            puzzle = puzzle,
                            repository = repository,
                            onOpen = { onOpenPuzzle(puzzle.id) },
                            onRequestDelete = { onRequestDelete(puzzle) },
                            now = now,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(state: PuzzleListUiState, onAddPuzzle: () -> Unit) {
    val brand = BrandTokens.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .then(if (state.showsAddPuzzleAction) Modifier else Modifier.semantics(mergeDescendants = true) {}),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Filled.Extension, contentDescription = null, tint = BrandTokens.accent, modifier = Modifier.size(48.dp))
        if (state.showsAddPuzzleAction) {
            Text(
                PuzzleListEmptyState.NEW_COLLECTION_TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = brand.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            state.emptyMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = brand.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (state.showsAddPuzzleAction) {
            Button(onClick = onAddPuzzle, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add puzzle") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterSheet(
    tags: List<PuzzleTagCount>,
    selection: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val trimmed = search.trim()
    val filtered = if (trimmed.isEmpty()) tags else tags.filter { it.name.contains(trimmed, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tags") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    TagRow("Any tag", null, isSelected = selection == null) { onSelect(null) }
                    HorizontalDivider()
                    Text(
                        if (trimmed.isEmpty()) {
                            "Your tags"
                        } else {
                            "${filtered.size} tag${if (filtered.size == 1) "" else "s"} match “$trimmed”."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandTokens.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(filtered, key = { it.name }) { tag ->
                    TagRow(tag.name, tag.count, isSelected = selection.equals(tag.name, ignoreCase = true)) {
                        onSelect(tag.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(name: String, count: Int?, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = count?.let { { Text("$it puzzle${if (it == 1) "" else "s"}") } },
        trailingContent = { if (isSelected) Icon(Icons.Filled.Check, contentDescription = null) },
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (count == null) name else "Filter by tag $name, $count puzzles"
                if (isSelected) stateDescription = "Selected"
            },
    )
}
