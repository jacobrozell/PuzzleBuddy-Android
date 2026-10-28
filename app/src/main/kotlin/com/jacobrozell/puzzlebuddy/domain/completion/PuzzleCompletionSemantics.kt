package com.jacobrozell.puzzlebuddy.domain.completion

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating

object PuzzleCompletionSemantics {
    fun makeCompletion(puzzle: Puzzle, number: Int): PuzzleCompletion = PuzzleCompletion(
        completionNumber = number,
        startedAt = puzzle.startDate,
        completedAt = puzzle.completionDate,
        timeSpentHours = puzzle.estimatedTimeSpent?.hours,
        timeSpentMinutes = puzzle.estimatedTimeSpent?.minutes,
        rating = if (puzzle.rating == PuzzleRating.NONE) null else puzzle.rating.value,
    )

    fun sortedNewestFirst(completions: List<PuzzleCompletion>): List<PuzzleCompletion> =
        completions.sortedByDescending { it.completionNumber }

    fun renumbered(completions: List<PuzzleCompletion>): List<PuzzleCompletion> {
        val sorted = completions.sortedWith(
            compareBy<PuzzleCompletion> { it.completedAt }.thenBy { it.completionNumber },
        )
        return sorted.mapIndexed { index, completion ->
            completion.copy(completionNumber = index + 1)
        }
    }
}
