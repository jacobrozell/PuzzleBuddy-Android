package com.jacobrozell.puzzlebuddy.domain.model

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics
import java.time.Instant
import java.util.UUID

enum class PuzzleRating(val value: Double) {
    NONE(0.0),
    ONE(1.0),
    ONE_HALF(1.5),
    TWO(2.0),
    TWO_HALF(2.5),
    THREE(3.0),
    THREE_HALF(3.5),
    FOUR(4.0),
    FOUR_HALF(4.5),
    FIVE(5.0),
    ;

    companion object {
        fun fromValue(value: Double): PuzzleRating =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}

enum class PuzzleDifficulty(val raw: String) {
    NONE("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    ;

    val intValue: Int get() = raw.toIntOrNull() ?: 0

    companion object {
        fun fromRaw(raw: String): PuzzleDifficulty =
            entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleStatus(val raw: String) {
    TODO("To-Do"),
    IN_PROGRESS("In-Progress"),
    COMPLETED("Completed"),
    ;

    companion object {
        fun fromRaw(raw: String): PuzzleStatus =
            entries.firstOrNull { it.raw == raw } ?: TODO
    }
}

data class PuzzleTime(
    val hours: Int? = null,
    val minutes: Int? = null,
) {
    fun displayLabel(): String? {
        val hourValue = maxOf(hours ?: 0, 0)
        val minuteValue = maxOf(minutes ?: 0, 0)
        if (hourValue == 0 && minuteValue == 0) return null
        val parts = buildList {
            if (hourValue > 0) add(if (hourValue == 1) "1 hr" else "$hourValue hr")
            if (minuteValue > 0) add(if (minuteValue == 1) "1 min" else "$minuteValue min")
        }
        return parts.joinToString(" ")
    }

    fun normalized(): PuzzleTime {
        var hourValue = maxOf(hours ?: 0, 0)
        var minuteValue = maxOf(minutes ?: 0, 0)
        if (minuteValue >= 60) {
            hourValue += minuteValue / 60
            minuteValue %= 60
        }
        return copy(hours = hourValue, minutes = minuteValue)
    }

    fun totalMinutes(): Int? {
        val h = hours ?: return null
        val m = minutes ?: return null
        return maxOf((h * 60) + m, 0)
    }
}

data class Puzzle(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val pieces: Int? = null,
    val rating: PuzzleRating = PuzzleRating.NONE,
    val difficulty: PuzzleDifficulty = PuzzleDifficulty.NONE,
    val estimatedTimeSpent: PuzzleTime? = null,
    val completionDate: Instant = Instant.now(),
    val status: PuzzleStatus = PuzzleStatus.TODO,
    val hasMissingPieces: Boolean = false,
    val notes: String? = null,
    val source: String? = null,
    val progressPercent: Int = 0,
    val isDemo: Boolean = false,
    val barcode: String? = null,
    val tags: List<String> = emptyList(),
    val hasImage: Boolean = false,
) {
    fun sanitized(): Puzzle = copy(
        name = name.trim(),
        barcode = BarcodeNormalizer.normalize(barcode),
        tags = PuzzleTagSemantics.sanitizedTags(tags),
        progressPercent = PuzzleProgressSemantics.clamped(progressPercent),
    )
}
