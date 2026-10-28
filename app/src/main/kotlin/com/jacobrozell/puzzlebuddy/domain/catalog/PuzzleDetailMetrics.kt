package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import java.util.Locale
import kotlin.math.roundToInt

data class PuzzleDetailMetrics(
    val timeBucketLabel: String?,
    val hoursPer1000Pieces: Double?,
) {
    val formattedHoursPer1000Pieces: String?
        get() {
            val value = hoursPer1000Pieces ?: return null
            if (value >= 10) return "${value.roundToInt()} hrs per 1,000 pieces"
            if (value >= 1) {
                val rounded = (value * 10).roundToInt() / 10.0
                return if (rounded == rounded.toInt().toDouble()) {
                    "${rounded.toInt()} hrs per 1,000 pieces"
                } else {
                    String.format(Locale.US, "%.1f hrs per 1,000 pieces", rounded)
                }
            }
            val minutesPer1000 = value * 60
            return if (minutesPer1000 >= 10) {
                "${minutesPer1000.roundToInt()} min per 1,000 pieces"
            } else {
                String.format(Locale.US, "%.1f min per 1,000 pieces", minutesPer1000)
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
