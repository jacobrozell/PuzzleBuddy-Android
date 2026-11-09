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

    @Test
    fun leftOfFirstStarClears_andOtherTapsMatchIos() {
        assertEquals(PuzzleRating.NONE, ratingForStarTap(1, leftSide = true))
        assertEquals(PuzzleRating.ONE, ratingForStarTap(1, leftSide = false))
        assertEquals(PuzzleRating.ONE_HALF, ratingForStarTap(2, leftSide = true))
        assertEquals(PuzzleRating.FOUR_HALF, ratingForStarTap(5, leftSide = true))
        assertEquals(PuzzleRating.FIVE, ratingForStarTap(5, leftSide = false))
    }

    @Test
    fun adjustStepsThroughHalvesAndStopsAtTheEnds() {
        assertEquals(PuzzleRating.ONE, incrementRating(PuzzleRating.NONE))
        assertEquals(PuzzleRating.ONE_HALF, incrementRating(PuzzleRating.ONE))
        assertEquals(PuzzleRating.FIVE, incrementRating(PuzzleRating.FIVE))
        assertEquals(PuzzleRating.FOUR_HALF, decrementRating(PuzzleRating.FIVE))
        assertEquals(PuzzleRating.NONE, decrementRating(PuzzleRating.NONE))
    }

    @Test
    fun selectionLabelDropsAWholeNumberDecimal() {
        assertEquals("Not rated", ratingSelectionLabel(PuzzleRating.NONE))
        assertEquals("Rated 4", ratingSelectionLabel(PuzzleRating.FOUR))
        assertEquals("Rated 4.5", ratingSelectionLabel(PuzzleRating.FOUR_HALF))
    }
}
