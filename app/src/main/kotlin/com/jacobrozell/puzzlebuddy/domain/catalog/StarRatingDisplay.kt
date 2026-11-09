package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import java.util.Locale

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

private val ratingsInOrder = PuzzleRating.entries.sortedBy { it.value }

/**
 * Mirrors iOS `PuzzleRatingSelection.rating(forStar:side:)`.
 * The left half of star 1 clears the rating. There is no 0.5 step.
 */
fun ratingForStarTap(starIndex: Int, leftSide: Boolean): PuzzleRating {
    require(starIndex in 1..5)
    return if (leftSide) {
        if (starIndex == 1) PuzzleRating.NONE else PuzzleRating.fromValue(starIndex - 0.5)
    } else {
        PuzzleRating.fromValue(starIndex.toDouble())
    }
}

fun incrementRating(rating: PuzzleRating): PuzzleRating {
    val index = ratingsInOrder.indexOf(rating)
    if (index < 0 || index == ratingsInOrder.lastIndex) return rating
    return ratingsInOrder[index + 1]
}

fun decrementRating(rating: PuzzleRating): PuzzleRating {
    val index = ratingsInOrder.indexOf(rating)
    if (index <= 0) return rating
    return ratingsInOrder[index - 1]
}

/** Short label beside the stars. Whole numbers drop the decimal. */
fun ratingSelectionLabel(rating: PuzzleRating): String {
    if (rating == PuzzleRating.NONE) return "Not rated"
    val tenths = Math.round(rating.value * 10) / 10.0
    return if (tenths == Math.round(tenths).toDouble()) {
        "Rated ${Math.round(tenths)}"
    } else {
        String.format(Locale.US, "Rated %.1f", tenths)
    }
}
