package com.jacobrozell.puzzlebuddy.domain

import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.catalog.DemoDataCatalog
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListQuery
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleRandomPicker
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleSimilarMatchFinder
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleCollectionCollageLayout
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleShareSummary
import com.jacobrozell.puzzlebuddy.domain.surface.AppLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * Ports iOS `PuzzleShareTests`, `PuzzleSimilarMatchFinderTests`, `DemoDataCatalogTests`,
 * `PuzzleMetadataEnumsTests`, and the pure parts of `PuzzleLoanAndFriendTests` (tag 1.1.0).
 * Collage rendering and share payload tests wait for WP4.7.
 */
class ShareLoanDemoAndEnumsTest {
    private val zone = ZoneOffset.UTC
    private val newYear = Instant.ofEpochSecond(1_704_067_200)

    private fun fixture(name: String, pieces: Int = 500, status: PuzzleStatus = PuzzleStatus.TODO) =
        Puzzle(name = name, pieces = pieces, status = status, completionDate = newYear)

    // PuzzleShareTests

    @Test
    fun gridDimensions() {
        assertEquals(1, PuzzleCollectionCollageLayout.gridDimensions(1).columns)
        assertEquals(2, PuzzleCollectionCollageLayout.gridDimensions(4).columns)
        assertEquals(3, PuzzleCollectionCollageLayout.gridDimensions(8).columns)
        assertEquals(12, PuzzleCollectionCollageLayout.gridDimensions(20).displayed)
    }

    @Test
    fun shareSummaryIncludesCounts() {
        val puzzles = listOf(
            fixture("A", status = PuzzleStatus.COMPLETED),
            fixture("B", status = PuzzleStatus.IN_PROGRESS),
            fixture("C"),
        )
        val summary = PuzzleShareSummary.make(CollectionStats.compute(puzzles, zone, newYear), puzzles)
        assertTrue(summary.contains("3 puzzles"))
        assertTrue(summary.contains("1 completed"))
        assertTrue(summary.contains("1 in progress"))
        assertTrue(summary.contains("A"))
    }

    @Test
    fun shareFooterAndPlayLinks() {
        assertEquals("jacobrozell.github.io/PuzzleBuddy", AppLinks.shareFooterLabel)
        assertTrue(AppLinks.playStoreMarket.endsWith("id=com.jacobrozell.puzzlebuddy"))
        assertTrue(AppLinks.playStoreWeb.endsWith("id=com.jacobrozell.puzzlebuddy"))
    }

    // PuzzleSimilarMatchFinderTests

    @Test
    fun findsMatchByNameBrandAndPieces() {
        val existing = fixture("Winter Lights", 1000).copy(source = "Galison")
        val matches = PuzzleSimilarMatchFinder.findSimilar(
            name = "Winter Lights",
            source = "Galison",
            pieces = 1000,
            barcode = "999999999999",
            excludingId = null,
            puzzles = listOf(existing),
        )
        assertEquals(listOf(existing.id), matches.map { it.id })
    }

    @Test
    fun skipsExactBarcodeDuplicate() {
        val existing = fixture("Winter Lights", 1000).copy(barcode = "012345678905")
        val matches = PuzzleSimilarMatchFinder.findSimilar(
            name = "Winter Lights",
            source = "Galison",
            pieces = 1000,
            barcode = "012345678905",
            excludingId = null,
            puzzles = listOf(existing),
        )
        assertTrue(matches.isEmpty())
    }

    // DemoDataCatalogTests

    @Test
    fun demoPuzzlesProduceShowcaseStats() {
        val now = Instant.parse("2026-06-20T12:00:00Z")
        val puzzles = DemoDataCatalog.makePuzzles(now, zone)
        assertEquals(DemoDataCatalog.PUZZLE_COUNT, puzzles.size)

        val stats = CollectionStats.compute(puzzles, zone, now)
        assertTrue(stats.completedCount >= 7)
        assertTrue(stats.totalPiecesCompleted >= 7_000)
        assertTrue(stats.totalMinutesPuzzling >= 3_000)
        assertTrue(stats.completionsThisYear >= 7)
        assertTrue(stats.inProgressCount >= 2)
        assertTrue(stats.backlogCount >= 2)
        assertEquals(1, stats.wishlistCount)
        assertEquals(1, stats.abandonedCount)
        assertEquals(1, stats.missingPiecesCount)
        assertEquals(1, stats.onLoanCount)
        assertEquals(1, stats.replayedPuzzleCount)
        assertNotNull(stats.averageRating)
        assertNotNull(stats.averageDaysToComplete)
        assertNotNull(stats.totalSpend)
        assertEquals("Ravensburger", stats.favoriteBrand)
        assertFalse(stats.paceBuckets.isEmpty())
        assertFalse(stats.topTags.isEmpty())
        assertFalse(stats.purchaseLocationCounts.isEmpty())
        assertTrue(stats.completionsByMonthThisYear.sum() > 1)
    }

