package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

/** Ports iOS `PuzzleListFilterTests` (tag 1.1.0). */
class PuzzleListFilterTest {
    private fun fixture(name: String, pieces: Int = 500, rating: PuzzleRating = PuzzleRating.NONE) =
        Puzzle(name = name, pieces = pieces, rating = rating, completionDate = Instant.EPOCH)

    private fun samplePuzzles() = listOf(
        fixture("Shelf"),
        fixture("Done", 1000).copy(status = PuzzleStatus.COMPLETED),
    )

    private fun filtered(puzzles: List<Puzzle>, filter: PuzzleListStatusFilter) = puzzles.filter(filter::matches)

    @Test
    fun allFilterReturnsEveryPuzzle() {
        assertEquals(listOf("Shelf", "Done"), filtered(samplePuzzles(), PuzzleListStatusFilter.ALL).map { it.name })
    }

    @Test
    fun todoFilterReturnsOnlyTodo() {
        val result = filtered(samplePuzzles(), PuzzleListStatusFilter.TODO)
        assertEquals(listOf("Shelf"), result.map { it.name })
        assertEquals(PuzzleStatus.TODO, result.single().status)
    }

    @Test
    fun completedFilterReturnsOnlyCompleted() {
        val result = filtered(samplePuzzles(), PuzzleListStatusFilter.COMPLETED)
        assertEquals(listOf("Done"), result.map { it.name })
    }

    @Test
    fun inProgressFilterReturnsOnlyInProgress() {
        val puzzles = samplePuzzles() + fixture("On Table", 300).copy(status = PuzzleStatus.IN_PROGRESS)
        assertEquals(listOf("On Table"), filtered(puzzles, PuzzleListStatusFilter.IN_PROGRESS).map { it.name })
    }

    @Test
    fun inProgressEmptyStateMessage() {
        assertTrue(PuzzleListStatusFilter.IN_PROGRESS.emptyStateMessage(hasSearchQuery = false).contains("table"))
    }

    @Test
    fun emptyStateMessages() {
        assertTrue(PuzzleListStatusFilter.TODO.emptyStateMessage(false).contains("shelf"))
        assertTrue(PuzzleListStatusFilter.COMPLETED.emptyStateMessage(false).contains("completed"))
        assertTrue(PuzzleListStatusFilter.ALL.emptyStateMessage(false).contains("Add puzzle"))
        assertTrue(PuzzleListStatusFilter.ALL.emptyStateMessage(true).contains("search"))
    }

    @Test
    fun searchMatchesNameCaseInsensitively() {
        val results = PuzzleListQuery.search(samplePuzzles() + fixture("Alpine Meadow", 300), "alpine")
        assertEquals(listOf("Alpine Meadow"), results.map { it.name })
    }

    @Test
    fun searchMatchesBrandSource() {
        assertEquals(1, PuzzleListQuery.search(listOf(fixture("Untitled").copy(source = "Ravensburger")), "ravens").size)
    }

    @Test
    fun searchMatchesBarcodeDigits() {
        val puzzle = fixture("Barcode Puzzle", 1000).copy(barcode = "4005556197523")
        assertEquals(1, PuzzleListQuery.search(listOf(puzzle), "6197523").size)
    }

    @Test
    fun filterNeedsPhotoOnly() {
        val withPhoto = fixture("Photo").copy(photos = listOf(PuzzlePhoto(sortOrder = 0, createdAt = Instant.EPOCH)))
        val results = PuzzleListQuery.filterNeedsPhoto(listOf(withPhoto, fixture("No Photo")), needsPhotoOnly = true)
        assertEquals(listOf("No Photo"), results.map { it.name })
    }

    @Test
    fun filterPieceCountRanges() {
        val puzzles = listOf(fixture("Small", 300), fixture("Medium", 1000), fixture("Large", 2000))
        assertEquals(
            listOf("Medium"),
            PuzzleListQuery.filterPieceCount(puzzles, PuzzleListPieceCountFilter.THOUSAND).map { it.name },
        )
        assertEquals(
            listOf("Large"),
            PuzzleListQuery.filterPieceCount(puzzles, PuzzleListPieceCountFilter.AT_LEAST_1500).map { it.name },
        )
    }

    @Test
    fun defaultSortUsesNameForTodoTab() {
        assertEquals(PuzzleListSortOption.NAME, PuzzleListSortOption.defaultFor(PuzzleListStatusFilter.TODO))
        assertEquals(PuzzleListSortOption.COMPLETION_DATE, PuzzleListSortOption.defaultFor(PuzzleListStatusFilter.COMPLETED))
    }

