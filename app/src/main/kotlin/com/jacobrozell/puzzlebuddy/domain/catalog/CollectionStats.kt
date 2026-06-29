package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class CollectionStats(
    val totalCount: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val totalPiecesCompleted: Int,
    val totalMinutesPuzzling: Int,
    val backlogCount: Int,
    val missingPiecesCount: Int,
    val averageRating: Double?,
    val favoritePieceCount: Int?,
    val completionsThisMonth: Int,
    val completionsThisYear: Int,
    val biggestCompletedPieces: Int?,
    val smallestCompletedPieces: Int?,
    val topTags: List<PuzzleTagCount>,
) {
    val formattedTotalHours: String get() = formatHours(totalMinutesPuzzling)

    val formattedAverageRating: String? get() = averageRating?.let { String.format(Locale.US, "%.1f", it) }

    companion object {
        fun compute(
            puzzles: List<Puzzle>,
            zoneId: ZoneId = ZoneId.systemDefault(),
            now: Instant = Instant.now(),
        ): CollectionStats {
            val completed = puzzles.filter { it.status == PuzzleStatus.COMPLETED }
            val todo = puzzles.filter { it.status == PuzzleStatus.TODO }
            val inProgress = puzzles.filter { it.status == PuzzleStatus.IN_PROGRESS }
            val missingPieces = puzzles.filter { it.hasMissingPieces }
            val pieceCounts = completed.mapNotNull { it.pieces }
            val ratedCompleted = completed.filter { it.rating != PuzzleRating.NONE }
            val averageRating = if (ratedCompleted.isEmpty()) {
                null
            } else {
                ratedCompleted.sumOf { it.rating.value } / ratedCompleted.size
            }
            val totalMinutes = completed.sumOf { minutesSpent(it) }
            val nowZoned = ZonedDateTime.ofInstant(now, zoneId)
            return CollectionStats(
                totalCount = puzzles.size,
                completedCount = completed.size,
                inProgressCount = inProgress.size,
                totalPiecesCompleted = pieceCounts.sum(),
                totalMinutesPuzzling = totalMinutes,
                backlogCount = todo.size,
                missingPiecesCount = missingPieces.size,
                averageRating = averageRating,
                favoritePieceCount = favoritePieceCount(pieceCounts),
                completionsThisMonth = completionCount(completed, nowZoned, zoneId) { it.monthValue },
                completionsThisYear = completionCount(completed, nowZoned, zoneId) { it.year },
                biggestCompletedPieces = pieceCounts.maxOrNull(),
                smallestCompletedPieces = pieceCounts.minOrNull(),
                topTags = PuzzleTagIndex.counts(puzzles, limit = 5),
            )
        }

        fun formatHours(minutes: Int): String {
            if (minutes <= 0) return "0 hours"
            val hours = minutes / 60.0
            return when {
                hours >= 10 -> "${hours.toInt()} hours"
                hours >= 1 -> {
                    val rounded = (hours * 10).toInt() / 10.0
                    if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()} hours"
                    else String.format(Locale.US, "%.1f hours", rounded)
                }
                else -> "$minutes minutes"
            }
        }

        fun formatPieceCount(count: Int): String =
            NumberFormat.getNumberInstance(Locale.getDefault()).format(count)

        private fun minutesSpent(puzzle: Puzzle): Int {
            val time = puzzle.estimatedTimeSpent ?: return 0
            val hours = time.hours ?: return 0
            val minutes = time.minutes ?: return 0
            return maxOf((hours * 60) + minutes, 0)
        }

        private fun completionCount(
            completed: List<Puzzle>,
            now: ZonedDateTime,
            zoneId: ZoneId,
            component: (ZonedDateTime) -> Int,
        ): Int = completed.count { puzzle ->
            val date = ZonedDateTime.ofInstant(puzzle.completionDate, zoneId)
            component(date) == component(now)
        }

        fun favoritePieceCount(counts: List<Int>): Int? {
            if (counts.isEmpty()) return null
            val frequencies = counts.groupingBy { it }.eachCount()
            val maxFrequency = frequencies.values.maxOrNull() ?: return null
            val modes = frequencies.filterValues { it == maxFrequency }.keys.sorted()
            if (modes.size == 1) return modes.first()
            return median(counts)
        }

        private fun median(values: List<Int>): Int {
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 0) {
                (sorted[middle - 1] + sorted[middle]) / 2
            } else {
                sorted[middle]
            }
        }
    }
}

data class PuzzleDetailMetrics(
    val timeBucketLabel: String?,
    val hoursPer1000Pieces: Double?,
) {
    val formattedHoursPer1000Pieces: String? get() {
        val value = hoursPer1000Pieces ?: return null
        return when {
            value >= 10 -> "${value.toInt()} hrs per 1,000 pieces"
            value >= 1 -> {
                val rounded = (value * 10).toInt() / 10.0
                if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()} hrs per 1,000 pieces"
                else String.format(Locale.US, "%.1f hrs per 1,000 pieces", rounded)
            }
            else -> {
                val minutesPer1000 = value * 60
                if (minutesPer1000 >= 10) "${minutesPer1000.toInt()} min per 1,000 pieces"
                else String.format(Locale.US, "%.1f min per 1,000 pieces", minutesPer1000)
            }
        }
    }

    companion object {
        fun compute(pieces: Int?, time: PuzzleTime?): PuzzleDetailMetrics {
            val minutes = totalMinutes(time)
            val bucket = minutes?.let { timeBucketLabel(it) }
            val hoursPer1000 = if (minutes != null && pieces != null && pieces > 0) {
                val hours = minutes / 60.0
                hours / (pieces / 1000.0)
            } else {
                null
            }
            return PuzzleDetailMetrics(bucket, hoursPer1000)
        }

        fun timeBucketLabel(minutes: Int): String = when {
            minutes < 240 -> "Quick finish"
            minutes < 720 -> "Weekend puzzle"
            else -> "Marathon project"
        }

        fun totalMinutes(time: PuzzleTime?): Int? {
            val hours = time?.hours ?: return null
            val minutes = time.minutes ?: return null
            return maxOf((hours * 60) + minutes, 0)
        }
    }
}