    @Test
    fun featuredDemoPuzzlesKeepMarketingNames() {
        val names = DemoDataCatalog.makePuzzles().map { it.name }.toSet()
        assertTrue(names.contains(DemoDataCatalog.DUPLICATE_CHECK_PUZZLE_NAME))
        assertTrue(names.contains(DemoDataCatalog.COMPLETED_PUZZLE_NAME))
    }

    // PuzzleMetadataEnumsTests

    @Test
    fun shapeSelectableCasesExcludeNone() {
        assertFalse(PuzzleShape.selectableCases.contains(PuzzleShape.NONE))
        assertTrue(PuzzleShape.selectableCases.contains(PuzzleShape.ROUND))
    }

    @Test
    fun cutTypeAccessibilityDescription() {
        assertEquals("Cut type Grid", PuzzleCutType.GRID.accessibilityDescription)
        assertEquals("No cut type", PuzzleCutType.NONE.accessibilityDescription)
    }

    @Test
    fun shapeRoundTripsRawValue() {
        assertEquals(PuzzleShape.IRREGULAR, PuzzleShape.fromRaw(PuzzleShape.IRREGULAR.raw))
    }

    // PuzzleLoanAndFriendTests (pure parts)

    @Test
    fun overdueSemanticsAndFilter() {
        val overdue = fixture("Late", 100).copy(isOnLoan = true, dueBackDate = newYear.minusSeconds(86_400))
        val dueToday = fixture("Today", 100).copy(isOnLoan = true, dueBackDate = newYear)
        val home = fixture("Home", 100)

        assertTrue(PuzzleLoanSemantics.isOverdue(overdue, newYear, zone))
        assertFalse(PuzzleLoanSemantics.isOverdue(dueToday, newYear, zone))
        assertFalse(PuzzleLoanSemantics.isOverdue(home, newYear, zone))
        assertEquals("Overdue", PuzzleLoanSemantics.listBadgeTitle(overdue, newYear, zone))
        assertEquals("On loan", PuzzleLoanSemantics.listBadgeTitle(dueToday, newYear, zone))
        assertTrue(PuzzleLoanSemantics.dueBackDisplayValue(overdue, newYear, zone)?.contains("Overdue") == true)

        val filtered = PuzzleListQuery.filterOverdueOnly(listOf(overdue, dueToday, home), true, newYear, zone)
        assertEquals(listOf("Late"), filtered.map { it.name })
    }

    @Test
    fun filterOnLoanOnly() {
        val out = fixture("Out", 200).copy(isOnLoan = true, loanedToDisplayName = "Mom")
        assertEquals(listOf("Out"), PuzzleListQuery.filterOnLoanOnly(listOf(fixture("Home", 100), out), true).map { it.name })
    }

    @Test
    fun searchMatchesLoanedToName() {
        val puzzle = fixture("Skyline", 1000).copy(isOnLoan = true, loanedToDisplayName = "Mom")
        assertEquals(1, PuzzleListQuery.search(listOf(puzzle), "mom").size)
    }

    @Test
    fun pickNextExcludesOnLoan() {
        val eligible = PuzzleRandomPicker.eligible(
            listOf(fixture("Shelf"), fixture("Lent").copy(isOnLoan = true)),
            includeInProgress = false,
            pieceCountFilter = PuzzleListPieceCountFilter.ANY,
            tagFilter = null,
        )
        assertEquals(listOf("Shelf"), eligible.map { it.name })
    }

    @Test
    fun collectionStatsOnLoanCount() {
        val stats = CollectionStats.compute(listOf(fixture("A", 100).copy(isOnLoan = true), fixture("B", 100)), zone, newYear)
        assertEquals(1, stats.onLoanCount)
    }
}
