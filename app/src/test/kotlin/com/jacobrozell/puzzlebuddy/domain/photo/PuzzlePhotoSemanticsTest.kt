package com.jacobrozell.puzzlebuddy.domain.photo

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Ports iOS `PuzzlePhotoSemanticsTests` (tag 1.1.0). */
class PuzzlePhotoSemanticsTest {
    private fun photo(sortOrder: Int, name: String = "p$sortOrder.jpg") =
        PuzzlePhoto(sortOrder = sortOrder, fileName = name, createdAt = Instant.EPOCH)

    @Test
    fun normalizedSortOrdersAreZeroBased() {
        val normalized = PuzzlePhotoSemantics.sortedAndNormalized(listOf(photo(2), photo(0)))
        assertEquals(listOf(0, 1), normalized.map { it.sortOrder })
    }

    @Test
    fun coverUsesLowestSortOrder() {
        val photos = listOf(photo(1, "second.jpg"), photo(0, "first.jpg"))
        assertEquals(0, PuzzlePhotoSemantics.photosInOrder(photos).first().sortOrder)
        assertEquals("first.jpg", PuzzlePhotoSemantics.cover(photos)?.fileName)
    }

    @Test
    fun prepareForPersistenceEnforcesPhotoLimit() {
        val puzzle = Puzzle(name = "Many", pieces = 100, photos = (0 until 7).map { photo(it) })
            .preparedForPersistence("USD")
        assertEquals(MAX_PHOTOS, puzzle.photos.size)
        assertEquals((0 until MAX_PHOTOS).toList(), puzzle.photos.map { it.sortOrder })
    }

    @Test
    fun movingPhotoBeforeDestinationUpdatesCover() {
        val (first, second, third) = listOf(photo(0), photo(1), photo(2))
        val reordered = PuzzlePhotoSemantics.movingPhoto(third.id, first.id, listOf(first, second, third))
        assertEquals(listOf(third.id, first.id, second.id), reordered.map { it.id })
        assertEquals(third.id, PuzzlePhotoSemantics.cover(reordered)?.id)
    }

    @Test
    fun movingPhotoOneStepLater() {
        val (first, second, third) = listOf(photo(0), photo(1), photo(2))
        val reordered = PuzzlePhotoSemantics.movingPhotoOneStep(
            first.id,
            PuzzlePhotoSemantics.MoveDirection.LATER,
            listOf(first, second, third),
        )
        assertEquals(listOf(second.id, first.id, third.id), reordered.map { it.id })
    }

    @Test
    fun movingPhotoToEnd() {
        val (first, second, third) = listOf(photo(0), photo(1), photo(2))
        val reordered = PuzzlePhotoSemantics.movingPhotoToEnd(first.id, listOf(first, second, third))
        assertEquals(listOf(second.id, third.id, first.id), reordered.map { it.id })
    }

    @Test
    fun movingPhotoToCover() {
        val (first, second) = listOf(photo(0), photo(1))
        val reordered = PuzzlePhotoSemantics.movingPhotoToCover(second.id, listOf(first, second))
        assertEquals(listOf(second.id, first.id), reordered.map { it.id })
        assertEquals(second.id, PuzzlePhotoSemantics.cover(reordered)?.id)
    }
}
