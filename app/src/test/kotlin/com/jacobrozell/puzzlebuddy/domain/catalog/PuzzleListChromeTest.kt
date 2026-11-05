package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * List filter state, empty-state copy, and row TalkBack labels. Ports iOS `AccessibilityLabelTests`
 * (rating, status, difficulty) and the `PuzzleCell` / `PuzzleList` copy at tag 1.1.0.
 */
class PuzzleListChromeTest {
    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-06-15T12:00:00Z")

    private fun puzzle(name: String) = Puzzle(name = name, pieces = 500, completionDate = now)

    @Test
    fun ratingStatusAndDifficultyDescriptions() {
        assertEquals("Rating 4.0 out of 5", PuzzleRating.FOUR.accessibilityDescription)
        assertEquals("Rating 4.5 out of 5", PuzzleRating.FOUR_HALF.accessibilityDescription)
        assertEquals("No rating", PuzzleRating.NONE.accessibilityDescription)
        assertEquals("Wishlist, not yet owned", PuzzleStatus.WISHLIST.accessibilityDescription)
        assertEquals("To-Do, not started", PuzzleStatus.TODO.accessibilityDescription)
        assertEquals("In progress", PuzzleStatus.IN_PROGRESS.accessibilityDescription)
        assertEquals("Completed", PuzzleStatus.COMPLETED.accessibilityDescription)
        assertEquals("No difficulty", PuzzleDifficulty.NONE.accessibilityDescription)
        assertEquals("Difficulty 3 out of 5", PuzzleDifficulty.THREE.accessibilityDescription)
    }

    @Test
    fun secondaryFiltersIgnoreStatusAndClearKeepsStatus() {
        val filters = PuzzleListFilters(status = PuzzleListStatusFilter.COMPLETED)
        assertFalse(filters.hasSecondaryFilters)
        assertTrue(filters.hasActiveFilters)

        val busy = filters.copy(searchText = "winter", type = PuzzleType.MYSTERY, overdueOnly = true)
        assertTrue(busy.hasSecondaryFilters)
        assertEquals(PuzzleListFilters(status = PuzzleListStatusFilter.COMPLETED), busy.clearingSecondary())
    }

    @Test
    fun applyUsesEveryFilter() {
        val loaned = puzzle("Lent").copy(isOnLoan = true, loanedToDisplayName = "Mom")
        val home = puzzle("Home")
        assertEquals(
            listOf("Lent"),
            PuzzleListFilters(onLoanOnly = true).apply(listOf(loaned, home), PuzzleListSortOption.NAME, now, zone).map { it.name },
        )
        assertEquals(
            listOf("Home"),
            PuzzleListFilters(searchText = "home").apply(listOf(loaned, home), PuzzleListSortOption.NAME, now, zone).map { it.name },
        )
    }

    @Test
    fun emptyStateForNewCollectionOffersAdd() {
        val filters = PuzzleListFilters()
        assertTrue(PuzzleListEmptyState.showsAddPuzzleAction(0, filters))
        assertEquals("Tap Add puzzle to start your collection.", PuzzleListEmptyState.message(0, filters))
        assertFalse(PuzzleListEmptyState.showsAddPuzzleAction(3, filters))
        assertFalse(PuzzleListEmptyState.showsAddPuzzleAction(0, PuzzleListFilters(status = PuzzleListStatusFilter.TODO)))
    }

    @Test
    fun emptyStateNamesTheActiveFilter() {
        assertEquals("Every puzzle in this view has a photo.", PuzzleListEmptyState.message(4, PuzzleListFilters(needsPhotoOnly = true)))
        assertEquals("No puzzles are overdue.", PuzzleListEmptyState.message(4, PuzzleListFilters(overdueOnly = true)))
        assertEquals(
            "No puzzles on loan match your search.",
            PuzzleListEmptyState.message(4, PuzzleListFilters(onLoanOnly = true, searchText = "x")),
        )
        assertEquals(
            "No puzzles match this piece-count filter.",
            PuzzleListEmptyState.message(4, PuzzleListFilters(pieceCount = PuzzleListPieceCountFilter.THOUSAND)),
        )
        assertEquals("No puzzles use this tag yet.", PuzzleListEmptyState.message(4, PuzzleListFilters(tag = "Cozy")))
        assertTrue(PuzzleListEmptyState.message(4, PuzzleListFilters(status = PuzzleListStatusFilter.TODO)).contains("shelf"))
    }

    @Test
    fun rowLabelListsDetailsInIosOrder() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val active = puzzle("Harbor").copy(
                source = "Ravensburger",
                rating = PuzzleRating.FOUR,
                difficulty = PuzzleDifficulty.THREE,
                status = PuzzleStatus.IN_PROGRESS,
                startDate = Instant.parse("2026-06-01T12:00:00Z"),
                progressPercent = 40,
                timesCompleted = 2,
                hasMissingPieces = true,
                isOnLoan = true,
                loanedToDisplayName = "Mom",
                tags = listOf("Cozy", "Winter"),
                timerRunningSince = now.minusSeconds(60),
            )
            assertEquals(
                "Harbor, Ravensburger, 500 pieces, Rating 4.0 out of 5, Difficulty 3 out of 5, In progress, " +
                    "Completed 2 times, Missing pieces, On loan to Mom, Tags: Cozy, Winter, Started Jun 1, 2026, " +
                    "40% complete, Timer running",
                PuzzleRowAccessibility.label(active, now, zone),
            )

            val done = puzzle("Done").copy(status = PuzzleStatus.COMPLETED)
            assertEquals("Done, 500 pieces, Completed, Completed Jun 15, 2026", PuzzleRowAccessibility.label(done, now, zone))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
