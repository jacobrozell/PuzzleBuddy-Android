package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object PuzzleDuplicateChecker {
    fun findDuplicate(
        barcode: String?,
        excludingId: String?,
        puzzles: List<Puzzle>,
    ): Puzzle? {
        if (BarcodeNormalizer.normalize(barcode) == null) return null
        return puzzles.firstOrNull { puzzle ->
            puzzle.id != excludingId && BarcodeNormalizer.sameProduct(puzzle.barcode, barcode)
        }
    }
}
