package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Ports iOS `CollectionStatsTests` (tag 1.1.0). */
class CollectionStatsParityTest {
    private val zone = ZoneOffset.UTC
    private val reference = Instant.parse("2026-06-15T00:00:00Z")

    private fun day(year: Int, month: Int, dayOfMonth: Int): Instant =
        LocalDate.of(year, month, dayOfMonth).atStartOfDay(zone).toInstant()

    private fun puzzle(
        name: String,
        pieces: Int,
        status: PuzzleStatus,
        rating: PuzzleRating = PuzzleRating.NONE,
    ) = Puzzle(name = name, pieces = pieces, rating = rating, status = status, completionDate = Instant.now())

    private fun compute(vararg puzzles: Puzzle) = CollectionStats.compute(puzzles.toList(), zone, reference)

    @Test
    fun computeEmptyCollection() {
        val stats = compute()
        assertEquals(0, stats.completedCount)
        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.inProgressCount)
        assertEquals(0, stats.totalPiecesCompleted)
        assertEquals(0, stats.totalMinutesPuzzling)
        assertEquals(0, stats.backlogCount)
        assertEquals(0, stats.wishlistCount)
        assertEquals(0, stats.abandonedCount)
        assertEquals(0, stats.missingPiecesCount)
        assertNull(stats.averageRating)
        assertNull(stats.averageDaysToComplete)
        assertNull(stats.favoritePieceCount)
        assertEquals(0, stats.completionsThisMonth)
        assertEquals(0, stats.completionsThisYear)
        assertNull(stats.biggestCompletedPieces)
        assertNull(stats.smallestCompletedPieces)
        assertTrue(stats.topTags.isEmpty())
    }

    @Test
    fun computeAggregatesCompletedAndBacklog() {
        val completed = puzzle("Done", 500, PuzzleStatus.COMPLETED, PuzzleRating.FOUR).copy(
            estimatedTimeSpent = PuzzleTime(2, 30),
            completionDate = reference,
        )
        val todo = puzzle("Shelf", 1000, PuzzleStatus.TODO)

        val stats = compute(completed, todo)

        assertEquals(1, stats.completedCount)
        assertEquals(2, stats.totalCount)
        assertEquals(0, stats.inProgressCount)
        assertEquals(500, stats.totalPiecesCompleted)
        assertEquals(150, stats.totalMinutesPuzzling)
        assertEquals(1, stats.backlogCount)
        assertEquals(0, stats.missingPiecesCount)
        assertEquals(4.0, stats.averageRating)
        assertEquals(500, stats.favoritePieceCount)
        assertEquals(1, stats.completionsThisMonth)
        assertEquals(1, stats.completionsThisYear)
        assertEquals(500, stats.biggestCompletedPieces)
        assertEquals(500, stats.smallestCompletedPieces)
    }

    @Test
    fun computeIgnoresPuzzlesWithoutTimeOrRating() {
        val stats = compute(puzzle("No time", 300, PuzzleStatus.COMPLETED).copy(completionDate = reference))
        assertEquals(0, stats.totalMinutesPuzzling)
        assertNull(stats.averageRating)
    }

    @Test
    fun minutesSpentHoursOnly() {
        val stats = compute(
            puzzle("Hours only", 500, PuzzleStatus.COMPLETED).copy(estimatedTimeSpent = PuzzleTime(hours = 2)),
        )
        assertEquals(120, stats.totalMinutesPuzzling)
    }

    @Test
    fun minutesSpentMinutesOnly() {
        val stats = compute(
            puzzle("Minutes only", 500, PuzzleStatus.COMPLETED).copy(estimatedTimeSpent = PuzzleTime(minutes = 45)),
        )
        assertEquals(45, stats.totalMinutesPuzzling)
    }

    @Test
    fun favoritePieceCountUsesMode() {
        assertEquals(500, CollectionStats.favoritePieceCount(listOf(500, 1000, 500, 300)))
    }

    @Test
    fun favoritePieceCountFallsBackToMedianWhenAllUnique() {
        assertEquals(500, CollectionStats.favoritePieceCount(listOf(300, 500, 1000)))
    }

    @Test
    fun formatHours() {
        assertEquals("0 hours", CollectionStats.formatHours(0))
        assertEquals("45 minutes", CollectionStats.formatHours(45))
        assertEquals("1.5 hours", CollectionStats.formatHours(90))
        assertEquals("142 hours", CollectionStats.formatHours(8520))
    }

    @Test
    fun inProgressExcludedFromBacklogAndCompletedCounts() {
        val stats = compute(
            puzzle("Shelf", 500, PuzzleStatus.TODO),
            puzzle("Table", 300, PuzzleStatus.IN_PROGRESS),
            puzzle("Done", 1000, PuzzleStatus.COMPLETED).copy(completionDate = reference),
        )
        assertEquals(1, stats.backlogCount)
        assertEquals(1, stats.completedCount)
        assertEquals(3, stats.totalCount)
        assertEquals(1, stats.inProgressCount)
    }

    @Test
    fun computeCountsMissingPieces() {
        val stats = compute(
            puzzle("Thrift", 500, PuzzleStatus.TODO).copy(hasMissingPieces = true),
            puzzle("New", 1000, PuzzleStatus.TODO),
        )
        assertEquals(1, stats.missingPiecesCount)
        assertEquals(2, stats.totalCount)
    }

    @Test
    fun computeTopTags() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.TODO).copy(tags = listOf("Cozy", "Winter")),
            puzzle("B", 500, PuzzleStatus.TODO).copy(tags = listOf("cozy")),
        )
        assertEquals(2, stats.topTags.size)
        assertEquals("Cozy", stats.topTags.first().name)
        assertEquals(2, stats.topTags.first().count)
    }

    @Test
    fun replayLogCountsWhilePuzzleIsInProgress() {
        val again = puzzle("Again", 500, PuzzleStatus.IN_PROGRESS).copy(
            completions = listOf(PuzzleCompletion(completionNumber = 1, completedAt = reference)),
            timesCompleted = 1,
        )

        val stats = compute(again)

        assertEquals(0, stats.completedCount)
        assertEquals(1, stats.completionsThisMonth)
        assertEquals(1, stats.completionsThisYear)
        assertEquals(1, stats.completionsByMonthThisYear[5])
        assertEquals(1, CollectionStats.monthlyCompletionCounts(listOf(again), 2026, zone)[5])
    }

    @Test
    fun completionsFilterByMonthAndYear() {
        val stats = compute(
            puzzle("June", 500, PuzzleStatus.COMPLETED).copy(completionDate = reference),
            puzzle("Old", 500, PuzzleStatus.COMPLETED).copy(completionDate = day(2024, 12, 1)),
        )
        assertEquals(1, stats.completionsThisMonth)
        assertEquals(1, stats.completionsThisYear)
        assertEquals(2, stats.completedCount)
    }

    @Test
    fun computeAverageDaysToComplete() {
        val stats = compute(
            puzzle("Done", 500, PuzzleStatus.COMPLETED).copy(
                startDate = day(2026, 6, 11),
                completionDate = reference,
            ),
        )
        assertEquals(4.0, stats.averageDaysToComplete)
        assertEquals("About 4 days", stats.formattedAverageDaysToComplete)
    }

    @Test
    fun computeWishlistAndAbandonedCounts() {
        val stats = compute(
            puzzle("Want", 500, PuzzleStatus.WISHLIST),
            puzzle("Quit", 500, PuzzleStatus.ABANDONED),
        )
        assertEquals(1, stats.wishlistCount)
        assertEquals(1, stats.abandonedCount)
    }

    @Test
    fun computeTopPurchaseLocations() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.TODO).copy(purchaseLocation = "Goodwill"),
            puzzle("B", 500, PuzzleStatus.TODO).copy(purchaseLocation = "Goodwill"),
            puzzle("C", 500, PuzzleStatus.TODO).copy(purchaseLocation = "Amazon"),
        )
        assertEquals("Goodwill", stats.topPurchaseLocations.first())
    }

    @Test
    fun computeTotalSpendUsesDominantCurrency() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.COMPLETED).copy(purchasePrice = 19.99, purchaseCurrencyCode = "USD"),
            puzzle("B", 500, PuzzleStatus.TODO).copy(purchasePrice = 25.0, purchaseCurrencyCode = "USD"),
            puzzle("C", 500, PuzzleStatus.TODO).copy(purchasePrice = 100.0, purchaseCurrencyCode = "EUR"),
        )
        assertEquals("USD", stats.spendCurrencyCode)
        assertEquals(44.99, stats.totalSpend ?: 0.0, 0.001)
    }

    @Test
    fun computeFavoriteBrand() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.COMPLETED).copy(source = "Ravensburger"),
            puzzle("B", 500, PuzzleStatus.TODO).copy(source = "ravensburger"),
            puzzle("C", 500, PuzzleStatus.TODO).copy(source = "Buffalo"),
        )
        assertEquals("Ravensburger", stats.favoriteBrand)
    }

    @Test
    fun computeAverageDifficulty() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.COMPLETED).copy(difficulty = PuzzleDifficulty.TWO),
            puzzle("B", 500, PuzzleStatus.TODO).copy(difficulty = PuzzleDifficulty.FOUR),
            puzzle("C", 500, PuzzleStatus.TODO),
        )
        assertEquals(3.0, stats.averageDifficulty ?: 0.0, 0.001)
        assertEquals("3.0 / 5", stats.formattedAverageDifficulty)
    }

    @Test
    fun computeAverageInProgressPercent() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.IN_PROGRESS).copy(progressPercent = 40),
            puzzle("B", 500, PuzzleStatus.IN_PROGRESS).copy(progressPercent = 60),
        )
        assertEquals(50, stats.averageInProgressPercent)
    }

    @Test
    fun computeReplayedPuzzleCount() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.COMPLETED).copy(timesCompleted = 3),
            puzzle("B", 500, PuzzleStatus.COMPLETED).copy(timesCompleted = 1),
        )
        assertEquals(1, stats.replayedPuzzleCount)
    }

    @Test
    fun computeAverageHoursPer1000Pieces() {
        val stats = compute(
            puzzle("A", 1000, PuzzleStatus.COMPLETED).copy(
                estimatedTimeSpent = PuzzleTime(5, 0),
                completionDate = reference,
            ),
        )
        assertEquals(5.0, stats.averageHoursPer1000Pieces ?: 0.0, 0.001)
    }

    @Test
    fun computePaceBuckets() {
        val stats = compute(
            puzzle("Quick", 300, PuzzleStatus.COMPLETED).copy(estimatedTimeSpent = PuzzleTime(2, 0)),
            puzzle("Marathon", 2000, PuzzleStatus.COMPLETED).copy(estimatedTimeSpent = PuzzleTime(20, 0)),
        )
        assertEquals(2, stats.paceBuckets.size)
        assertEquals("Quick finish", stats.paceBuckets.first().label)
        assertEquals(1, stats.paceBuckets.first().count)
        assertEquals("Marathon project", stats.paceBuckets.last().label)
    }

    @Test
    fun computePurchaseLocationCounts() {
        val stats = compute(
            puzzle("A", 500, PuzzleStatus.TODO).copy(purchaseLocation = "Goodwill"),
            puzzle("B", 500, PuzzleStatus.TODO).copy(purchaseLocation = "goodwill"),
            puzzle("C", 500, PuzzleStatus.TODO).copy(purchaseLocation = "Amazon"),
        )
        assertEquals("Goodwill", stats.purchaseLocationCounts.first().name)
        assertEquals(2, stats.purchaseLocationCounts.first().count)
        assertEquals(2, stats.purchaseLocationCounts.size)
    }

    @Test
    fun computeCompletionsByMonthThisYear() {
        val stats = compute(
            puzzle("June", 500, PuzzleStatus.COMPLETED).copy(completionDate = reference),
            puzzle("Jan", 500, PuzzleStatus.COMPLETED).copy(completionDate = day(2026, 1, 5)),
            puzzle("Old", 500, PuzzleStatus.COMPLETED).copy(completionDate = day(2025, 6, 1)),
        )
        assertEquals(12, stats.completionsByMonthThisYear.size)
        assertEquals(1, stats.completionsByMonthThisYear[5])
        assertEquals(1, stats.completionsByMonthThisYear[0])
        assertEquals(2, stats.completionsByMonthThisYear.sum())
        assertEquals(1, stats.mostProductiveMonthThisYear?.second)
    }
}
