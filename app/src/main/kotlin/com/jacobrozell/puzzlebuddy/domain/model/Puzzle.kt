package com.jacobrozell.puzzlebuddy.domain.model

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.loan.FriendSemantics
import com.jacobrozell.puzzlebuddy.domain.photo.MAX_PHOTOS
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
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

    val accessibilityDescription: String
        get() = if (this == NONE) "No difficulty" else "Difficulty $raw out of 5"

    companion object {
        fun fromRaw(raw: String): PuzzleDifficulty =
            entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleStatus(val raw: String) {
    WISHLIST("Wishlist"),
    TODO("To-Do"),
    IN_PROGRESS("In-Progress"),
    COMPLETED("Completed"),
    ABANDONED("Abandoned"),
    ;

    val accessibilityDescription: String
        get() = when (this) {
            WISHLIST -> "Wishlist, not yet owned"
            TODO -> "To-Do, not started"
            IN_PROGRESS -> "In progress"
            COMPLETED -> "Completed"
            ABANDONED -> "Abandoned, will not finish"
        }

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
    val startDate: Instant? = null,
    val status: PuzzleStatus = PuzzleStatus.TODO,
    val hasMissingPieces: Boolean = false,
    val notes: String? = null,
    val source: String? = null,
    val purchaseLocation: String? = null,
    val releaseYear: Int? = null,
    val puzzleType: PuzzleType = PuzzleType.NONE,
    val material: PuzzleMaterial = PuzzleMaterial.NONE,
    val disposition: PuzzleDisposition = PuzzleDisposition.NONE,
    val progressPercent: Int = 0,
    val purchasePrice: Double? = null,
    val purchaseCurrencyCode: String? = null,
    val puzzleShape: PuzzleShape = PuzzleShape.NONE,
    val cutType: PuzzleCutType = PuzzleCutType.NONE,
    val dimensionsText: String? = null,
    val timesCompleted: Int = 0,
    val photos: List<PuzzlePhoto> = emptyList(),
    val completions: List<PuzzleCompletion> = emptyList(),
    val isDemo: Boolean = false,
    val barcode: String? = null,
    val tags: List<String> = emptyList(),
    val isOnLoan: Boolean = false,
    val loanedToFriendId: String? = null,
    val loanedToDisplayName: String? = null,
    val loanedAt: Instant? = null,
    val dueBackDate: Instant? = null,
    val lastLoanNudgeAt: Instant? = null,
    val timerAccumulatedSeconds: Long = 0,
    val timerRunningSince: Instant? = null,
) {
    val hasImage: Boolean get() = photos.isNotEmpty()

    fun sanitized(): Puzzle = copy(
        name = name.trim(),
        barcode = BarcodeNormalizer.normalize(barcode),
        tags = PuzzleTagSemantics.sanitizedTags(tags),
        progressPercent = PuzzleProgressSemantics.clamped(progressPercent),
    )

    fun preparedForPersistence(defaultCurrency: String): Puzzle {
        val base = sanitized()
        val limited = PuzzlePhotoSemantics.normalizedSortOrders(
            PuzzlePhotoSemantics.sortedAndNormalized(base.photos).take(MAX_PHOTOS),
        )
        val price = base.purchasePrice?.coerceIn(0.0, 999_999.99)
        return base.copy(
            photos = limited,
            purchasePrice = price,
            purchaseCurrencyCode = if (price == null) null else base.purchaseCurrencyCode ?: defaultCurrency,
            dimensionsText = base.dimensionsText?.take(80),
            loanedToDisplayName = FriendSemantics.normalizedDisplayName(base.loanedToDisplayName),
            timerAccumulatedSeconds = base.timerAccumulatedSeconds.coerceAtLeast(0),
        )
    }

    fun withStatusChange(previous: PuzzleStatus, new: PuzzleStatus, now: Instant): Puzzle {
        val progress = PuzzleProgressSemantics.progressFor(new, progressPercent)
        val start = when {
            new == PuzzleStatus.IN_PROGRESS && startDate == null -> now
            new == PuzzleStatus.COMPLETED && startDate == null && previous == PuzzleStatus.IN_PROGRESS -> completionDate
            new == PuzzleStatus.ABANDONED && previous == PuzzleStatus.IN_PROGRESS && startDate == null -> completionDate
            else -> startDate
        }
        return copy(status = new, progressPercent = progress, startDate = start)
    }
}
