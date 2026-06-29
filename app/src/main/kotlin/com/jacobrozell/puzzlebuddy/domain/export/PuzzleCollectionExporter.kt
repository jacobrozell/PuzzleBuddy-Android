package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class PuzzleCollectionExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
}

class PuzzleCollectionExportError(message: String) : Exception(message)

@Serializable
data class PuzzleExportRecord(
    val id: String,
    val name: String,
    val pieces: Int? = null,
    val status: String,
    val rating: Double,
    val difficulty: String,
    val estimatedTimeHours: Int? = null,
    val estimatedTimeMinutes: Int? = null,
    val completionDate: String,
    val notes: String? = null,
    val source: String? = null,
    val progressPercent: Int,
    val barcode: String? = null,
    val tags: List<String> = emptyList(),
    val hasMissingPieces: Boolean = false,
    val hasImage: Boolean = false,
)

@Serializable
private data class ExportPayload(
    val exportedAt: String,
    val appVersion: String,
    val puzzleCount: Int,
    val puzzles: List<PuzzleExportRecord>,
)

object PuzzleCollectionExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun exportRecords(puzzles: List<Puzzle>): List<PuzzleExportRecord> =
        puzzles.map(::exportRecord)

    fun jsonData(puzzles: List<Puzzle>): ByteArray {
        if (puzzles.isEmpty()) throw PuzzleCollectionExportError("Add at least one puzzle before exporting.")
        val payload = ExportPayload(
            exportedAt = isoFormatter.format(Instant.now().atZone(ZoneId.systemDefault())),
            appVersion = ProductSurface.LEAN_VERSION,
            puzzleCount = puzzles.size,
            puzzles = exportRecords(puzzles),
        )
        return json.encodeToString(payload).toByteArray(Charsets.UTF_8)
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

    private fun exportRecord(puzzle: Puzzle): PuzzleExportRecord = PuzzleExportRecord(
        id = puzzle.id,
        name = puzzle.name,
        pieces = puzzle.pieces,
        status = puzzle.status.raw,
        rating = puzzle.rating.value,
        difficulty = puzzle.difficulty.raw,
        estimatedTimeHours = puzzle.estimatedTimeSpent?.hours,
        estimatedTimeMinutes = puzzle.estimatedTimeSpent?.minutes,
        completionDate = isoFormatter.format(puzzle.completionDate.atZone(ZoneId.systemDefault())),
        notes = puzzle.notes,
        source = puzzle.source,
        progressPercent = puzzle.progressPercent,
        barcode = puzzle.barcode,
        tags = puzzle.tags,
        hasMissingPieces = puzzle.hasMissingPieces,
        hasImage = puzzle.hasImage,
    )
}
