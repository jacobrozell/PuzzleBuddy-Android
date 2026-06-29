package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object PuzzleDuplicateChecker {
    fun findDuplicate(
        barcode: String?,
        excludingId: String?,
        puzzles: List<Puzzle>,
    ): Puzzle? {
        val normalized = BarcodeNormalizer.normalize(barcode) ?: return null
        return puzzles.firstOrNull { puzzle ->
            puzzle.id != excludingId &&
                BarcodeNormalizer.normalize(puzzle.barcode)?.let { it == normalized } == true
        }
    }
}
