package com.jacobrozell.puzzlebuddy.domain.export

import com.jacobrozell.puzzlebuddy.data.export.PuzzleCollectionBackup
import com.jacobrozell.puzzlebuddy.data.repository.InMemoryPhotoFileStore
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64

class PuzzleCollectionBackupTest {
    @Test
    fun sparseBackupFillsOptionalFields() {
        val puzzles = PuzzleCollectionJSONImporter.puzzles(SPARSE.toByteArray())
        assertEquals(2, puzzles.size)

        val completed = puzzles.first { it.name == "Sparse Sunrise" }
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", completed.id)
        assertEquals("Galison", completed.source)
        assertEquals(PuzzleStatus.COMPLETED, completed.status)
        assertEquals(PuzzleShape.NONE, completed.puzzleShape)
        assertEquals(PuzzleCutType.NONE, completed.cutType)
        assertEquals(PuzzleType.OTHER, completed.puzzleType)
        assertEquals(PuzzleDisposition.NONE, completed.disposition)
        assertEquals(PuzzleMaterial.CARDBOARD, completed.material)
        assertNull(completed.purchasePrice)
        assertEquals(1, completed.timesCompleted)
        assertEquals(1, completed.completions.size)
        assertTrue(completed.photos.isEmpty())
        assertEquals(listOf("landscape"), completed.tags)

        val todo = puzzles.first { it.name == "Minimal Row" }
        assertEquals(PuzzleStatus.TODO, todo.status)
        assertEquals(0, todo.progressPercent)
    }

    @Test
    fun importerRejectsANewerFormatVersion() {
        val json = """
            {"backupFormatVersion":99,"puzzles":[{
            "name":"Future","status":"To-Do","rating":0,"difficulty":"0",
            "completionDate":"2024-01-01T00:00:00Z","puzzleType":"None","material":"None",
            "disposition":"None","progressPercent":0,"timesCompleted":0,"tags":[],
            "hasMissingPieces":false,"hasImage":false,"photoCount":0,"completions":[]
            }]}
        """.trimIndent()
        val error = runCatching { PuzzleCollectionJSONImporter.puzzles(json.toByteArray()) }.exceptionOrNull()
        assertTrue(error is PuzzleCollectionJSONImportError.UnsupportedFormatVersion)
        assertEquals(99, (error as PuzzleCollectionJSONImportError.UnsupportedFormatVersion).version)
    }

    @Test
    fun expandedMetadataAndPhotosRoundTrip() {
        val photo = PuzzlePhoto(sortOrder = 0, createdAt = Instant.parse("2024-01-02T00:00:00Z"))
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val puzzle = Puzzle(
            name = "Expanded",
            pieces = 1000,
            status = PuzzleStatus.IN_PROGRESS,
            progressPercent = 40,
            puzzleShape = PuzzleShape.ROUND,
            cutType = PuzzleCutType.GRID,
            dimensionsText = "68 × 49 cm",
            purchasePrice = 19.99,
            purchaseCurrencyCode = "USD",
            timesCompleted = 2,
            photos = listOf(photo),
            completions = listOf(
                PuzzleCompletion(completionNumber = 1, completedAt = Instant.parse("2024-01-01T00:00:00Z")),
                PuzzleCompletion(completionNumber = 2, completedAt = Instant.parse("2024-06-01T00:00:00Z")),
            ),
            completionDate = Instant.parse("2024-06-01T00:00:00Z"),
        )
        val data = PuzzleCollectionExporter.jsonData(
            listOf(puzzle),
            appVersion = "1.1.0",
            photoBytes = mapOf(photo.id to bytes),
        )
        val text = data.decodeToString()
        assertTrue(text.contains("\"backupFormatVersion\": 1"))
        assertTrue(text.contains("Round"))
        assertTrue(text.contains("68 × 49 cm"))
        assertTrue(text.contains(Base64.getEncoder().encodeToString(bytes)))

        val parsed = PuzzleCollectionJSONImporter.parse(data)
        val restored = parsed.puzzles.single()
        assertEquals(PuzzleShape.ROUND, restored.puzzleShape)
        assertEquals(PuzzleCutType.GRID, restored.cutType)
        assertEquals("68 × 49 cm", restored.dimensionsText)
        assertEquals(19.99, restored.purchasePrice)
        assertEquals(2, restored.timesCompleted)
        assertEquals(1, restored.photos.size)
        assertEquals(2, restored.completions.size)
        assertTrue(bytes.contentEquals(parsed.photoBytes[restored.photos.single().id]))
    }

