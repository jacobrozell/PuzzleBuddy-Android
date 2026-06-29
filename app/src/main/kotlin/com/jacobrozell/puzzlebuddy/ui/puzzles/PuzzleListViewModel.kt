package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.barcode.BarcodeLookupService
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListQuery
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListSortOption
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListStatusFilter
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PuzzleListUiState(
    val allPuzzles: List<Puzzle> = emptyList(),
    val displayedPuzzles: List<Puzzle> = emptyList(),
    val statusFilter: PuzzleListStatusFilter = PuzzleListStatusFilter.ALL,
    val searchText: String = "",
    val sortOption: PuzzleListSortOption = PuzzleListSortOption.COMPLETION_DATE,
    val missingPiecesOnly: Boolean = false,
    val needsPhotoOnly: Boolean = false,
    val pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
    val resultCountLabel: String = "",
    val emptyMessage: String = "",
    val scanAlert: ScanAlert? = null,
    val isLookingUpBarcode: Boolean = false,
)

data class ScanAlert(val title: String, val message: String)

@HiltViewModel
class PuzzleListViewModel @Inject constructor(
    val repository: PuzzleRepository,
    private val logger: AppLogger,
    private val barcodeLookupService: BarcodeLookupService,
) : ViewModel() {
    private var didLogListRefresh = false
    private val statusFilter = MutableStateFlow(PuzzleListStatusFilter.ALL)
    private val searchText = MutableStateFlow("")
    private val sortOption = MutableStateFlow(PuzzleListSortOption.COMPLETION_DATE)
    private val missingPiecesOnly = MutableStateFlow(false)
    private val needsPhotoOnly = MutableStateFlow(false)
    private val pieceCountFilter = MutableStateFlow(PuzzleListPieceCountFilter.ANY)
    private val scanAlert = MutableStateFlow<ScanAlert?>(null)
    private val isLookingUpBarcode = MutableStateFlow(false)

    val uiState: StateFlow<PuzzleListUiState> = combine(
        repository.observePuzzles(),
        statusFilter,
        searchText,
        sortOption,
        missingPiecesOnly,
        needsPhotoOnly,
        pieceCountFilter,
        scanAlert,
        isLookingUpBarcode,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val puzzles = values[0] as List<Puzzle>
        val status = values[1] as PuzzleListStatusFilter
        val search = values[2] as String
        val sort = values[3] as PuzzleListSortOption
        val missingOnly = values[4] as Boolean
        val photoOnly = values[5] as Boolean
        val pieceFilter = values[6] as PuzzleListPieceCountFilter
        val alert = values[7] as ScanAlert?
        val lookingUp = values[8] as Boolean
        val displayed = PuzzleListQuery.apply(
            puzzles = puzzles,
            statusFilter = status,
            searchText = search,
            sortOption = sort,
            missingPiecesOnly = missingOnly,
            needsPhotoOnly = photoOnly,
            pieceCountFilter = pieceFilter,
        )
        PuzzleListUiState(
            allPuzzles = puzzles,
            displayedPuzzles = displayed,
            statusFilter = status,
            searchText = search,
            sortOption = sort,
            missingPiecesOnly = missingOnly,
            needsPhotoOnly = photoOnly,
            pieceCountFilter = pieceFilter,
            resultCountLabel = PuzzleListQuery.resultCountLabel(
                displayedCount = displayed.size,
                totalCount = puzzles.size,
                hasActiveFilters = PuzzleListQuery.hasActiveFilters(
                    statusFilter = status,
                    searchText = search,
                    missingPiecesOnly = missingOnly,
                    needsPhotoOnly = photoOnly,
                    pieceCountFilter = pieceFilter,
                ),
            ),
            emptyMessage = status.emptyStateMessage(PuzzleListQuery.hasActiveSearch(search)),
            scanAlert = alert,
            isLookingUpBarcode = lookingUp,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PuzzleListUiState())

    init {
        viewModelScope.launch {
            repository.observePuzzles().collect { puzzles ->
                if (!didLogListRefresh) {
                    didLogListRefresh = true
                    logger.info(
                        LogCategory.PUZZLES,
                        eventName = "puzzle_list_refreshed",
                        message = "Puzzle list loaded.",
                        metadata = mapOf("puzzle_count" to puzzles.size.toString()),
                    )
                }
            }
        }
    }

    fun setStatusFilter(filter: PuzzleListStatusFilter) {
        statusFilter.value = filter
        sortOption.value = PuzzleListSortOption.defaultFor(filter)
    }

    fun setSearchText(text: String) {
        searchText.value = text
    }

    fun setSortOption(option: PuzzleListSortOption) {
        sortOption.value = option
    }

    fun toggleMissingPiecesOnly() {
        missingPiecesOnly.value = !missingPiecesOnly.value
    }

    fun toggleNeedsPhotoOnly() {
        needsPhotoOnly.value = !needsPhotoOnly.value
    }

    fun setPieceCountFilter(filter: PuzzleListPieceCountFilter) {
        pieceCountFilter.value = filter
    }

    fun deletePuzzle(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun dismissScanAlert() {
        scanAlert.value = null
    }

    fun handleScannedBarcode(raw: String, onQuickAdd: (QuickAddRequest) -> Unit) {
        viewModelScope.launch {
            val normalized = BarcodeValidator.normalizeOrNull(raw)
            if (normalized == null) {
                scanAlert.value = ScanAlert(
                    title = "Invalid barcode",
                    message = "Enter a barcode with 6 to 14 digits, or try scanning again.",
                )
                return@launch
            }
            val duplicate = repository.isDuplicateBarcode(normalized)
            if (duplicate != null) {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "barcode_scan_duplicate",
                    message = "Duplicate barcode on quick scan.",
                    metadata = mapOf("source" to "list_scan"),
                )
                scanAlert.value = ScanAlert(
                    title = "Already in your collection",
                    message = "${duplicate.name} already uses this barcode.",
                )
                return@launch
            }
            isLookingUpBarcode.value = true
            val lookup = barcodeLookupService.lookup(normalized)
            isLookingUpBarcode.value = false
            val metadata = lookup.metadata
            logger.info(
                LogCategory.PUZZLES,
                eventName = "barcode_scan_quick_add",
                message = "Quick add from barcode scan.",
                metadata = mapOf("source" to "list_scan"),
            )
            onQuickAdd(
                QuickAddRequest(
                    barcode = normalized,
                    name = metadata?.suggestedName,
                    pieces = metadata?.suggestedPieces,
                    source = metadata?.brand,
                    lookupNotice = lookup.notice?.message,
                ),
            )
        }
    }
}
