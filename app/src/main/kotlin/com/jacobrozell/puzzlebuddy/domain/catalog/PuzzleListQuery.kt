package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

enum class PuzzleListStatusFilter(val title: String) {
    TODO("To-Do"),
    IN_PROGRESS("In-Progress"),
    COMPLETED("Completed"),
    ALL("All"),
    ;

    fun matches(puzzle: Puzzle): Boolean = when (this) {
        TODO -> puzzle.status == PuzzleStatus.TODO
        IN_PROGRESS -> puzzle.status == PuzzleStatus.IN_PROGRESS
        COMPLETED -> puzzle.status == PuzzleStatus.COMPLETED
        ALL -> true
    }

    fun emptyStateMessage(hasSearchQuery: Boolean, hasTagFilter: Boolean = false): String {
        if (hasTagFilter) {
            return if (hasSearchQuery) {
                "No puzzles with this tag match your search."
            } else {
                "No puzzles use this tag yet."
            }
        }
        if (hasSearchQuery) {
            return "No puzzles match your search. Try a different name, brand, barcode, or clear the search field."
        }
        return when (this) {
            TODO -> "No puzzles on your shelf. Add a To-Do puzzle or switch to All."
            IN_PROGRESS -> "Nothing on the table right now. Move a puzzle to In-Progress when you start."
            COMPLETED -> "No completed puzzles yet. Finish one and mark it Completed."
            ALL -> "No puzzles yet. Tap Add puzzle to start your collection."
        }
    }
}

enum class PuzzleListPieceCountFilter(val title: String, val accessibilityLabel: String) {
    ANY("Any", "Any piece count"),
    UP_TO_500("≤500", "500 pieces or fewer"),
    THOUSAND("1000", "Around 1000 pieces"),
    AT_LEAST_1500("1500+", "1500 pieces or more"),
    ;

    fun matches(puzzle: Puzzle): Boolean {
        val pieces = puzzle.pieces ?: return false
        return when (this) {
            ANY -> true
            UP_TO_500 -> pieces <= 500
            THOUSAND -> pieces in 751..1249
            AT_LEAST_1500 -> pieces >= 1500
        }
    }
}

enum class PuzzleListSortOption(val title: String, val accessibilityLabel: String) {
    COMPLETION_DATE("Date", "Sort by completion date, newest first"),
    NAME("Name", "Sort by name, A to Z"),
    RATING("Rating", "Sort by rating, highest first"),
    DIFFICULTY("Difficulty", "Sort by difficulty, highest first"),
    PIECES("Pieces", "Sort by piece count, largest first"),
    ;

    companion object {
        fun defaultFor(statusFilter: PuzzleListStatusFilter): PuzzleListSortOption = when (statusFilter) {
            PuzzleListStatusFilter.TODO, PuzzleListStatusFilter.IN_PROGRESS -> NAME
            PuzzleListStatusFilter.COMPLETED, PuzzleListStatusFilter.ALL -> COMPLETION_DATE
        }
    }
}

object PuzzleListQuery {
    fun apply(
        puzzles: List<Puzzle>,
        statusFilter: PuzzleListStatusFilter,
        searchText: String,
        sortOption: PuzzleListSortOption,
        missingPiecesOnly: Boolean = false,
        needsPhotoOnly: Boolean = false,
        pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
        tagFilter: String? = null,
    ): List<Puzzle> {
        val statusFiltered = puzzles.filter { statusFilter.matches(it) }
        val missingFiltered = filterMissingPieces(statusFiltered, missingPiecesOnly)
        val photoFiltered = filterNeedsPhoto(missingFiltered, needsPhotoOnly)
        val pieceFiltered = filterPieceCount(photoFiltered, pieceCountFilter)
        val tagFiltered = PuzzleTagIndex.filter(pieceFiltered, tagFilter)
        val searched = search(tagFiltered, searchText)
        return sort(searched, sortOption)
    }

    fun filterMissingPieces(puzzles: List<Puzzle>, missingPiecesOnly: Boolean): List<Puzzle> {
        if (!missingPiecesOnly) return puzzles
        return puzzles.filter { it.hasMissingPieces }
    }

    fun filterNeedsPhoto(puzzles: List<Puzzle>, needsPhotoOnly: Boolean): List<Puzzle> {
        if (!needsPhotoOnly) return puzzles
        return puzzles.filter { !it.hasImage }
    }

    fun filterPieceCount(
        puzzles: List<Puzzle>,
        pieceCountFilter: PuzzleListPieceCountFilter,
    ): List<Puzzle> {
        if (pieceCountFilter == PuzzleListPieceCountFilter.ANY) return puzzles
        return puzzles.filter { pieceCountFilter.matches(it) }
    }

    fun resultCountLabel(displayedCount: Int, totalCount: Int, hasActiveFilters: Boolean): String {
        if (totalCount == 0) return ""
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val total = formatter.format(totalCount)
        if (!hasActiveFilters || displayedCount == totalCount) return "$total puzzles"
        val shown = formatter.format(displayedCount)
        return "Showing $shown of $total"
    }

    fun hasActiveFilters(
        statusFilter: PuzzleListStatusFilter,
        searchText: String,
        missingPiecesOnly: Boolean,
        needsPhotoOnly: Boolean = false,
        pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
        tagFilter: String? = null,
    ): Boolean = statusFilter != PuzzleListStatusFilter.ALL ||
        hasActiveSearch(searchText) ||
        missingPiecesOnly ||
        needsPhotoOnly ||
        pieceCountFilter != PuzzleListPieceCountFilter.ANY ||
        tagFilter != null

    fun search(puzzles: List<Puzzle>, query: String): List<Puzzle> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return puzzles
        val normalizedQuery = trimmed.lowercase()
        val digitQuery = trimmed.filter { it.isDigit() }
        return puzzles.filter { puzzle ->
            puzzle.name.contains(trimmed, ignoreCase = true) ||
                puzzle.source?.contains(trimmed, ignoreCase = true) == true ||
                matchesBarcode(puzzle.barcode, normalizedQuery, digitQuery) ||
                puzzle.tags.any { it.contains(trimmed, ignoreCase = true) }
        }
    }

    fun sort(puzzles: List<Puzzle>, option: PuzzleListSortOption): List<Puzzle> = when (option) {
        PuzzleListSortOption.COMPLETION_DATE ->
            puzzles.sortedByDescending { it.completionDate }
        PuzzleListSortOption.NAME ->
            puzzles.sortedBy { it.name.lowercase() }
        PuzzleListSortOption.RATING ->
            puzzles.sortedByDescending { it.rating.value }
        PuzzleListSortOption.DIFFICULTY ->
            puzzles.sortedByDescending { it.difficulty.intValue }
        PuzzleListSortOption.PIECES ->
            puzzles.sortedByDescending { it.pieces ?: -1 }
    }

    fun hasActiveSearch(searchText: String): Boolean = searchText.trim().isNotEmpty()

    private fun matchesBarcode(barcode: String?, query: String, digitQuery: String): Boolean {
        if (barcode.isNullOrEmpty()) return false
        if (barcode.lowercase().contains(query)) return true
        if (digitQuery.isEmpty()) return false
        val barcodeDigits = barcode.filter { it.isDigit() }
        return barcodeDigits.contains(digitQuery)
    }
}