    @Test
    fun timerFieldsRoundTripAndMissingKeysDefault() {
        val anchor = Instant.ofEpochSecond(1_800_000_000)
        val puzzle = Puzzle(
            name = "Timing",
            status = PuzzleStatus.IN_PROGRESS,
            progressPercent = 20,
            timerAccumulatedSeconds = 4_321,
            timerRunningSince = anchor,
            completionDate = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val restored = PuzzleCollectionJSONImporter.puzzles(
            PuzzleCollectionExporter.jsonData(listOf(puzzle), appVersion = "1.1.0"),
        ).single()
        assertEquals(4_321L, restored.timerAccumulatedSeconds)
        assertEquals(anchor, restored.timerRunningSince)

        val old = """
            {"backupFormatVersion":1,"puzzles":[{
            "name":"Pre-timer","status":"In-Progress","rating":0,"difficulty":"0",
            "completionDate":"2024-01-01T00:00:00Z","progressPercent":30,"tags":[]
            }]}
        """.trimIndent()
        val preTimer = PuzzleCollectionJSONImporter.puzzles(old.toByteArray()).single()
        assertEquals("Pre-timer", preTimer.name)
        assertEquals(0L, preTimer.timerAccumulatedSeconds)
        assertNull(preTimer.timerRunningSince)
    }

    @Test
    fun loanAndFriendsRoundTrip() {
        val friend = Friend.create(
            displayName = "Mom",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val puzzle = Puzzle(
            name = "Export Loan",
            pieces = 500,
            isOnLoan = true,
            loanedToFriendId = friend.id,
            dueBackDate = Instant.ofEpochSecond(1_700_000_000),
            completionDate = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val parsed = PuzzleCollectionJSONImporter.parse(
            PuzzleCollectionExporter.jsonData(listOf(puzzle), appVersion = "1.1.0", friends = listOf(friend)),
        )
        assertEquals("Mom", parsed.friends.single().displayName)
        assertTrue(parsed.puzzles.single().isOnLoan)
        assertEquals(friend.id, parsed.puzzles.single().loanedToFriendId)
        assertEquals(Instant.ofEpochSecond(1_700_000_000), parsed.puzzles.single().dueBackDate)
    }

    @Test
    fun emptyFileIsRejected() {
        val error = runCatching { PuzzleCollectionJSONImporter.parse(ByteArray(0)) }.exceptionOrNull()
        assertTrue(error is PuzzleCollectionJSONImportError.EmptyFile)
    }

    @Test
    fun fileStoreBytesAreEmbedded() = runTest {
        val store = InMemoryPhotoFileStore()
        val name = store.write(byteArrayOf(9, 8, 7))
        val photo = PuzzlePhoto(sortOrder = 0, fileName = name, createdAt = Instant.parse("2024-02-02T00:00:00Z"))
        val puzzle = Puzzle(
            name = "Filed",
            photos = listOf(photo),
            completionDate = Instant.parse("2024-02-02T00:00:00Z"),
        )
        val parsed = PuzzleCollectionJSONImporter.parse(
            PuzzleCollectionBackup.jsonData(listOf(puzzle), emptyList(), store, "1.1.0"),
        )
        assertTrue(byteArrayOf(9, 8, 7).contentEquals(parsed.photoBytes.values.single()))
        assertFalse(parsed.puzzles.single().photos.isEmpty())
    }

    private companion object {
        val SPARSE = """
            {
              "backupFormatVersion": 1,
              "exportedAt": "2024-06-01T12:00:00Z",
              "appVersion": "1.0.0",
              "puzzleCount": 2,
              "puzzles": [
                {
                  "id": "A1B2C3D4-E5F6-7890-ABCD-EF1234567890",
                  "name": "Sparse Sunrise",
                  "pieces": 500,
                  "status": "Completed",
                  "rating": 4.0,
                  "difficulty": "2",
                  "completionDate": "2024-05-15T00:00:00Z",
                  "source": "Galison",
                  "barcode": "012345678905",
                  "progressPercent": 100,
                  "tags": ["landscape"],
                  "puzzleType": "Jigsaw",
                  "material": "Cardboard",
                  "disposition": "Keep",
                  "hasMissingPieces": false
                },
                {
                  "id": "B2C3D4E5-F6A7-8901-BCDE-F12345678901",
                  "name": "Minimal Row",
                  "pieces": 1000,
                  "status": "To-Do",
                  "rating": 0.0,
                  "difficulty": "0",
                  "completionDate": "2024-06-01T12:00:00Z",
                  "progressPercent": 0,
                  "tags": [],
                  "puzzleType": "None",
                  "material": "None",
                  "disposition": "None",
                  "hasMissingPieces": false
                }
              ]
            }
        """.trimIndent()
    }
}
