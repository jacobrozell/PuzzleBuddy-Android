package com.jacobrozell.puzzlebuddy.domain.share

import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object PuzzleShareSummary {
    fun make(stats: CollectionStats, puzzles: List<Puzzle>): String {
        val lines = mutableListOf(
            "My Puzzle Buddy collection",
            statsLine(stats),
        )
        val featured = puzzles
            .sortedWith(
                compareByDescending<Puzzle> { it.hasImage }
                    .thenByDescending { it.completionDate },
            )
            .take(6)
            .map { it.name }
            .filter { it.isNotEmpty() }
        if (featured.isNotEmpty()) {
            lines += featured.joinToString(" · ")
        }
        lines += "Tracked with Puzzle Buddy"
        return lines.joinToString("\n")
    }

    fun statsLine(stats: CollectionStats): String {
        val parts = mutableListOf("${stats.totalCount} puzzles")
        if (stats.completedCount > 0) parts += "${stats.completedCount} completed"
        if (stats.inProgressCount > 0) parts += "${stats.inProgressCount} in progress"
        if (stats.backlogCount > 0) parts += "${stats.backlogCount} on the shelf"
        return parts.joinToString(" · ")
    }
}
