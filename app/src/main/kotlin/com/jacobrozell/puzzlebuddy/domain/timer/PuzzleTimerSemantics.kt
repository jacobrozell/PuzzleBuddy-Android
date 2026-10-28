package com.jacobrozell.puzzlebuddy.domain.timer

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import java.time.Instant
import kotlin.math.roundToInt

object PuzzleTimerSemantics {
    enum class PauseReason(val raw: String) {
        PAUSE("pause"),
        RESET("reset"),
        OTHER_STARTED("other_started"),
        STATUS_LEFT("status_left"),
    }

    fun isClockSkewed(runningSince: Instant?, now: Instant): Boolean =
        runningSince != null && now < runningSince

    fun isRunning(runningSince: Instant?, now: Instant): Boolean =
        runningSince != null && !isClockSkewed(runningSince, now)

    fun elapsedSeconds(accumulated: Long, runningSince: Instant?, now: Instant): Long {
        val base = maxOf(accumulated, 0)
        if (runningSince == null || isClockSkewed(runningSince, now)) return base
        return base + java.time.Duration.between(runningSince, now).seconds
    }

    fun elapsedSeconds(puzzle: Puzzle, now: Instant): Long =
        elapsedSeconds(puzzle.timerAccumulatedSeconds, puzzle.timerRunningSince, now)

    fun paused(puzzle: Puzzle, now: Instant): Puzzle = puzzle.copy(
        timerAccumulatedSeconds = elapsedSeconds(puzzle, now),
        timerRunningSince = null,
    )

    fun idsToPause(startingId: String, puzzles: List<Puzzle>): List<String> =
        puzzles.filter { it.id != startingId && it.timerRunningSince != null }.map { it.id }

    fun roundedMinutes(seconds: Long): Int? {
        if (seconds <= 0) return null
        return maxOf(1, (seconds / 60.0).roundToInt())
    }

    fun puzzleTime(seconds: Long): PuzzleTime? {
        val minutes = roundedMinutes(seconds) ?: return null
        return PuzzleTime(hours = minutes / 60, minutes = minutes % 60)
    }

    fun completionPrefill(timerSeconds: Long, logged: PuzzleTime?): PuzzleTime? {
        if (logged?.hours != null || logged?.minutes != null) return null
        return puzzleTime(timerSeconds)
    }

    fun formSuggestion(timerSeconds: Long, logged: PuzzleTime?): PuzzleTime? {
        val suggestion = puzzleTime(timerSeconds) ?: return null
        if (totalMinutes(suggestion) == totalMinutes(logged)) return null
        return suggestion
    }

    fun durationBucket(seconds: Long): String = when {
        seconds < 900 -> "under_15m"
        seconds < 3_600 -> "15m_1h"
        seconds < 10_800 -> "1h_3h"
        else -> "over_3h"
    }

    fun clockLabel(seconds: Long): String {
        val total = maxOf(seconds, 0)
        val hours = total / 3_600
        val minutes = (total % 3_600) / 60
        val secs = total % 60
        return "%d:%02d:%02d".format(hours, minutes, secs)
    }

    fun spokenLabel(seconds: Long): String {
        if (seconds <= 0) return "0 seconds"
        val total = seconds
        val hours = total / 3_600
        val minutes = (total % 3_600) / 60
        val secs = total % 60
        val parts = buildList {
            if (hours > 0) add(if (hours == 1L) "1 hour" else "$hours hours")
            if (minutes > 0) add(if (minutes == 1L) "1 minute" else "$minutes minutes")
            if (secs > 0) add(if (secs == 1L) "1 second" else "$secs seconds")
        }
        return parts.joinToString(", ")
    }

    private fun totalMinutes(time: PuzzleTime?): Int =
        maxOf(time?.hours ?: 0, 0) * 60 + maxOf(time?.minutes ?: 0, 0)
}
