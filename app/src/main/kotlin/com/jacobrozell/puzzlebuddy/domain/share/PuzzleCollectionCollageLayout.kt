package com.jacobrozell.puzzlebuddy.domain.share

object PuzzleCollectionCollageLayout {
    const val MAX_CELLS = 12

    data class Grid(val columns: Int, val rows: Int, val displayed: Int)

    fun gridDimensions(count: Int): Grid {
        val displayed = count.coerceIn(1, MAX_CELLS)
        return when (displayed) {
            1 -> Grid(1, 1, 1)
            2 -> Grid(2, 1, 2)
            3, 4 -> Grid(2, 2, displayed)
            in 5..6 -> Grid(3, 2, displayed)
            in 7..9 -> Grid(3, 3, displayed)
            else -> Grid(4, 3, displayed)
        }
    }
}
