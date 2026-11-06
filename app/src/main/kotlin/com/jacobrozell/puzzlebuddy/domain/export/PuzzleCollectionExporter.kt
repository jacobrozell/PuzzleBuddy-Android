package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

enum class PuzzleCollectionExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
}

class PuzzleCollectionExportError(message: String) : Exception(message)

object PuzzleCollectionBackupFormat {
    /** Increment when the export shape changes in a breaking way. Additive fields stay on this version. */
    const val CURRENT_VERSION = 1
}

object PuzzleCollectionExporter {
    private val json = Json { prettyPrint = true }
    private val instantFormatter = DateTimeFormatter.ISO_INSTANT

    fun jsonData(
        puzzles: List<Puzzle>,
        appVersion: String,
        friends: List<Friend> = emptyList(),
        photoBytes: Map<String, ByteArray> = emptyMap(),
    ): ByteArray {
        if (puzzles.isEmpty()) throw PuzzleCollectionExportError("Add at least one puzzle before exporting.")
        val payload = buildJsonObject {
            put("backupFormatVersion", PuzzleCollectionBackupFormat.CURRENT_VERSION)
            put("exportedAt", instantFormatter.format(Instant.now()))
            put("appVersion", appVersion)
            put("puzzleCount", puzzles.size)
            put("friends", buildJsonArray { friends.forEach { add(friendObject(it)) } })
            put("puzzles", buildJsonArray { puzzles.forEach { add(puzzleObject(it, photoBytes)) } })
        }
        return json.encodeToString(JsonObject.serializer(), payload).toByteArray(Charsets.UTF_8)
    }

    fun csvData(puzzles: List<Puzzle>): ByteArray {
        if (puzzles.isEmpty()) throw PuzzleCollectionExportError("Add at least one puzzle before exporting.")
        return IPDbCSVFormat.csvData(puzzles)
    }

    fun fileName(format: PuzzleCollectionExportFormat): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(Instant.now().atZone(ZoneId.systemDefault()))
        val prefix = if (format == PuzzleCollectionExportFormat.CSV) {
            "puzzle-buddy-ipdb-export"
        } else {
            "puzzle-buddy-export"
        }
        return "$prefix-$stamp.${format.extension}"
    }

    private fun friendObject(friend: Friend): JsonObject = buildJsonObject {
        put("id", friend.id)
        put("displayName", friend.displayName)
        put("createdAt", instantFormatter.format(friend.createdAt))
        put("updatedAt", instantFormatter.format(friend.updatedAt))
        putOptional("remoteId", friend.remoteId)
        put("isDemo", friend.isDemo)
    }

    private fun puzzleObject(puzzle: Puzzle, photoBytes: Map<String, ByteArray>): JsonObject {
        val photos = PuzzlePhotoSemantics.photosInOrder(puzzle.photos).mapNotNull { photo ->
            val bytes = photoBytes[photo.id] ?: return@mapNotNull null
            photo to bytes
        }
        return buildJsonObject {
            put("id", puzzle.id)
            put("name", puzzle.name)
            putOptional("pieces", puzzle.pieces)
            put("status", puzzle.status.raw)
            put("rating", puzzle.rating.value)
            put("difficulty", puzzle.difficulty.raw)
            putOptional("estimatedTimeHours", puzzle.estimatedTimeSpent?.hours)
            putOptional("estimatedTimeMinutes", puzzle.estimatedTimeSpent?.minutes)
            put("completionDate", instantFormatter.format(puzzle.completionDate))
            putOptional("startDate", puzzle.startDate?.let(instantFormatter::format))
            putOptional("notes", puzzle.notes)
            putOptional("source", puzzle.source)
            putOptional("purchaseLocation", puzzle.purchaseLocation)
            putOptional("purchasePrice", puzzle.purchasePrice)
            putOptional("purchaseCurrencyCode", puzzle.purchaseCurrencyCode)
            putOptional("releaseYear", puzzle.releaseYear)
            put("puzzleType", puzzle.puzzleType.raw)
            put("material", puzzle.material.raw)
            put("disposition", puzzle.disposition.raw)
            put("puzzleShape", puzzle.puzzleShape.raw)
            put("cutType", puzzle.cutType.raw)
            putOptional("dimensionsText", puzzle.dimensionsText)
            put("progressPercent", puzzle.progressPercent)
            put("timesCompleted", puzzle.timesCompleted)
            putOptional("barcode", puzzle.barcode)
            put("tags", buildJsonArray { puzzle.tags.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("hasMissingPieces", puzzle.hasMissingPieces)
            put("isOnLoan", puzzle.isOnLoan)
            putOptional("loanedToFriendId", puzzle.loanedToFriendId)
            putOptional("loanedAt", puzzle.loanedAt?.let(instantFormatter::format))
            putOptional("dueBackDate", puzzle.dueBackDate?.let(instantFormatter::format))
            putOptional("lastLoanNudgeAt", puzzle.lastLoanNudgeAt?.let(instantFormatter::format))
            put("timerAccumulatedSeconds", puzzle.timerAccumulatedSeconds)
            putOptional("timerRunningSince", puzzle.timerRunningSince?.let(instantFormatter::format))
            put("hasImage", photos.isNotEmpty())
            put("photoCount", photos.size)
            put("photos", buildJsonArray {
                photos.forEach { (photo, bytes) ->
                    add(buildJsonObject {
                        put("id", photo.id)
                        put("sortOrder", photo.sortOrder)
                        put("imageDataBase64", Base64.getEncoder().encodeToString(bytes))
                        putOptional("createdAt", instantFormatter.format(photo.createdAt))
                    })
                }
            })
            put("completions", buildJsonArray {
                puzzle.completions.sortedBy { it.completionNumber }.forEach { completion ->
                    add(buildJsonObject {
                        put("completionNumber", completion.completionNumber)
                        putOptional("startedAt", completion.startedAt?.let(instantFormatter::format))
                        put("completedAt", instantFormatter.format(completion.completedAt))
                        putOptional("timeSpentHours", completion.timeSpentHours)
                        putOptional("timeSpentMinutes", completion.timeSpentMinutes)
                        putOptional("rating", completion.rating)
                    })
                }
            })
            put("isDemo", puzzle.isDemo)
        }
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putOptional(key: String, value: String?) {
    if (value != null) put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putOptional(key: String, value: Int?) {
    if (value != null) put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putOptional(key: String, value: Double?) {
    if (value != null) put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putOptional(key: String, value: Long?) {
    if (value != null) put(key, value)
}
