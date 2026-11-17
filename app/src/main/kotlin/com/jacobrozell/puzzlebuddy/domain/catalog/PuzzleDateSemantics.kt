package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object PuzzleDateSemantics {
    fun detailDateLabel(status: PuzzleStatus): String = when (status) {
        PuzzleStatus.WISHLIST -> "Added to wishlist"
        PuzzleStatus.TODO -> "Added"
        PuzzleStatus.IN_PROGRESS -> "Started"
        PuzzleStatus.COMPLETED -> "Completed"
        PuzzleStatus.ABANDONED -> "Abandoned"
    }

    fun formDateSectionTitle(status: PuzzleStatus, puzzleName: String): String {
        val name = puzzleName.ifEmpty { "the puzzle" }
        return when (status) {
            PuzzleStatus.WISHLIST -> "Added to wishlist on"
            PuzzleStatus.TODO -> "Target date (optional)"
            PuzzleStatus.IN_PROGRESS -> "Started on"
            PuzzleStatus.COMPLETED -> "When did you finish $name?"
            PuzzleStatus.ABANDONED -> "Abandoned on"
        }
    }

    fun listTrailingDate(puzzle: Puzzle): Instant? =
        if (puzzle.status == PuzzleStatus.COMPLETED) puzzle.completionDate else null

    fun statusPillLabel(status: PuzzleStatus): String = when (status) {
        PuzzleStatus.WISHLIST -> "Wishlist"
        PuzzleStatus.TODO -> "To-Do"
        PuzzleStatus.IN_PROGRESS -> "In progress"
        PuzzleStatus.COMPLETED -> "Completed"
        PuzzleStatus.ABANDONED -> "Abandoned"
    }

    fun showsStartDatePicker(status: PuzzleStatus): Boolean =
        status == PuzzleStatus.IN_PROGRESS || status == PuzzleStatus.COMPLETED

    fun resolveStartDate(puzzle: Puzzle): Instant? {
        puzzle.startDate?.let { return it }
        return when (puzzle.status) {
            PuzzleStatus.IN_PROGRESS, PuzzleStatus.COMPLETED -> puzzle.completionDate
            else -> null
        }
    }

    fun progressDaysLabel(
        puzzle: Puzzle,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): String? {
        val start = resolveStartDate(puzzle) ?: return null
        return when (puzzle.status) {
            PuzzleStatus.IN_PROGRESS -> {
                val days = dayCount(start, now, zoneId)
                if (days == 1) "1 day puzzling" else "$days days puzzling"
            }
            PuzzleStatus.COMPLETED -> {
                val days = dayCount(start, puzzle.completionDate, zoneId)
                when (days) {
                    0 -> "Finished same day"
                    1 -> "Finished in 1 day"
                    else -> "Finished in $days days"
                }
            }
            else -> null
        }
    }

    fun dayCount(start: Instant, end: Instant, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val startDay = start.atZone(zoneId).toLocalDate()
        val endDay = end.atZone(zoneId).toLocalDate()
        return maxOf(ChronoUnit.DAYS.between(startDay, endDay).toInt(), 0)
    }

    fun completionDayCounts(
        puzzles: List<Puzzle>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Int> = puzzles.mapNotNull { puzzle ->
        if (puzzle.status != PuzzleStatus.COMPLETED) return@mapNotNull null
        val start = resolveStartDate(puzzle) ?: return@mapNotNull null
        dayCount(start, puzzle.completionDate, zoneId)
    }
}
