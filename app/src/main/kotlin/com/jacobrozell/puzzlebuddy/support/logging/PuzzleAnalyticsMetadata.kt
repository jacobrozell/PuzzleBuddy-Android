package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import kotlin.math.floor

enum class PuzzleAddSource(val raw: String) {
    MANUAL("manual"),
    BARCODE("barcode"),
    IMPORT("import"),
    DEMO("demo"),
}

object PuzzleAnalyticsMetadata {
    fun pieceCountBucket(pieces: Int?): String = when (pieces) {
        null -> "unknown"
        in 0..500 -> "under_500"
        in 501..749 -> "500"
        in 750..1499 -> "1000"
        else -> "1500_plus"
    }

    fun pieceCountBucket(filter: PuzzleListPieceCountFilter): String = when (filter) {
        PuzzleListPieceCountFilter.ANY -> "any"
        PuzzleListPieceCountFilter.UP_TO_500 -> "under_500"
        PuzzleListPieceCountFilter.THOUSAND -> "1000"
        PuzzleListPieceCountFilter.AT_LEAST_1500 -> "1500_plus"
    }

    fun ratingBucket(rating: PuzzleRating): String {
        if (rating == PuzzleRating.NONE) return "none"
        return when (floor(rating.value).toInt()) {
            1, 2 -> "1_2"
            3 -> "3"
            4 -> "4"
            else -> "5"
        }
    }

    fun metadata(
        puzzle: Puzzle,
        addSource: PuzzleAddSource? = null,
    ): Map<String, String> {
        val values = linkedMapOf(
            "puzzle_status" to puzzle.status.raw,
            "piece_count_bucket" to pieceCountBucket(puzzle.pieces),
            "has_photo" to if (puzzle.hasImage) "true" else "false",
            "photo_count" to if (puzzle.hasImage) "1" else "0",
        )
        if (addSource != null) {
            values["add_source"] = addSource.raw
        }
        return values
    }

    fun completionMetadata(
        puzzle: Puzzle,
        completionNumber: Int,
    ): Map<String, String> = metadata(puzzle).toMutableMap().apply {
        put("completion_number", completionNumber.toString())
        put("puzzle_type", "None")
        put("difficulty", puzzle.difficulty.raw)
        put("rating_bucket", ratingBucket(puzzle.rating))
        put("has_missing_pieces", if (puzzle.hasMissingPieces) "true" else "false")
    }

    fun statusChangedMetadata(
        from: PuzzleStatus,
        to: PuzzleStatus,
        pieces: Int?,
    ): Map<String, String> = mapOf(
        "status_from" to from.raw,
        "status_to" to to.raw,
        "piece_count_bucket" to pieceCountBucket(pieces),
    )
}
