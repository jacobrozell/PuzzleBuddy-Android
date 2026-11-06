package com.jacobrozell.puzzlebuddy.domain.share

import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object PuzzleCollectionShare {
    fun makeCaption(puzzles: List<Puzzle>): String? {
        if (puzzles.isEmpty()) return null
        val caption = PuzzleShareSummary.make(CollectionStats.compute(puzzles), puzzles)
        return caption.ifEmpty { null }
    }
}
