package com.jacobrozell.puzzlebuddy.domain.completion

import java.time.Instant
import java.util.UUID

data class PuzzleCompletion(
    val id: String = UUID.randomUUID().toString(),
    val completionNumber: Int,
    val startedAt: Instant? = null,
    val completedAt: Instant,
    val timeSpentHours: Int? = null,
    val timeSpentMinutes: Int? = null,
    val rating: Double? = null,
) {
    val timeSpentLabel: String?
        get() {
            val hourValue = maxOf(timeSpentHours ?: 0, 0)
            val minuteValue = maxOf(timeSpentMinutes ?: 0, 0)
            if (hourValue == 0 && minuteValue == 0) return null
            return buildList {
                if (hourValue > 0) add(if (hourValue == 1) "1 hr" else "$hourValue hr")
                if (minuteValue > 0) add(if (minuteValue == 1) "1 min" else "$minuteValue min")
            }.joinToString(" ")
        }
}
