package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionSemantics
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.loan.FriendSemantics
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.MAX_PHOTOS
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Base64
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.round

sealed class PuzzleCollectionJSONImportError(message: String) : Exception(message) {
    class EmptyFile : PuzzleCollectionJSONImportError(
        "The backup file looks empty. Choose a Puzzle Buddy JSON export and try again.",
    )

    class InvalidFormat : PuzzleCollectionJSONImportError(
        "Could not read this backup. Share a fresh JSON backup from Puzzle Buddy and try again.",
    )

    class NoRestorablePuzzles : PuzzleCollectionJSONImportError(
        "No puzzles were found in this backup.",
    )

    class UnsupportedFormatVersion(val version: Int) : PuzzleCollectionJSONImportError(
        "This backup requires a newer version of Puzzle Buddy (format v$version). Update the app and try again.",
    )
}

data class PuzzleBackupParseResult(
    val puzzles: List<Puzzle>,
    val friends: List<Friend>,
    val photoBytes: Map<String, ByteArray>,
    val skippedInvalid: Int,
    val totalRecords: Int,
)

object PuzzleCollectionJSONImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(data: ByteArray): PuzzleBackupParseResult {
        if (data.isEmpty()) throw PuzzleCollectionJSONImportError.EmptyFile()
        val payload = try {
            json.decodeFromString<BackupPayload>(data.decodeToString())
        } catch (_: Exception) {
            throw PuzzleCollectionJSONImportError.InvalidFormat()
        }
        val version = payload.backupFormatVersion ?: PuzzleCollectionBackupFormat.CURRENT_VERSION
        if (version > PuzzleCollectionBackupFormat.CURRENT_VERSION) {
            throw PuzzleCollectionJSONImportError.UnsupportedFormatVersion(version)
        }
        val friends = payload.friends.orEmpty().mapNotNull(::friend)
        val photoBytes = linkedMapOf<String, ByteArray>()
        var skipped = 0
        val puzzles = payload.puzzles.mapNotNull { record ->
            val restored = puzzle(record, photoBytes) ?: run {
                skipped += 1
                return@mapNotNull null
            }
            restored
        }
        if (puzzles.isEmpty()) throw PuzzleCollectionJSONImportError.NoRestorablePuzzles()
        return PuzzleBackupParseResult(puzzles, friends, photoBytes, skipped, payload.puzzles.size)
    }

    fun puzzles(data: ByteArray): List<Puzzle> = parse(data).puzzles

    private fun friend(record: FriendPayload): Friend? {
        val id = record.id?.let(::uuidOrNull) ?: return null
        val name = FriendSemantics.normalizedDisplayName(record.displayName) ?: return null
        val created = parseInstant(record.createdAt) ?: return null
        val updated = parseInstant(record.updatedAt) ?: created
        return Friend(
            id = id,
            displayName = name,
            createdAt = created,
            updatedAt = updated,
            remoteId = record.remoteId,
            isDemo = record.isDemo ?: false,
        )
    }

    private fun puzzle(record: PuzzlePayload, photoBytes: MutableMap<String, ByteArray>): Puzzle? {
        val name = record.name?.trim().orEmpty()
        if (name.isEmpty()) return null
        val status = PuzzleStatus.fromRaw(record.status ?: PuzzleStatus.TODO.raw)
        val photos = photos(record.photos, photoBytes)
        val completions = completions(record.completions)
        var puzzle = Puzzle(
            id = record.id?.let(::uuidOrNull) ?: UUID.randomUUID().toString(),
            name = name.take(200),
            pieces = record.pieces?.takeIf { it > 0 },
            rating = rating(record.rating),
            difficulty = PuzzleDifficulty.fromRaw(record.difficulty ?: PuzzleDifficulty.NONE.raw),
            estimatedTimeSpent = time(record.estimatedTimeHours, record.estimatedTimeMinutes),
            completionDate = parseInstant(record.completionDate) ?: Instant.now(),
            startDate = parseInstant(record.startDate),
            status = status,
            hasMissingPieces = record.hasMissingPieces ?: false,
            notes = optionalText(record.notes, 2_000),
            source = optionalText(record.source, 200),
            purchaseLocation = optionalText(record.purchaseLocation, 200),
            releaseYear = record.releaseYear?.takeIf { it in 1900..2100 },
            puzzleType = type(record.puzzleType),
            material = material(record.material),
            disposition = disposition(record.disposition),
            progressPercent = record.progressPercent?.let(PuzzleProgressSemantics::clamped)
                ?: PuzzleProgressSemantics.progressFor(status, 0),
            purchasePrice = record.purchasePrice?.takeIf { it >= 0 }?.coerceAtMost(999_999.99),
            purchaseCurrencyCode = optionalText(record.purchaseCurrencyCode, 8),
            puzzleShape = PuzzleShape.fromRaw(record.puzzleShape ?: PuzzleShape.NONE.raw),
            cutType = PuzzleCutType.fromRaw(record.cutType ?: PuzzleCutType.NONE.raw),
            dimensionsText = optionalText(record.dimensionsText, 80),
            timesCompleted = maxOf(record.timesCompleted ?: 0, 0),
            photos = photos,
            completions = completions,
            isDemo = record.isDemo ?: false,
            barcode = BarcodeNormalizer.normalize(record.barcode),
            tags = PuzzleTagSemantics.sanitizedTags(record.tags.orEmpty()),
            isOnLoan = record.isOnLoan ?: false,
            loanedToFriendId = record.loanedToFriendId?.let(::uuidOrNull),
            loanedAt = parseInstant(record.loanedAt),
            dueBackDate = parseInstant(record.dueBackDate),
            lastLoanNudgeAt = parseInstant(record.lastLoanNudgeAt),
            timerAccumulatedSeconds = (record.timerAccumulatedSeconds ?: 0).toLong().coerceAtLeast(0),
            timerRunningSince = parseInstant(record.timerRunningSince),
        )
        if (puzzle.progressPercent == 0) {
            puzzle = puzzle.copy(progressPercent = PuzzleProgressSemantics.progressFor(puzzle.status, 0))
        }
        if (puzzle.status != PuzzleStatus.IN_PROGRESS) {
            puzzle = PuzzleTimerSemantics.paused(puzzle, Instant.now())
        }
        if (puzzle.timesCompleted < puzzle.completions.size) {
            puzzle = puzzle.copy(timesCompleted = puzzle.completions.size)
        }
        if (puzzle.status == PuzzleStatus.COMPLETED && puzzle.completions.isEmpty()) {
            val count = maxOf(puzzle.timesCompleted, 1)
            puzzle = puzzle.copy(
                timesCompleted = count,
                completions = (1..count).map { PuzzleCompletionSemantics.makeCompletion(puzzle, it) },
            )
        }
        return puzzle.preparedForPersistence(defaultCurrency())
    }

    private fun photos(records: List<PhotoPayload>?, into: MutableMap<String, ByteArray>): List<PuzzlePhoto> {
        if (records == null) return emptyList()
        val decoded = mutableListOf<PuzzlePhoto>()
        for (record in records) {
            val encoded = record.imageDataBase64 ?: continue
            val bytes = try {
                Base64.getDecoder().decode(encoded)
            } catch (_: IllegalArgumentException) {
                continue
            }
            if (bytes.isEmpty()) continue
            val id = record.id?.let(::uuidOrNull) ?: UUID.randomUUID().toString()
            into[id] = bytes
            decoded += PuzzlePhoto(
                id = id,
                sortOrder = record.sortOrder ?: decoded.size,
                createdAt = parseInstant(record.createdAt) ?: Instant.now(),
            )
        }
        return PuzzlePhotoSemantics.sortedAndNormalized(decoded).take(MAX_PHOTOS)
    }

    private fun completions(records: List<CompletionPayload>?): List<PuzzleCompletion> {
        if (records == null) return emptyList()
        return records.mapNotNull { record ->
            val number = record.completionNumber ?: return@mapNotNull null
            if (number <= 0) return@mapNotNull null
            PuzzleCompletion(
                id = record.id?.let(::uuidOrNull) ?: UUID.randomUUID().toString(),
                completionNumber = number,
                startedAt = parseInstant(record.startedAt),
                completedAt = parseInstant(record.completedAt) ?: Instant.now(),
                timeSpentHours = record.timeSpentHours,
                timeSpentMinutes = record.timeSpentMinutes,
                rating = record.rating,
            )
        }.sortedBy { it.completionNumber }
    }

    private fun rating(raw: Double?): PuzzleRating {
        if (raw == null) return PuzzleRating.NONE
        val snapped = round(raw * 2) / 2.0
        return PuzzleRating.entries.minBy { abs(it.value - snapped) }
    }

    private fun type(raw: String?): PuzzleType {
        if (raw.isNullOrBlank()) return PuzzleType.NONE
        return PuzzleType.entries.firstOrNull { it != PuzzleType.NONE && it.raw.equals(raw, ignoreCase = true) }
            ?: PuzzleType.OTHER
    }

    private fun material(raw: String?): PuzzleMaterial {
        if (raw.isNullOrBlank()) return PuzzleMaterial.NONE
        return PuzzleMaterial.entries.firstOrNull { it != PuzzleMaterial.NONE && it.raw.equals(raw, ignoreCase = true) }
            ?: PuzzleMaterial.OTHER
    }

    private fun disposition(raw: String?): PuzzleDisposition {
        if (raw.isNullOrBlank()) return PuzzleDisposition.NONE
        return PuzzleDisposition.entries.firstOrNull { it != PuzzleDisposition.NONE && it.raw.equals(raw, ignoreCase = true) }
            ?: PuzzleDisposition.NONE
    }

    private fun time(hours: Int?, minutes: Int?): PuzzleTime? {
        if (hours == null && minutes == null) return null
        return PuzzleTime(hours = hours, minutes = minutes)
    }

    private fun optionalText(value: String?, maxLength: Int): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.take(maxLength)
    }

    private fun uuidOrNull(raw: String): String? = try {
        UUID.fromString(raw).toString()
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(raw).toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun defaultCurrency(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("USD")
}

@Serializable
private data class BackupPayload(
    val backupFormatVersion: Int? = null,
    val puzzles: List<PuzzlePayload>,
    val friends: List<FriendPayload>? = null,
)

@Serializable
private data class FriendPayload(
    val id: String? = null,
    val displayName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val remoteId: String? = null,
    val isDemo: Boolean? = null,
)

@Serializable
private data class PuzzlePayload(
    val id: String? = null,
    val name: String? = null,
    val pieces: Int? = null,
    val status: String? = null,
    val rating: Double? = null,
    val difficulty: String? = null,
    val estimatedTimeHours: Int? = null,
    val estimatedTimeMinutes: Int? = null,
    val completionDate: String? = null,
    val startDate: String? = null,
    val notes: String? = null,
    val source: String? = null,
    val purchaseLocation: String? = null,
    val purchasePrice: Double? = null,
    val purchaseCurrencyCode: String? = null,
    val releaseYear: Int? = null,
    val puzzleType: String? = null,
    val material: String? = null,
    val disposition: String? = null,
    val puzzleShape: String? = null,
    val cutType: String? = null,
    val dimensionsText: String? = null,
    val progressPercent: Int? = null,
    val timesCompleted: Int? = null,
    val barcode: String? = null,
    val tags: List<String>? = null,
    val hasMissingPieces: Boolean? = null,
    val isOnLoan: Boolean? = null,
    val loanedToFriendId: String? = null,
    val loanedAt: String? = null,
    val dueBackDate: String? = null,
    val lastLoanNudgeAt: String? = null,
    val timerAccumulatedSeconds: Int? = null,
    val timerRunningSince: String? = null,
    val photos: List<PhotoPayload>? = null,
    val completions: List<CompletionPayload>? = null,
    val isDemo: Boolean? = null,
)

@Serializable
private data class PhotoPayload(
    val id: String? = null,
    val sortOrder: Int? = null,
    val imageDataBase64: String? = null,
    val createdAt: String? = null,
)

@Serializable
private data class CompletionPayload(
    val id: String? = null,
    val completionNumber: Int? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val timeSpentHours: Int? = null,
    val timeSpentMinutes: Int? = null,
    val rating: Double? = null,
)
