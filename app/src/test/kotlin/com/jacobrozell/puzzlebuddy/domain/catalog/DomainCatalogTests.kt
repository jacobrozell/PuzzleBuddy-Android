package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PuzzleListQueryTest {
    private fun puzzle(
        name: String,
        status: PuzzleStatus = PuzzleStatus.TODO,
        pieces: Int? = 1000,
        rating: PuzzleRating = PuzzleRating.NONE,
        barcode: String? = null,
        source: String? = null,
        tags: List<String> = emptyList(),
        hasImage: Boolean = true,
    ) = Puzzle(
        name = name,
        pieces = pieces,
        rating = rating,
        status = status,
        estimatedTimeSpent = null,
        completionDate = Instant.parse("2025-06-01T12:00:00Z"),
        barcode = barcode,
        source = source,
        tags = tags,
        hasImage = hasImage,
    )

    @Test
    fun filtersByStatus() {
        val puzzles = listOf(
            puzzle("A", PuzzleStatus.TODO),
            puzzle("B", PuzzleStatus.COMPLETED),
        )
        val result = PuzzleListQuery.apply(
            puzzles = puzzles,
            statusFilter = PuzzleListStatusFilter.COMPLETED,
            searchText = "",
            sortOption = PuzzleListSortOption.NAME,
        )
        assertEquals(1, result.size)
        assertEquals("B", result.first().name)
    }

    @Test
    fun searchMatchesBarcodeDigits() {
        val puzzles = listOf(puzzle("Mystery", barcode = "012345678905"))
        val result = PuzzleListQuery.search(puzzles, "45678")
        assertEquals(1, result.size)
    }

    @Test
    fun sortByNameAscending() {
        val puzzles = listOf(puzzle("Zebra"), puzzle("Apple"))
        val result = PuzzleListQuery.sort(puzzles, PuzzleListSortOption.NAME)
        assertEquals("Apple", result.first().name)
    }

    @Test
    fun filterNeedsPhoto() {
        val puzzles = listOf(
            puzzle("With", hasImage = true),
            puzzle("Without", hasImage = false),
        )
        val result = PuzzleListQuery.filterNeedsPhoto(puzzles, needsPhotoOnly = true)
        assertEquals(1, result.size)
        assertEquals("Without", result.first().name)
    }

    @Test
    fun pieceCountFilterThousandBand() {
        val puzzles = listOf(
            puzzle("Small", pieces = 500),
            puzzle("Target", pieces = 1000),
            puzzle("Huge", pieces = 2000),
        )
        val result = PuzzleListQuery.filterPieceCount(puzzles, PuzzleListPieceCountFilter.THOUSAND)
        assertEquals(1, result.size)
        assertEquals("Target", result.first().name)
    }
}

class PuzzleTagSemanticsTest {
    @Test
    fun sanitizedTagsDedupesCaseInsensitive() {
        val tags = PuzzleTagSemantics.sanitizedTags(listOf("Holiday", "holiday", "  Gift "))
        assertEquals(listOf("Holiday", "Gift"), tags)
    }
}

class PuzzleProgressSemanticsTest {
    @Test
    fun clampedProgress() {
        assertEquals(0, PuzzleProgressSemantics.clamped(-5))
        assertEquals(100, PuzzleProgressSemantics.clamped(150))
    }

    @Test
    fun statusForProgress() {
        assertEquals(PuzzleStatus.TODO, PuzzleProgressSemantics.statusFor(0))
        assertEquals(PuzzleStatus.COMPLETED, PuzzleProgressSemantics.statusFor(100))
        assertEquals(PuzzleStatus.IN_PROGRESS, PuzzleProgressSemantics.statusFor(42))
    }
}

class CollectionStatsTest {
    @Test
    fun computesCompletedTotals() {
        val puzzles = listOf(
            Puzzle(
                name = "Done",
                pieces = 1000,
                rating = PuzzleRating.FOUR,
                difficulty = PuzzleDifficulty.TWO,
                estimatedTimeSpent = PuzzleTime(2, 30),
                completionDate = Instant.now(),
                status = PuzzleStatus.COMPLETED,
            ),
            Puzzle(
                name = "Shelf",
                pieces = 500,
                estimatedTimeSpent = null,
                completionDate = Instant.now(),
                status = PuzzleStatus.TODO,
            ),
        )
        val stats = CollectionStats.compute(puzzles)
        assertEquals(1, stats.completedCount)
        assertEquals(1000, stats.totalPiecesCompleted)
        assertEquals(1, stats.backlogCount)
        assertEquals(150, stats.totalMinutesPuzzling)
        assertTrue(stats.averageRating != null)
    }
}

class PuzzleDetailMetricsTest {
    @Test
    fun timeBucketLabels() {
        assertEquals("Quick finish", PuzzleDetailMetrics.timeBucketLabel(120))
        assertEquals("Weekend puzzle", PuzzleDetailMetrics.timeBucketLabel(400))
        assertEquals("Marathon project", PuzzleDetailMetrics.timeBucketLabel(900))
    }
}
