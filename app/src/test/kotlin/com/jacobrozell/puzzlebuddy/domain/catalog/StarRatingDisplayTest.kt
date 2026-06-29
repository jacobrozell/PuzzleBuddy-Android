package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import org.junit.Assert.assertEquals
import org.junit.Test

class StarRatingDisplayTest {
    @Test
    fun fourPointZero_showsFourFullStarsAndOneEmpty() {
        val fills = (1..5).map { starFillForRating(PuzzleRating.FOUR.value, it) }
        assertEquals(
            listOf(StarFill.Full, StarFill.Full, StarFill.Full, StarFill.Full, StarFill.Empty),
            fills,
        )
    }

    @Test
    fun fourPointFive_showsFourFullStarsAndOneHalf() {
        val fills = (1..5).map { starFillForRating(PuzzleRating.FOUR_HALF.value, it) }
        assertEquals(
            listOf(StarFill.Full, StarFill.Full, StarFill.Full, StarFill.Full, StarFill.Half),
            fills,
        )
    }

    @Test
    fun fivePointZero_showsFiveFullStars() {
        val fills = (1..5).map { starFillForRating(PuzzleRating.FIVE.value, it) }
        assertEquals(List(5) { StarFill.Full }, fills)
    }
}