    @Test
    fun searchWithEmptyQueryReturnsAll() {
        val puzzles = samplePuzzles()
        assertEquals(puzzles, PuzzleListQuery.search(puzzles, ""))
        assertEquals(puzzles, PuzzleListQuery.search(puzzles, "   "))
    }

    @Test
    fun sortByCompletionDateNewestFirst() {
        val older = fixture("Older", 100).copy(completionDate = Instant.ofEpochSecond(1_000))
        val newer = fixture("Newer", 200).copy(completionDate = Instant.ofEpochSecond(2_000))
        assertEquals(
            listOf("Newer", "Older"),
            PuzzleListQuery.sort(listOf(older, newer), PuzzleListSortOption.COMPLETION_DATE).map { it.name },
        )
    }

    @Test
    fun sortByRatingHighestFirst() {
        val sorted = PuzzleListQuery.sort(
            listOf(fixture("Low", 100, PuzzleRating.TWO), fixture("High", 100, PuzzleRating.FIVE)),
            PuzzleListSortOption.RATING,
        )
        assertEquals(listOf("High", "Low"), sorted.map { it.name })
    }

    @Test
    fun sortByPiecesLargestFirst() {
        val sorted = PuzzleListQuery.sort(listOf(fixture("Small", 300), fixture("Large", 2000)), PuzzleListSortOption.PIECES)
        assertEquals(listOf("Large", "Small"), sorted.map { it.name })
    }

    @Test
    fun sortByNameAscending() {
        val sorted = PuzzleListQuery.sort(listOf(fixture("Zebra"), fixture("Alpha")), PuzzleListSortOption.NAME)
        assertEquals(listOf("Alpha", "Zebra"), sorted.map { it.name })
    }

    @Test
    fun filterMissingPiecesOnly() {
        val results = PuzzleListQuery.filterMissingPieces(
            listOf(fixture("Thrift").copy(hasMissingPieces = true), fixture("New", 1000)),
            missingPiecesOnly = true,
        )
        assertEquals(listOf("Thrift"), results.map { it.name })
    }

    @Test
    fun resultCountLabel() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("Showing 12 of 1,000", PuzzleListQuery.resultCountLabel(12, 1000, hasActiveFilters = true))
            assertEquals("1,000 puzzles", PuzzleListQuery.resultCountLabel(1000, 1000, hasActiveFilters = false))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun searchMatchesTag() {
        assertEquals(1, PuzzleListQuery.search(listOf(fixture("Untitled").copy(tags = listOf("Wysocki", "cozy"))), "wysock").size)
    }

    @Test
    fun filterByTag() {
        val results = PuzzleListQuery.apply(
            puzzles = listOf(fixture("Tagged").copy(tags = listOf("Winter")), fixture("Plain")),
            statusFilter = PuzzleListStatusFilter.ALL,
            searchText = "",
            sortOption = PuzzleListSortOption.NAME,
            tagFilter = "winter",
        )
        assertEquals(listOf("Tagged"), results.map { it.name })
    }

    @Test
    fun hasActiveFiltersIncludesOverdue() {
        assertTrue(PuzzleListQuery.hasActiveFilters(PuzzleListStatusFilter.ALL, "", false, overdueOnly = true))
        assertTrue(PuzzleListQuery.hasSecondaryFilters("", false, overdueOnly = true))
    }

    @Test
    fun hasActiveFiltersIncludesTagFilter() {
        assertTrue(PuzzleListQuery.hasActiveFilters(PuzzleListStatusFilter.ALL, "", false, tagFilter = "Cozy"))
    }

    @Test
    fun hasSecondaryFiltersIgnoresStatusTab() {
        assertFalse(PuzzleListQuery.hasSecondaryFilters("", false, tagFilter = null))
        assertTrue(PuzzleListQuery.hasSecondaryFilters("winter", false, tagFilter = null))
        assertTrue(PuzzleListQuery.hasActiveFilters(PuzzleListStatusFilter.COMPLETED, "", false, tagFilter = null))
    }

    @Test
    fun applyCombinesStatusSearchAndSort() {
        val shelf = fixture("Winter Cabin", 500, PuzzleRating.THREE)
        val done = fixture("Winter Lights", 1000, PuzzleRating.FIVE).copy(status = PuzzleStatus.COMPLETED)
        val results = PuzzleListQuery.apply(
            puzzles = listOf(shelf, done),
            statusFilter = PuzzleListStatusFilter.ALL,
            searchText = "winter",
            sortOption = PuzzleListSortOption.RATING,
        )
        assertEquals(listOf("Winter Lights", "Winter Cabin"), results.map { it.name })
    }
}
