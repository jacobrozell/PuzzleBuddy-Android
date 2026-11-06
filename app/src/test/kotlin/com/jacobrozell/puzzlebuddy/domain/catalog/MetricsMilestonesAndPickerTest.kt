package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.support.logging.InMemoryAnalyticsKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Ports iOS `PuzzleDetailMetricsTests`, `CollectionMilestonesTests`, and `PuzzleRandomPickerTests` (tag 1.1.0). */
class MetricsMilestonesAndPickerTest {
    private val now = Instant.parse("2026-06-15T00:00:00Z")

    private fun puzzle(name: String, status: PuzzleStatus, pieces: Int = 500) =
        Puzzle(name = name, pieces = pieces, status = status, completionDate = now)

    private fun stats(puzzles: List<Puzzle>) = CollectionStats.compute(puzzles, ZoneOffset.UTC, now)

    private fun completed(count: Int, piecesEach: Int = 500) =
        (0 until count).map { puzzle("Done $it", PuzzleStatus.COMPLETED, piecesEach) }

    // PuzzleDetailMetricsTests

    @Test
    fun timeBucketLabels() {
        assertEquals("Quick finish", PuzzleDetailMetrics.timeBucketLabel(0))
        assertEquals("Quick finish", PuzzleDetailMetrics.timeBucketLabel(239))
        assertEquals("Weekend puzzle", PuzzleDetailMetrics.timeBucketLabel(240))
        assertEquals("Weekend puzzle", PuzzleDetailMetrics.timeBucketLabel(719))
        assertEquals("Marathon project", PuzzleDetailMetrics.timeBucketLabel(720))
        assertEquals("Marathon project", PuzzleDetailMetrics.timeBucketLabel(2000))
    }

    @Test
    fun hoursPer1000Pieces() {
        val metrics = PuzzleDetailMetrics.compute(1000, PuzzleTime(5, 0))
        assertEquals("Weekend puzzle", metrics.timeBucketLabel)
        assertEquals(5.0, metrics.hoursPer1000Pieces)
        assertEquals("5 hrs per 1,000 pieces", metrics.formattedHoursPer1000Pieces)
    }

    @Test
    fun hoursPer1000PiecesScalesWithPieceCount() {
        assertEquals(5.0, PuzzleDetailMetrics.compute(500, PuzzleTime(2, 30)).hoursPer1000Pieces)
    }

    @Test
    fun computeWithoutTimeReturnsNoDerivedMetrics() {
        val metrics = PuzzleDetailMetrics.compute(1000, null)
        assertNull(metrics.timeBucketLabel)
        assertNull(metrics.hoursPer1000Pieces)
    }

    @Test
    fun computeWithPartialTimeReturnsNoDerivedMetrics() {
        val metrics = PuzzleDetailMetrics.compute(1000, PuzzleTime(hours = 2))
        assertNull(metrics.timeBucketLabel)
        assertNull(metrics.hoursPer1000Pieces)
    }

    @Test
    fun computeTimeBucketWithoutPieces() {
        val metrics = PuzzleDetailMetrics.compute(null, PuzzleTime(1, 0))
        assertEquals("Quick finish", metrics.timeBucketLabel)
        assertNull(metrics.hoursPer1000Pieces)
    }

    @Test
    fun formattedHoursPer1000UsesMinutesForFastPuzzles() {
        assertEquals(
            "30 min per 1,000 pieces",
            PuzzleDetailMetrics.compute(1000, PuzzleTime(0, 30)).formattedHoursPer1000Pieces,
        )
    }

    // CollectionMilestonesTests

    @Test
    fun firstCompletionMilestone() {
        val earned = CollectionMilestones.earned(stats(completed(1)))
        assertTrue(earned.any { it.id == "completed_1" })
    }

    @Test
    fun newlyEarnedExcludesAcknowledged() {
        assertTrue(CollectionMilestones.newlyEarned(stats(completed(1)), setOf("completed_1")).isEmpty())
    }

    @Test
    fun completedThresholds() {
        assertTrue(CollectionMilestones.earned(stats(completed(10))).any { it.id == "completed_10" })
        assertTrue(CollectionMilestones.earned(stats(completed(50))).any { it.id == "completed_50" })
    }

