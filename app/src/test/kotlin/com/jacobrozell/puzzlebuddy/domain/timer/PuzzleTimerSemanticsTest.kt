package com.jacobrozell.puzzlebuddy.domain.timer

import com.jacobrozell.puzzlebuddy.data.local.toDomain
import com.jacobrozell.puzzlebuddy.data.local.toEntity
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Ports iOS `PuzzleTimerSemanticsTests` (tag 1.1.0). */
class PuzzleTimerSemanticsTest {
    private val anchor = Instant.ofEpochSecond(1_800_000_000)

    private fun puzzle(name: String) = Puzzle(name = name, pieces = 500, completionDate = anchor)

    @Test
    fun elapsedWhilePausedIsAccumulated() {
        assertEquals(125, PuzzleTimerSemantics.elapsedSeconds(125, null, anchor))
        assertFalse(PuzzleTimerSemantics.isRunning(null, anchor))
    }

    @Test
    fun elapsedWhileRunningAddsOpenSegment() {
        val now = anchor.plusSeconds(90)
        assertEquals(120, PuzzleTimerSemantics.elapsedSeconds(30, anchor, now))
        assertTrue(PuzzleTimerSemantics.isRunning(anchor, now))
    }

    @Test
    fun pauseResumeAccumulates() {
        var timed = PuzzleTimerSemantics.paused(puzzle("Timer").copy(timerRunningSince = anchor), anchor.plusSeconds(600))
        assertEquals(600, timed.timerAccumulatedSeconds)
        assertNull(timed.timerRunningSince)

        val resumed = anchor.plusSeconds(1_000)
        timed = PuzzleTimerSemantics.paused(timed.copy(timerRunningSince = resumed), resumed.plusSeconds(60))
        assertEquals(660, timed.timerAccumulatedSeconds)
    }

    @Test
    fun backwardClockDropsOpenSegmentAndNeverGoesNegative() {
        val earlier = anchor.minusSeconds(300)
        assertTrue(PuzzleTimerSemantics.isClockSkewed(anchor, earlier))
        assertFalse(PuzzleTimerSemantics.isRunning(anchor, earlier))
        assertEquals(45, PuzzleTimerSemantics.elapsedSeconds(45, anchor, earlier))
        assertEquals(0, PuzzleTimerSemantics.elapsedSeconds(-10, null, earlier))

        val skewed = PuzzleTimerSemantics.paused(
            puzzle("Skew").copy(timerAccumulatedSeconds = 45, timerRunningSince = anchor),
            earlier,
        )
        assertEquals(45, skewed.timerAccumulatedSeconds)
        assertNull(skewed.timerRunningSince)
    }

    @Test
    fun roundingToMinutes() {
        assertNull(PuzzleTimerSemantics.roundedMinutes(0))
        assertEquals(1, PuzzleTimerSemantics.roundedMinutes(1))
        assertEquals(1, PuzzleTimerSemantics.roundedMinutes(29))
        assertEquals(1, PuzzleTimerSemantics.roundedMinutes(89))
        assertEquals(2, PuzzleTimerSemantics.roundedMinutes(90))
        assertEquals(134, PuzzleTimerSemantics.roundedMinutes(8_040))

        val time = PuzzleTimerSemantics.puzzleTime(8_040)
        assertEquals(2, time?.hours)
        assertEquals(14, time?.minutes)
        assertEquals("2 hr 14 min", time?.displayLabel())
        assertNull(PuzzleTimerSemantics.puzzleTime(0))
    }

    @Test
    fun completionPrefillOnlyWhenLoggedTimeEmpty() {
        assertEquals(1, PuzzleTimerSemantics.completionPrefill(3_600, null)?.hours)
        assertEquals(1, PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime())?.hours)
        assertNull(PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime(hours = 3)))
        assertNull(PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime(minutes = 5)))
        assertNull(PuzzleTimerSemantics.completionPrefill(0, null))
    }

    @Test
    fun formSuggestionHiddenWhenMatching() {
        assertNull(PuzzleTimerSemantics.formSuggestion(8_040, PuzzleTime(2, 14)))
        assertEquals(14, PuzzleTimerSemantics.formSuggestion(8_040, PuzzleTime(2, 0))?.minutes)
        assertNull(PuzzleTimerSemantics.formSuggestion(0, null))
    }

    @Test
    fun otherRunningTimerMustPause() {
        val first = puzzle("A").copy(timerRunningSince = anchor)
        val second = puzzle("B")
        val idle = puzzle("C")
        assertEquals(listOf(first.id), PuzzleTimerSemantics.idsToPause(second.id, listOf(first, second, idle)))
        assertEquals(emptyList<String>(), PuzzleTimerSemantics.idsToPause(first.id, listOf(first, second, idle)))
    }

    @Test
    fun labelsAndBuckets() {
        assertEquals("0:00:00", PuzzleTimerSemantics.clockLabel(0))
        assertEquals("2:14:05", PuzzleTimerSemantics.clockLabel(8_045))
        assertEquals("25:01:01", PuzzleTimerSemantics.clockLabel(90_061))
        assertEquals("0 seconds", PuzzleTimerSemantics.spokenLabel(0))
        assertEquals("2 hours, 14 minutes, 5 seconds", PuzzleTimerSemantics.spokenLabel(8_045))

        assertEquals("under_15m", PuzzleTimerSemantics.durationBucket(0))
        assertEquals("15m_1h", PuzzleTimerSemantics.durationBucket(900))
        assertEquals("1h_3h", PuzzleTimerSemantics.durationBucket(3_600))
        assertEquals("over_3h", PuzzleTimerSemantics.durationBucket(10_800))
    }

    @Test
    fun copyAndEntityRoundTripKeepTimerFields() {
        val timed = puzzle("Round trip").copy(
            status = PuzzleStatus.IN_PROGRESS,
            timerAccumulatedSeconds = 321,
            timerRunningSince = anchor,
        )
        val restored = timed.toEntity().toDomain()
        assertEquals(321, restored.timerAccumulatedSeconds)
        assertEquals(anchor, restored.timerRunningSince)

        val cleared = timed.copy(timerAccumulatedSeconds = 10, timerRunningSince = null).toEntity()
        assertEquals(10, cleared.timerAccumulatedSeconds)
        assertNull(cleared.timerRunningSinceEpochMillis)
    }
}
