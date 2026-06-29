package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object IPDbCSVFormat {
    val columnHeaders = listOf(
        "Title",
        "Brand",
        "Piece Count",
        "Barcode",
        "Folder",
        "Rating",
        "Difficulty",
        "Completion Date",
        "Notes",
        "Progress Percent",
        "Manufacturer ID",
    )

    fun csvData(puzzles: List<Puzzle>): ByteArray {
        val rows = buildList {
            add(columnHeaders.joinToString(","))
            puzzles.forEach { puzzle ->
                add(rowValues(puzzle).joinToString(",") { csvField(it) })
            }
        }
        return rows.joinToString("\n").toByteArray(Charsets.UTF_8)
    }

    fun rowValues(puzzle: Puzzle): List<String> {
        val (notes, manufacturerId) = splitManufacturerId(puzzle.notes)
        return listOf(
            puzzle.name,
            puzzle.source.orEmpty(),
            puzzle.pieces?.toString().orEmpty(),
            puzzle.barcode.orEmpty(),
            folderValue(puzzle.status),
            if (puzzle.rating == PuzzleRating.NONE) "" else puzzle.rating.value.toString(),
            if (puzzle.difficulty.intValue == 0) "" else puzzle.difficulty.raw,
            completionDateValue(puzzle),
            notes.orEmpty(),
            progressValue(puzzle),
            manufacturerId.orEmpty(),
        )
    }

    fun folderValue(status: PuzzleStatus): String = when (status) {
        PuzzleStatus.TODO -> "Wishlist"
        PuzzleStatus.IN_PROGRESS -> "In-Progress"
        PuzzleStatus.COMPLETED -> "Completed"
    }

    fun splitManufacturerId(notes: String?): Pair<String?, String?> {
        if (notes.isNullOrBlank()) return null to null
        val prefix = "Manufacturer ID: "
        var manufacturerId: String? = null
        val remaining = notes.lines().filter { line ->
            if (line.startsWith(prefix)) {
                manufacturerId = line.removePrefix(prefix).trim()
                false
            } else {
                true
            }
        }.joinToString("\n").trim()
        return (remaining.ifEmpty { null } to manufacturerId)
    }

    private fun completionDateValue(puzzle: Puzzle): String {
        if (puzzle.status != PuzzleStatus.COMPLETED) return ""
        return completionDateFormatter.format(puzzle.completionDate.atZone(ZoneId.systemDefault()))
    }

    private fun progressValue(puzzle: Puzzle): String {
        val progress = PuzzleProgressSemantics.clamped(puzzle.progressPercent)
        return if (progress > 0) progress.toString() else ""
    }

    private val completionDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun csvField(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        if (!needsQuoting) return value
        val escaped = value
            .replace("\"", "\"\"")
            .replace("\n", " ")
            .replace("\r", " ")
        return "\"$escaped\""
    }
}
