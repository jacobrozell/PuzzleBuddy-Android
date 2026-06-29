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
}
