package com.jacobrozell.puzzlebuddy.domain.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzlePhotoPickTest {
    @Test
    fun staysQuietWhenEveryPhotoFits() {
        assertNull(PuzzlePhotoPick.overflowMessage(slots = 2, selected = 2, unreadable = 0))
    }

    @Test
    fun namesHowManyWereLeftOut() {
        assertEquals(
            "Only 2 more photos fit on this puzzle. 3 were not added.",
            PuzzlePhotoPick.overflowMessage(slots = 2, selected = 5, unreadable = 0),
        )
        assertEquals(
            "Only 1 more photo fits on this puzzle. 1 was not added.",
            PuzzlePhotoPick.overflowMessage(slots = 1, selected = 2, unreadable = 0),
        )
    }

    @Test
    fun namesAFullPuzzle() {
        assertEquals(
            "This puzzle already has 5 photos. 2 were not added.",
            PuzzlePhotoPick.overflowMessage(slots = 0, selected = 2, unreadable = 0),
        )
    }

    @Test
    fun namesPhotosThatCouldNotBeRead() {
        assertEquals(
            "1 photo could not be read and was not added.",
            PuzzlePhotoPick.overflowMessage(slots = 3, selected = 1, unreadable = 1),
        )
        assertEquals(
            "Only 2 more photos fit on this puzzle. 1 was not added. 1 photo could not be read and was not added.",
            PuzzlePhotoPick.overflowMessage(slots = 2, selected = 3, unreadable = 1),
        )
    }
}
