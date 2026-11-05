package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import java.time.Instant
import java.time.ZoneId

/** Every list filter in one value, as iOS `PuzzleList` keeps them in separate `@State`s. */
data class PuzzleListFilters(
    val status: PuzzleListStatusFilter = PuzzleListStatusFilter.ALL,
    val searchText: String = "",
    val missingPiecesOnly: Boolean = false,
    val onLoanOnly: Boolean = false,
    val overdueOnly: Boolean = false,
    val needsPhotoOnly: Boolean = false,
    val pieceCount: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
    val tag: String? = null,
    val type: PuzzleType? = null,
    val material: PuzzleMaterial? = null,
    val disposition: PuzzleDisposition? = null,
) {
    val hasActiveSearch: Boolean get() = PuzzleListQuery.hasActiveSearch(searchText)

    val hasSecondaryFilters: Boolean
        get() = PuzzleListQuery.hasSecondaryFilters(
            searchText = searchText,
            missingPiecesOnly = missingPiecesOnly,
            onLoanOnly = onLoanOnly,
            overdueOnly = overdueOnly,
            needsPhotoOnly = needsPhotoOnly,
            pieceCountFilter = pieceCount,
            tagFilter = tag,
            typeFilter = type,
            materialFilter = material,
            dispositionFilter = disposition,
        )

    val hasActiveFilters: Boolean get() = status != PuzzleListStatusFilter.ALL || hasSecondaryFilters

    /** Clears search and every filter except the status tab (iOS `clearSecondaryFilters`). */
    fun clearingSecondary(): PuzzleListFilters = PuzzleListFilters(status = status)

    fun apply(
        puzzles: List<Puzzle>,
        sort: PuzzleListSortOption,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Puzzle> = PuzzleListQuery.apply(
        puzzles = puzzles,
        statusFilter = status,
        searchText = searchText,
        sortOption = sort,
        missingPiecesOnly = missingPiecesOnly,
        onLoanOnly = onLoanOnly,
        overdueOnly = overdueOnly,
        needsPhotoOnly = needsPhotoOnly,
        pieceCountFilter = pieceCount,
        tagFilter = tag,
        typeFilter = type,
        materialFilter = material,
        dispositionFilter = disposition,
        now = now,
        zoneId = zoneId,
    )
}

/** List empty-state copy from iOS `PuzzleList.emptyStateRow` / `emptyStateMessage`. */
object PuzzleListEmptyState {
    const val NEW_COLLECTION_TITLE = "No puzzles yet"
    const val NEW_COLLECTION_MESSAGE = "Tap Add puzzle to start your collection."

    /** True only for an empty collection with no filters, where the Add puzzle button shows. */
    fun showsAddPuzzleAction(totalCount: Int, filters: PuzzleListFilters): Boolean =
        totalCount == 0 && !filters.hasActiveFilters

    fun message(totalCount: Int, filters: PuzzleListFilters): String {
        if (showsAddPuzzleAction(totalCount, filters)) return NEW_COLLECTION_MESSAGE
        val searching = filters.hasActiveSearch
        return when {
            filters.needsPhotoOnly -> if (searching) {
                "No puzzles without photos match your search."
            } else {
                "Every puzzle in this view has a photo."
            }
            filters.missingPiecesOnly -> if (searching) {
                "No puzzles with missing pieces match your search."
            } else {
                "No puzzles flagged with missing pieces."
            }
            filters.overdueOnly -> if (searching) "No overdue loans match your search." else "No puzzles are overdue."
            filters.onLoanOnly -> if (searching) {
                "No puzzles on loan match your search."
            } else {
                "No puzzles are on loan right now."
            }
            filters.pieceCount != PuzzleListPieceCountFilter.ANY -> if (searching) {
                "No puzzles in this piece-count range match your search."
            } else {
                "No puzzles match this piece-count filter."
            }
            else -> filters.status.emptyStateMessage(searching, hasTagFilter = filters.tag != null)
        }
    }
}

/** TalkBack description for a list row, ported from iOS `PuzzleCell.cellAccessibilityLabel`. */
object PuzzleRowAccessibility {
    fun label(puzzle: Puzzle, now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): String {
        val parts = mutableListOf(puzzle.name)
        puzzle.source?.takeIf { it.isNotEmpty() }?.let { parts += it }
        puzzle.pieces?.let { parts += "$it pieces" }
        if (puzzle.rating != PuzzleRating.NONE) {
            parts += puzzle.rating.accessibilityDescription
        }
        if (puzzle.difficulty != PuzzleDifficulty.NONE) {
            parts += puzzle.difficulty.accessibilityDescription
        }
        parts += puzzle.status.accessibilityDescription
        if (puzzle.timesCompleted > 1) parts += "Completed ${puzzle.timesCompleted} times"
        if (puzzle.hasMissingPieces) parts += "Missing pieces"
        if (puzzle.isOnLoan) parts += PuzzleLoanSemantics.listBadgeAccessibilityLabel(puzzle, now, zoneId)
        if (puzzle.tags.isNotEmpty()) parts += "Tags: ${puzzle.tags.joinToString(", ")}"
        when (puzzle.status) {
            PuzzleStatus.COMPLETED -> parts += "Completed ${MediumDateLabel.format(puzzle.completionDate, zoneId)}"
            PuzzleStatus.IN_PROGRESS -> {
                parts += "Started ${MediumDateLabel.format(puzzle.startDate ?: puzzle.completionDate, zoneId)}"
                parts += PuzzleProgressSemantics.displayLabel(puzzle.progressPercent)
                if (PuzzleTimerSemantics.isRunning(puzzle.timerRunningSince, now)) parts += "Timer running"
            }
            else -> Unit
        }
        return parts.joinToString(", ")
    }
}
