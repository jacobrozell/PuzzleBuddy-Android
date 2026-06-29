package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PuzzleCollectionExporterTest {
    private val sample = Puzzle(
        name = "Winter Lights",
        pieces = 1000,
        status = PuzzleStatus.COMPLETED,
        rating = PuzzleRating.fromValue(4.5),
        difficulty = PuzzleDifficulty.THREE,
        completionDate = Instant.parse("2025-01-15T10:00:00Z"),
        barcode = "012345678905",
        source = "Ravensburger",
        tags = listOf("winter"),
    )

    @Test
    fun jsonExportIncludesPuzzleName() {
        val json = PuzzleCollectionExporter.jsonData(listOf(sample)).decodeToString()
        assertTrue(json.contains("Winter Lights"))
        assertTrue(json.contains("012345678905"))
    }

    @Test
    fun csvExportUsesIpdbHeaders() {
        val csv = PuzzleCollectionExporter.csvData(listOf(sample)).decodeToString()
        assertTrue(csv.startsWith("Title,Brand,Piece Count,Barcode"))
        assertTrue(csv.contains("Winter Lights"))
        assertTrue(csv.contains("Ravensburger"))
    }

    @Test
    fun emptyCollectionThrows() {
        try {
            PuzzleCollectionExporter.jsonData(emptyList())
            error("Expected export error")
        } catch (error: PuzzleCollectionExportError) {
            assertEquals("Add at least one puzzle before exporting.", error.message)
        }
    }
}
