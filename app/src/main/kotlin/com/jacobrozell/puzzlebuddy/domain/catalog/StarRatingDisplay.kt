package com.jacobrozell.puzzlebuddy.domain.catalog

enum class StarFill {
    Full,
    Half,
    Empty,
}

/** Mirrors iOS `StarGlyphs.display(for:at:)`. */
fun starFillForRating(ratingValue: Double, position: Int): StarFill {
    val threshold = position.toDouble()
    return when {
        ratingValue >= threshold -> StarFill.Full
        ratingValue >= threshold - 0.5 -> StarFill.Half
        else -> StarFill.Empty
    }
}
