package com.jacobrozell.puzzlebuddy.domain.catalog

import java.text.NumberFormat
import java.util.Locale

object PuzzlePieceCount {
    val commonValues = listOf(100, 300, 500, 750, 1000, 1500, 2000)

    fun formatted(count: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(count)

    fun matchesCommon(pieces: Int?): Boolean = pieces != null && pieces in commonValues
}
