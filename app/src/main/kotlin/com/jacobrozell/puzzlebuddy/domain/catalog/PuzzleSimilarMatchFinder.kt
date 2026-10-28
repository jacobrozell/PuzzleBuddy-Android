package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object PuzzleSimilarMatchFinder {
    fun findSimilar(
        name: String,
        source: String?,
        pieces: Int?,
        barcode: String?,
        excludingId: String?,
        puzzles: List<Puzzle>,
        limit: Int = 3,
    ): List<Puzzle> {
        val normalizedName = normalize(name)
        val normalizedSource = normalize(source)
        val normalizedBarcode = BarcodeNormalizer.normalize(barcode)
        if (normalizedName.isEmpty() && normalizedSource.isEmpty() && pieces == null) return emptyList()

        val matches = mutableListOf<Puzzle>()
        for (puzzle in puzzles) {
            if (puzzle.id == excludingId) continue
            if (normalizedBarcode != null) {
                val existing = BarcodeNormalizer.normalize(puzzle.barcode)
                if (existing == normalizedBarcode) continue
            }
            val puzzleName = normalize(puzzle.name)
            val puzzleSource = normalize(puzzle.source)
            val nameMatches = normalizedName.isNotEmpty() && puzzleName == normalizedName
            val sourceMatches = puzzleSource == normalizedSource
            val piecesMatch = pieces != null && puzzle.pieces == pieces
            val isSimilar = (nameMatches && sourceMatches) ||
                (nameMatches && piecesMatch) ||
                (sourceMatches && piecesMatch && normalizedName.isNotEmpty() && puzzleName == normalizedName)
            if (isSimilar) matches += puzzle
        }
        return matches.take(limit)
    }

    private fun normalize(value: String?): String = value?.trim()?.lowercase().orEmpty()
}
