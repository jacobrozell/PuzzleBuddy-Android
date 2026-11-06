package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.local.StoreNotice
import com.jacobrozell.puzzlebuddy.data.local.StoreNotices
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListEmptyState
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListFilters
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListQuery
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListSortOption
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListStatusFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagCount
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagIndex
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsSessionContext
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAnalyticsMetadata
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.support.share.PuzzleCollectionSharer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PuzzleListUiState(
    val allPuzzles: List<Puzzle> = emptyList(),
    val displayedPuzzles: List<Puzzle> = emptyList(),
    val filters: PuzzleListFilters = PuzzleListFilters(),
    val sortOption: PuzzleListSortOption = PuzzleListSortOption.COMPLETION_DATE,
    val availableTags: List<PuzzleTagCount> = emptyList(),
    val resultCountLabel: String = "",
    val emptyMessage: String = "",
    val showsAddPuzzleAction: Boolean = false,
    val pendingDelete: Puzzle? = null,
    val scanAlert: ScanAlert? = null,
    /** A list scan found a puzzle that already uses the barcode. */
    val scanMatch: Puzzle? = null,
)

data class ScanAlert(val title: String, val message: String)

@HiltViewModel
class PuzzleListViewModel @Inject constructor(
    val repository: PuzzleRepository,
    private val logger: AppLogger,
    private val barcodeMetadataCache: BarcodeMetadataCache,
    private val sessionContext: AnalyticsSessionContext,
    private val storeNotices: StoreNotices,
) : ViewModel() {
    val storeNotice: StateFlow<StoreNotice?> = storeNotices.notice

    fun dismissStoreNotice(notice: StoreNotice) = storeNotices.dismiss(notice)

    private var didLogListRefresh = false
    private val filters = MutableStateFlow(PuzzleListFilters())
    private val sortOption = MutableStateFlow(PuzzleListSortOption.COMPLETION_DATE)
    private val pendingDelete = MutableStateFlow<Puzzle?>(null)
    private val scanAlert = MutableStateFlow<ScanAlert?>(null)
    private val scanMatch = MutableStateFlow<Puzzle?>(null)

    val uiState: StateFlow<PuzzleListUiState> = combine(
        repository.observePuzzles(),
        filters,
        sortOption,
        pendingDelete,
        combine(scanAlert, scanMatch, ::Pair),
    ) { puzzles, filters, sort, deleting, (alert, match) ->
        val displayed = filters.apply(puzzles, sort)
        PuzzleListUiState(
            allPuzzles = puzzles,
            displayedPuzzles = displayed,
            filters = filters,
            sortOption = sort,
            availableTags = PuzzleTagIndex.counts(puzzles, limit = Int.MAX_VALUE),
            resultCountLabel = PuzzleListQuery.resultCountLabel(
                displayedCount = displayed.size,
                totalCount = puzzles.size,
                hasActiveFilters = filters.hasActiveFilters,
            ),
            emptyMessage = PuzzleListEmptyState.message(puzzles.size, filters),
            showsAddPuzzleAction = PuzzleListEmptyState.showsAddPuzzleAction(puzzles.size, filters),
            pendingDelete = deleting,
            scanAlert = alert,
            scanMatch = match,
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
                        metadata = PuzzleAnalyticsMetadata.collectionSnapshotMetadata(puzzles),
                    )
                    sessionContext.logSnapshotIfNeeded(puzzles)
                }
            }
        }
    }

    fun setStatusFilter(filter: PuzzleListStatusFilter) {
        filters.update { it.copy(status = filter) }
        sortOption.value = PuzzleListSortOption.defaultFor(filter)
    }

    fun updateFilters(transform: (PuzzleListFilters) -> PuzzleListFilters) {
        filters.update(transform)
    }

    fun clearSecondaryFilters() {
        filters.update { it.clearingSecondary() }
    }

    fun setSortOption(option: PuzzleListSortOption) {
        sortOption.value = option
    }

    fun requestDelete(puzzle: Puzzle) {
        pendingDelete.value = puzzle
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val puzzle = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            try {
                repository.delete(puzzle.id)
            } catch (error: Exception) {
                scanAlert.value = ScanAlert("Could not delete puzzle", error.message ?: "Try again.")
            }
        }
    }

    fun dismissScanAlert() {
        scanAlert.value = null
    }

    fun dismissScanMatch() {
        scanMatch.value = null
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
                    eventName = "barcode_scan_completed",
                    message = "Barcode scan completed.",
                    metadata = mapOf(
                        "scan_context" to "list_scan",
                        "scan_result" to "match",
                    ),
                )
                scanMatch.value = duplicate
                return@launch
            }
            val metadata = barcodeMetadataCache.metadataFor(normalized)
            logger.info(
                LogCategory.PUZZLES,
                eventName = "barcode_scan_completed",
                message = "Barcode scan completed.",
                metadata = mapOf(
                    "scan_context" to "list_scan",
                    "scan_result" to "no_match",
                ),
            )
            onQuickAdd(
                QuickAddRequest(
                    barcode = normalized,
                    name = metadata?.suggestedName,
                    pieces = metadata?.suggestedPieces,
                    source = metadata?.brand,
                ),
            )
        }
    }

    fun share(context: Context, puzzles: List<Puzzle>) {
        viewModelScope.launch {
            PuzzleCollectionSharer.share(context, repository, puzzles)
        }
    }

    fun logPickNextSelected(entryPoint: String, pieceCount: PuzzleListPieceCountFilter) {
        logger.info(
            LogCategory.UI,
            eventName = "pick_next_puzzle_selected",
            message = "Pick next puzzle selected.",
            metadata = mapOf(
                "entry_point" to entryPoint,
                "piece_count_bucket" to PuzzleAnalyticsMetadata.pieceCountBucket(pieceCount),
            ),
        )
    }
}