    @Test
    fun piecesAndHoursThresholds() {
        val timed = completed(1, 10_000).single().copy(estimatedTimeSpent = PuzzleTime(100, 0))
        val earned = CollectionMilestones.earned(stats(listOf(timed)))
        assertTrue(earned.any { it.id == "pieces_10000" })
        assertTrue(earned.any { it.id == "hours_100" })
    }

    @Test
    fun yearCompletionThreshold() {
        val puzzles = (1..12).map { month ->
            puzzle("P$month", PuzzleStatus.COMPLETED).copy(
                completionDate = LocalDate.of(2026, month, 15).atStartOfDay(ZoneOffset.UTC).toInstant(),
            )
        }
        val yearStats = CollectionStats.compute(puzzles, ZoneOffset.UTC, Instant.parse("2026-06-01T00:00:00Z"))
        assertTrue(CollectionMilestones.earned(yearStats).any { it.id == "year_completions_12" })
    }

    @Test
    fun acknowledgePersists() {
        val store = InMemoryAnalyticsKeyValueStore()
        val updated = CollectionMilestones.acknowledge(
            store.getStringSet(CollectionMilestones.STORAGE_KEY),
            "completed_10",
        )
        store.putStringSet(CollectionMilestones.STORAGE_KEY, updated)
        assertTrue(store.getStringSet(CollectionMilestones.STORAGE_KEY).contains("completed_10"))
    }

    @Test
    fun excludingDemoDoesNotChangeThresholdIDs() {
        val demo = puzzle("Demo", PuzzleStatus.COMPLETED).copy(isDemo = true)
        val earned = CollectionMilestones.earned(stats(listOf(demo)), excludingDemo = true)
        assertTrue(earned.any { it.id == "completed_1" })
    }

    // PuzzleRandomPickerTests

    @Test
    fun eligibleExcludesWishlistAbandonedAndCompleted() {
        val eligible = PuzzleRandomPicker.eligible(
            listOf(
                puzzle("Wish", PuzzleStatus.WISHLIST),
                puzzle("Shelf", PuzzleStatus.TODO),
                puzzle("Table", PuzzleStatus.IN_PROGRESS),
                puzzle("Done", PuzzleStatus.COMPLETED),
                puzzle("Quit", PuzzleStatus.ABANDONED),
            ),
            includeInProgress = false,
            pieceCountFilter = PuzzleListPieceCountFilter.ANY,
            tagFilter = null,
        )
        assertEquals(listOf("Shelf"), eligible.map { it.name })
    }

    @Test
    fun eligibleIncludesInProgressWhenRequested() {
        val eligible = PuzzleRandomPicker.eligible(
            listOf(puzzle("Shelf", PuzzleStatus.TODO), puzzle("Table", PuzzleStatus.IN_PROGRESS)),
            includeInProgress = true,
            pieceCountFilter = PuzzleListPieceCountFilter.ANY,
            tagFilter = null,
        )
        assertEquals(setOf("Shelf", "Table"), eligible.map { it.name }.toSet())
    }

    @Test
    fun eligibleFiltersByTagAndPieceCount() {
        val eligible = PuzzleRandomPicker.eligible(
            listOf(
                puzzle("Winter", PuzzleStatus.TODO, 1000).copy(tags = listOf("cozy")),
                puzzle("Small", PuzzleStatus.TODO, 300).copy(tags = listOf("cozy")),
            ),
            includeInProgress = false,
            pieceCountFilter = PuzzleListPieceCountFilter.THOUSAND,
            tagFilter = "cozy",
        )
        assertEquals(listOf("Winter"), eligible.map { it.name })
    }

    @Test
    fun pickReturnsMemberOfPool() {
        val pool = listOf(puzzle("A", PuzzleStatus.TODO), puzzle("B", PuzzleStatus.TODO))
        val picked = PuzzleRandomPicker.pick(pool, excluding = null)
        assertTrue(pool.any { it.id == picked?.id })
    }

    @Test
    fun pickReturnsNullForEmptyPool() {
        assertNull(PuzzleRandomPicker.pick(emptyList(), excluding = null))
    }
}
