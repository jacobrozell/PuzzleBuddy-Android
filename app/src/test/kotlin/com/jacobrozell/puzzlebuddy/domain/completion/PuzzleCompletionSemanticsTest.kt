package com.jacobrozell.puzzlebuddy.domain.completion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Ports iOS `PuzzleCompletionSemanticsTests` rating cases (tag 1.1.0). */
class PuzzleCompletionSemanticsTest {
    private val now = Instant.parse("2026-06-15T12:00:00Z")

    @Test
    fun timeLabelHasNoRatingWhenUnrated() {
        val completion = PuzzleCompletion(
            completionNumber = 1,
            completedAt = now,
            timeSpentHours = 1,
            timeSpentMinutes = 5,
        )
        assertEquals("1 hr 5 min", completion.timeSpentLabel)
        assertNull(completion.ratingLabel)
    }

    @Test
    fun ratingLabelOmitsUnratedAndKeepsHalves() {
        assertNull(PuzzleCompletion(completionNumber = 1, completedAt = now, rating = 0.0).ratingLabel)
        assertEquals("Rated 4", PuzzleCompletion(completionNumber = 1, completedAt = now, rating = 4.0).ratingLabel)
        assertEquals("Rated 4.5", PuzzleCompletion(completionNumber = 1, completedAt = now, rating = 4.5).ratingLabel)
    }
}
