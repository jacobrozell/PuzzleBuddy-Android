package com.jacobrozell.puzzlebuddy.domain.importing

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object IPDbCSVImporter {
    fun puzzlesFrom(data: ByteArray): List<Puzzle> {
        val text = decodeText(data) ?: throw IPDbCSVImportError.UnreadableEncoding
        if (text.isBlank()) throw IPDbCSVImportError.EmptyFile

        val (_, records) = CSVTable.parseDelimitedRows(text)
        if (records.isEmpty()) throw IPDbCSVImportError.EmptyFile

        val puzzles = records.mapNotNull { puzzleFrom(it) }
        if (puzzles.isEmpty()) throw IPDbCSVImportError.MissingTitleColumn
        return puzzles
    }

    fun puzzleFrom(record: Map<String, String>): Puzzle? {
        val normalized = normalizeKeys(record)
        val name = firstValue(normalized, TITLE_KEYS) ?: return null
        if (name.isBlank()) return null

        val pieces = firstValue(normalized, PIECES_KEYS)?.let(::parsePieces)
        val brand = firstValue(normalized, BRAND_KEYS)
        val barcode = firstValue(normalized, BARCODE_KEYS)?.let {
            BarcodeNormalizer.normalize(it) ?: optionalDigits(it)
        }
        val notes = mergedNotes(normalized)
        val status = parseStatus(firstValue(normalized, STATUS_KEYS))
        val progress = parseProgress(firstValue(normalized, PROGRESS_KEYS))
            ?: PuzzleProgressSemantics.progressFor(status, 0)
        val rating = parseRating(firstValue(normalized, RATING_KEYS))
        val difficulty = parseDifficulty(firstValue(normalized, DIFFICULTY_KEYS))
        val completionDate = parseDate(firstValue(normalized, COMPLETION_DATE_KEYS)) ?: Instant.now()

        return Puzzle(
            name = name.take(200),
            pieces = pieces,
            rating = rating,
            difficulty = difficulty,
            estimatedTimeSpent = null,
            completionDate = completionDate,
            status = status,
            notes = notes,
            source = brand?.take(200),
            progressPercent = progress,
            barcode = barcode,
        )
    }

    private val TITLE_KEYS = listOf("name", "title", "puzzle name", "puzzle title", "name title", "puzzle")
    private val BRAND_KEYS = listOf("brand", "manufacturer", "source", "puzzle brand")
    private val PIECES_KEYS = listOf("pieces", "piece count", "piececount", "number of pieces", "of pieces", "pcs")
    private val BARCODE_KEYS = listOf("barcode", "upc", "ean", "barcode on the box", "bar code")
    private val STATUS_KEYS = listOf("status", "folder", "collection status", "my status", "puzzle status")
    private val PROGRESS_KEYS = listOf("progress", "progress percent", "percent complete", "% complete", "completion percent")
    private val NOTES_KEYS = listOf("notes", "note", "comments", "private notes", "my notes")
    private val RATING_KEYS = listOf("rating", "my rating", "user rating", "star rating")
    private val DIFFICULTY_KEYS = listOf("difficulty", "my difficulty", "puzzle difficulty")
    private val COMPLETION_DATE_KEYS = listOf("completion date", "completed date", "date completed", "finished date", "completiondate")
    private val MANUFACTURER_ID_KEYS = listOf("manufacturer id", "manufacturer id reference", "sku", "product id", "reference number")

    private fun normalizeKeys(record: Map<String, String>): Map<String, String> =
        record.mapKeys { (key, _) -> normalizeHeader(key) }

    private fun normalizeHeader(header: String): String =
        header.lowercase()
            .replace("/", " ")
            .replace("_", " ")
            .replace("#", "")
            .replace("  ", " ")
            .trim()

    private fun firstValue(record: Map<String, String>, keys: List<String>): String? {
        for (key in keys) {
            val value = record[key]?.trim()
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private fun mergedNotes(record: Map<String, String>): String? {
        val parts = buildList {
            firstValue(record, NOTES_KEYS)?.let { add(it) }
            firstValue(record, MANUFACTURER_ID_KEYS)?.let { add("Manufacturer ID: $it") }
        }
        val merged = parts.joinToString("\n").trim()
        if (merged.isEmpty()) return null
        return merged.take(2000)
    }

    private fun parsePieces(raw: String): Int? {
        val digits = raw.filter { it.isDigit() }
        val value = digits.toIntOrNull() ?: return null
        return value.takeIf { it > 0 }
    }

    private fun optionalDigits(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return digits.ifEmpty { null }
    }

    private fun parseStatus(raw: String?): PuzzleStatus {
        val value = raw?.lowercase().orEmpty()
        return when {
            value.contains("complete") || value.contains("finished") || value.contains("done") -> PuzzleStatus.COMPLETED
            value.contains("progress") || value.contains("started") || value.contains("working") -> PuzzleStatus.IN_PROGRESS
            else -> PuzzleStatus.TODO
        }
    }

    private fun parseProgress(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() }
        val value = digits.toIntOrNull() ?: return null
        return PuzzleProgressSemantics.clamped(value)
    }

    private fun parseRating(raw: String?): PuzzleRating {
        if (raw.isNullOrBlank()) return PuzzleRating.NONE
        val numeric = raw.filter { it.isDigit() || it == '.' }
        val value = numeric.toDoubleOrNull() ?: return PuzzleRating.NONE
        val snapped = (value * 2).toInt() / 2.0
        return PuzzleRating.entries.minByOrNull { kotlin.math.abs(it.value - snapped) } ?: PuzzleRating.NONE
    }

    private fun parseDifficulty(raw: String?): PuzzleDifficulty {
        val digit = raw?.firstOrNull { it.isDigit() } ?: return PuzzleDifficulty.NONE
        val value = digit.digitToIntOrNull() ?: return PuzzleDifficulty.NONE
        if (value !in 1..5) return PuzzleDifficulty.NONE
        return PuzzleDifficulty.entries.firstOrNull { it.raw == value.toString() } ?: PuzzleDifficulty.NONE
    }

    private fun parseDate(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val formats = listOf("yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "M/d/yyyy", "d/M/yyyy")
        for (pattern in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern)
                val date = LocalDate.parse(raw, formatter)
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private fun decodeText(data: ByteArray): String? =
        data.toString(Charsets.UTF_8).takeIf { it.isNotEmpty() }
            ?: data.toString(Charsets.ISO_8859_1).takeIf { it.isNotEmpty() }
}
