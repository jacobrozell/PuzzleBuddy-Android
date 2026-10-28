package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import kotlin.random.Random

object PuzzleRandomPicker {
    fun eligible(
        puzzles: List<Puzzle>,
        includeInProgress: Boolean,
        pieceCountFilter: PuzzleListPieceCountFilter,
        tagFilter: String?,
    ): List<Puzzle> = puzzles.filter { puzzle ->
        when (puzzle.status) {
            PuzzleStatus.WISHLIST, PuzzleStatus.COMPLETED, PuzzleStatus.ABANDONED -> return@filter false
            PuzzleStatus.TODO -> Unit
            PuzzleStatus.IN_PROGRESS -> if (!includeInProgress) return@filter false
        }
        if (!pieceCountFilter.matches(puzzle)) return@filter false
        if (puzzle.isOnLoan) return@filter false
        if (tagFilter != null && puzzle.tags.none { it.equals(tagFilter, ignoreCase = true) }) return@filter false
        true
    }

    fun pick(pool: List<Puzzle>, excluding: String? = null, random: Random = Random.Default): Puzzle? {
        if (pool.isEmpty()) return null
        if (pool.size > 1 && excluding != null) {
            val fresh = pool.filter { it.id != excluding }
            if (fresh.isNotEmpty()) return fresh[random.nextInt(fresh.size)]
        }
        return pool[random.nextInt(pool.size)]
    }
}
