package com.jacobrozell.puzzlebuddy.data.local

import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PuzzleMapperRoundTripTest {
    @Test
    fun fullyPopulatedPuzzleRoundTrips() {
        val whenAt = Instant.parse("2026-06-01T12:00:00Z")
        val friend = Friend(
            id = "friend-1",
            displayName = "Mom",
            createdAt = whenAt,
            updatedAt = whenAt,
            isDemo = true,
        )
        val puzzle = Puzzle(
            id = "puzzle-1",
            name = "Paris",
            pieces = 1000,
            status = PuzzleStatus.COMPLETED,
            completionDate = whenAt,
            startDate = whenAt,
            puzzleType = PuzzleType.COLLAGE,
            disposition = PuzzleDisposition.KEPT,
            purchasePrice = 18.0,
            purchaseCurrencyCode = "USD",
            timesCompleted = 2,
            isOnLoan = true,
            loanedToFriendId = friend.id,
            loanedAt = whenAt,
            dueBackDate = whenAt,
            timerAccumulatedSeconds = 90,
            photos = listOf(PuzzlePhoto(id = "photo-1", sortOrder = 0, fileName = "a.jpg", createdAt = whenAt)),
            completions = listOf(
                PuzzleCompletion(id = "c1", completionNumber = 1, completedAt = whenAt, rating = 3.0),
                PuzzleCompletion(id = "c2", completionNumber = 2, completedAt = whenAt, rating = 4.0),
            ),
        )
        val row = PuzzleWithChildren(
            puzzle = puzzle.toEntity(),
            photos = puzzle.photos.map { it.toEntity(puzzle.id) },
            completions = puzzle.completions.map { it.toEntity(puzzle.id) },
        )
        val restored = row.toDomain(mapOf(friend.id to friend))
        assertEquals("Paris", restored.name)
        assertEquals(PuzzleType.COLLAGE, restored.puzzleType)
        assertEquals("Mom", restored.loanedToDisplayName)
        assertEquals(2, restored.timesCompleted)
        assertEquals(listOf("a.jpg"), restored.photos.map { it.fileName })
        assertEquals(listOf(1, 2), restored.completions.map { it.completionNumber })
        assertEquals(90, restored.timerAccumulatedSeconds)
        assertEquals(PuzzleType.NONE, PuzzleType.fromRaw("nope"))
    }
}
