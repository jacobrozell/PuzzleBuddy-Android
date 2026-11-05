package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** Ports iOS `CollectionStatsTests.testReplayLogCountsWhilePuzzleIsInProgress` (review fix #44). */
class CollectionStatsReplayTest {
    private val zone = ZoneOffset.UTC
    private val reference = Instant.parse("2026-06-15T12:00:00Z")

    @Test
    fun replayLogCountsWhilePuzzleIsInProgress() {
        val again = Puzzle(
            name = "Again",
            pieces = 500,
            status = PuzzleStatus.IN_PROGRESS,
            completionDate = reference,
            completions = listOf(PuzzleCompletion(completionNumber = 1, completedAt = reference)),
            timesCompleted = 1,
        )

        val stats = CollectionStats.compute(listOf(again), zone, reference)

        assertEquals(0, stats.completedCount)
        assertEquals(1, stats.completionsThisMonth)
        assertEquals(1, stats.completionsThisYear)
        assertEquals(1, stats.completionsByMonthThisYear[5])
        assertEquals(1, CollectionStats.monthlyCompletionCounts(listOf(again), 2026, zone)[5])
    }
}
