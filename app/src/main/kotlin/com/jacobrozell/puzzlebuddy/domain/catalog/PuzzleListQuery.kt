package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class PuzzleListStatusFilter(val title: String) {
    WISHLIST("Wish"),
    TODO("To-Do"),
    IN_PROGRESS("Active"),
    COMPLETED("Done"),
    ABANDONED("Quit"),
    ALL("All"),
    ;

    fun matches(puzzle: Puzzle): Boolean = when (this) {
        WISHLIST -> puzzle.status == PuzzleStatus.WISHLIST
        TODO -> puzzle.status == PuzzleStatus.TODO
        IN_PROGRESS -> puzzle.status == PuzzleStatus.IN_PROGRESS
        COMPLETED -> puzzle.status == PuzzleStatus.COMPLETED
        ABANDONED -> puzzle.status == PuzzleStatus.ABANDONED
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
            WISHLIST -> "Your wishlist is empty. Mark a puzzle Wishlist to track what to buy next."
            TODO -> "No puzzles on your shelf. Add a To-Do puzzle or switch to All."
            IN_PROGRESS -> "Nothing on the table right now. Move a puzzle to In-Progress when you start."
            COMPLETED -> "No completed puzzles yet. Finish one and mark it Completed."
            ABANDONED -> "No abandoned puzzles. Mark one Abandoned if you will not finish it."
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
            PuzzleListStatusFilter.WISHLIST,
            PuzzleListStatusFilter.TODO,
            PuzzleListStatusFilter.IN_PROGRESS,
            PuzzleListStatusFilter.ABANDONED,
            -> NAME
            PuzzleListStatusFilter.COMPLETED,
            PuzzleListStatusFilter.ALL,
            -> COMPLETION_DATE
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
        onLoanOnly: Boolean = false,
        overdueOnly: Boolean = false,
        needsPhotoOnly: Boolean = false,
        pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
        tagFilter: String? = null,
        typeFilter: PuzzleType? = null,
        materialFilter: PuzzleMaterial? = null,
        dispositionFilter: PuzzleDisposition? = null,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Puzzle> {
        val statusFiltered = puzzles.filter { statusFilter.matches(it) }
        val missingFiltered = filterMissingPieces(statusFiltered, missingPiecesOnly)
        val onLoanFiltered = filterOnLoanOnly(missingFiltered, onLoanOnly)
        val overdueFiltered = filterOverdueOnly(onLoanFiltered, overdueOnly, now, zoneId)
        val photoFiltered = filterNeedsPhoto(overdueFiltered, needsPhotoOnly)
        val pieceFiltered = filterPieceCount(photoFiltered, pieceCountFilter)
        val tagFiltered = PuzzleTagIndex.filter(pieceFiltered, tagFilter)
        val typeFiltered = filterType(tagFiltered, typeFilter)
        val materialFiltered = filterMaterial(typeFiltered, materialFilter)
        val dispositionFiltered = filterDisposition(materialFiltered, dispositionFilter)
        val searched = search(dispositionFiltered, searchText)
        return sort(searched, sortOption)
    }

    fun filterMissingPieces(puzzles: List<Puzzle>, missingPiecesOnly: Boolean): List<Puzzle> {
        if (!missingPiecesOnly) return puzzles
        return puzzles.filter { it.hasMissingPieces }
    }

    fun filterOnLoanOnly(puzzles: List<Puzzle>, onLoanOnly: Boolean): List<Puzzle> {
        if (!onLoanOnly) return puzzles
        return puzzles.filter { it.isOnLoan }
    }

    fun filterOverdueOnly(
        puzzles: List<Puzzle>,
        overdueOnly: Boolean,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Puzzle> {
        if (!overdueOnly) return puzzles
        return puzzles.filter { PuzzleLoanSemantics.isOverdue(it, now, zoneId) }
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

    fun filterType(puzzles: List<Puzzle>, typeFilter: PuzzleType?): List<Puzzle> {
        if (typeFilter == null || typeFilter == PuzzleType.NONE) return puzzles
        return puzzles.filter { it.puzzleType == typeFilter }
    }

    fun filterMaterial(puzzles: List<Puzzle>, materialFilter: PuzzleMaterial?): List<Puzzle> {
        if (materialFilter == null || materialFilter == PuzzleMaterial.NONE) return puzzles
        return puzzles.filter { it.material == materialFilter }
    }

    fun filterDisposition(puzzles: List<Puzzle>, dispositionFilter: PuzzleDisposition?): List<Puzzle> {
        if (dispositionFilter == null || dispositionFilter == PuzzleDisposition.NONE) return puzzles
        return puzzles.filter { it.disposition == dispositionFilter }
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
        onLoanOnly: Boolean = false,
        overdueOnly: Boolean = false,
        needsPhotoOnly: Boolean = false,
        pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
        tagFilter: String? = null,
        typeFilter: PuzzleType? = null,
        materialFilter: PuzzleMaterial? = null,
        dispositionFilter: PuzzleDisposition? = null,
    ): Boolean = statusFilter != PuzzleListStatusFilter.ALL ||
        hasSecondaryFilters(
            searchText,
            missingPiecesOnly,
            onLoanOnly,
            overdueOnly,
            needsPhotoOnly,
            pieceCountFilter,
            tagFilter,
            typeFilter,
            materialFilter,
            dispositionFilter,
        )

    fun hasSecondaryFilters(
        searchText: String,
        missingPiecesOnly: Boolean,
        onLoanOnly: Boolean = false,
        overdueOnly: Boolean = false,
        needsPhotoOnly: Boolean = false,
        pieceCountFilter: PuzzleListPieceCountFilter = PuzzleListPieceCountFilter.ANY,
        tagFilter: String? = null,
        typeFilter: PuzzleType? = null,
        materialFilter: PuzzleMaterial? = null,
        dispositionFilter: PuzzleDisposition? = null,
    ): Boolean = hasActiveSearch(searchText) ||
        missingPiecesOnly ||
        onLoanOnly ||
        overdueOnly ||
        needsPhotoOnly ||
        pieceCountFilter != PuzzleListPieceCountFilter.ANY ||
        tagFilter != null ||
        (typeFilter != null && typeFilter != PuzzleType.NONE) ||
        (materialFilter != null && materialFilter != PuzzleMaterial.NONE) ||
        (dispositionFilter != null && dispositionFilter != PuzzleDisposition.NONE)

    fun search(puzzles: List<Puzzle>, query: String): List<Puzzle> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return puzzles
        val normalizedQuery = trimmed.lowercase()
        val digitQuery = trimmed.filter { it.isDigit() }
        return puzzles.filter { puzzle ->
            puzzle.name.contains(trimmed, ignoreCase = true) ||
                puzzle.source?.contains(trimmed, ignoreCase = true) == true ||
                matchesBarcode(puzzle.barcode, normalizedQuery, digitQuery) ||
                puzzle.tags.any { it.contains(trimmed, ignoreCase = true) } ||
                puzzle.purchaseLocation?.contains(trimmed, ignoreCase = true) == true ||
                matchesReleaseYear(puzzle.releaseYear, digitQuery) ||
                puzzle.puzzleType.raw.contains(trimmed, ignoreCase = true) ||
                puzzle.loanedToDisplayName?.contains(trimmed, ignoreCase = true) == true
        }
    }

    fun sort(puzzles: List<Puzzle>, option: PuzzleListSortOption): List<Puzzle> = when (option) {
        PuzzleListSortOption.COMPLETION_DATE ->
            puzzles.sortedByDescending { it.completionDate }
        PuzzleListSortOption.NAME ->
            puzzles.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        PuzzleListSortOption.RATING ->
            puzzles.sortedByDescending { it.rating.value }
        PuzzleListSortOption.DIFFICULTY ->
            puzzles.sortedByDescending { it.difficulty.intValue }
        PuzzleListSortOption.PIECES ->
            puzzles.sortedByDescending { it.pieces ?: -1 }
    }

    fun hasActiveSearch(searchText: String): Boolean = searchText.trim().isNotEmpty()

    private fun matchesReleaseYear(year: Int?, digitQuery: String): Boolean {
        if (year == null || digitQuery.isEmpty()) return false
        return year.toString().contains(digitQuery)
    }

    private fun matchesBarcode(barcode: String?, query: String, digitQuery: String): Boolean {
        if (barcode.isNullOrEmpty()) return false
        if (barcode.lowercase().contains(query)) return true
        if (digitQuery.isEmpty()) return false
        val barcodeDigits = barcode.filter { it.isDigit() }
        return barcodeDigits.contains(digitQuery)
    }
}
