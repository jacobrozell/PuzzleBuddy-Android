package com.jacobrozell.puzzlebuddy.domain.importing

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IPDbCSVImporterTest {
    @Test
    fun parsesStandardIpdbCsv() {
        val csv = """
            Title,Brand,Piece Count,Barcode,Status,Notes
            Winter Lights,Galison,1000,012345678905,Completed,Gift from Mom
            Harbor View,Ravensburger,500,,Wishlist,
        """.trimIndent()
        val puzzles = IPDbCSVImporter.puzzlesFrom(csv.toByteArray())
        assertEquals(2, puzzles.size)
        assertEquals("Winter Lights", puzzles[0].name)
        assertEquals("Galison", puzzles[0].source)
        assertEquals(1000, puzzles[0].pieces)
        assertEquals("012345678905", puzzles[0].barcode)
        assertEquals(PuzzleStatus.COMPLETED, puzzles[0].status)
        assertEquals("Gift from Mom", puzzles[0].notes)
        assertEquals(PuzzleStatus.TODO, puzzles[1].status)
        assertNull(puzzles[1].barcode)
    }

    @Test
    fun parsesSemicolonDelimitedCsv() {
        val csv = """
            Title;Brand;Piece Count;Barcode
            Cabin Retreat;Buffalo Games;750;818870028198
        """.trimIndent()
        val puzzles = IPDbCSVImporter.puzzlesFrom(csv.toByteArray())
        assertEquals(1, puzzles.size)
        assertEquals("818870028198", puzzles.first().barcode)
    }

    @Test
    fun includesManufacturerIdInNotes() {
        val puzzle = IPDbCSVImporter.puzzleFrom(
            mapOf(
                "Title" to "Death Foretold",
                "Brand" to "Parker Brothers",
                "Piece Count" to "500",
                "Manufacturer ID" to "4354-9",
            ),
        )
        assertTrue(puzzle?.notes?.contains("4354-9") == true)
    }

    @Test(expected = IPDbCSVImportError.MissingTitleColumn::class)
    fun throwsWhenNoValidRows() {
        val csv = "Brand,Piece Count\nGalison,1000\n"
        IPDbCSVImporter.puzzlesFrom(csv.toByteArray())
    }

    @Test
    fun parsesProgressPercentColumn() {
        val puzzle = IPDbCSVImporter.puzzleFrom(
            mapOf(
                "Title" to "Mountain Cabin",
                "Folder" to "In-Progress",
                "Progress Percent" to "35",
            ),
        )
        assertEquals(PuzzleStatus.IN_PROGRESS, puzzle?.status)
        assertEquals(35, puzzle?.progressPercent)
    }
}
