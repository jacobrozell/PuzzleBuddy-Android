package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PuzzleAnalyticsMetadataTest {
    @Test
    fun pieceCountBuckets() {
        assertEquals("unknown", PuzzleAnalyticsMetadata.pieceCountBucket(null))
        assertEquals("under_500", PuzzleAnalyticsMetadata.pieceCountBucket(500))
        assertEquals("500", PuzzleAnalyticsMetadata.pieceCountBucket(600))
        assertEquals("1000", PuzzleAnalyticsMetadata.pieceCountBucket(1000))
        assertEquals("1500_plus", PuzzleAnalyticsMetadata.pieceCountBucket(2000))
        assertEquals("1000", PuzzleAnalyticsMetadata.pieceCountBucket(PuzzleListPieceCountFilter.THOUSAND))
    }

    @Test
    fun completionMetadataIncludesEnrichedFields() {
        val puzzle = Puzzle(
            name = "Sample",
            pieces = 1000,
            rating = PuzzleRating.FOUR,
            difficulty = PuzzleDifficulty.THREE,
            status = PuzzleStatus.COMPLETED,
            hasMissingPieces = true,
        )
        val metadata = PuzzleAnalyticsMetadata.completionMetadata(puzzle, completionNumber = 2)
        assertEquals("2", metadata["completion_number"])
        assertEquals("1000", metadata["piece_count_bucket"])
        assertEquals("3", metadata["difficulty"])
        assertEquals("4", metadata["rating_bucket"])
        assertEquals("true", metadata["has_missing_pieces"])
    }

    @Test
    fun collectionSizeBuckets() {
        assertEquals("0", PuzzleAnalyticsMetadata.collectionSizeBucket(0))
        assertEquals("1", PuzzleAnalyticsMetadata.collectionSizeBucket(1))
        assertEquals("2_5", PuzzleAnalyticsMetadata.collectionSizeBucket(4))
        assertEquals("6_20", PuzzleAnalyticsMetadata.collectionSizeBucket(12))
        assertEquals("21_50", PuzzleAnalyticsMetadata.collectionSizeBucket(30))
        assertEquals("51_plus", PuzzleAnalyticsMetadata.collectionSizeBucket(80))
    }

    @Test
    fun completedCountBuckets() {
        assertEquals("0", PuzzleAnalyticsMetadata.completedCountBucket(0))
        assertEquals("1", PuzzleAnalyticsMetadata.completedCountBucket(1))
        assertEquals("2_5", PuzzleAnalyticsMetadata.completedCountBucket(3))
        assertEquals("6_plus", PuzzleAnalyticsMetadata.completedCountBucket(9))
    }

    @Test
    fun daysSinceLastOpenBuckets() {
        assertEquals("first_open", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(null))
        assertEquals("0", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(0))
        assertEquals("1", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(1))
        assertEquals("2_7", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(5))
        assertEquals("8_30", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(14))
        assertEquals("31_plus", PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(60))
    }

    @Test
    fun collectionSnapshotMetadata() {
        val todo = Puzzle(name = "Todo", pieces = 500, status = PuzzleStatus.TODO)
        val done = Puzzle(name = "Done", pieces = 1000, status = PuzzleStatus.COMPLETED)
        val meta = PuzzleAnalyticsMetadata.collectionSnapshotMetadata(listOf(todo, done))
        assertEquals("2", meta["puzzle_count"])
        assertEquals("1", meta["count_todo"])
        assertEquals("1", meta["count_completed"])
        assertEquals("0", meta["count_wishlist"])
        assertEquals("0", meta["count_abandoned"])
        assertEquals("2_5", meta["collection_size_bucket"])
        assertEquals("1", meta["completed_count_bucket"])
    }

    @Test
    fun userPropertyValues() {
        val puzzles = listOf(
            Puzzle(name = "Done", status = PuzzleStatus.COMPLETED),
        )
        val values = PuzzleAnalyticsMetadata.userPropertyValues(puzzles, onboardingComplete = true)
        assertEquals("1", values["collection_size_bucket"])
        assertEquals("1", values["completed_count_bucket"])
        assertEquals("true", values["has_completed_puzzle"])
        assertEquals("true", values["onboarding_complete"])
    }
}
