package com.jacobrozell.puzzlebuddy.domain.completion

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** Ports iOS `PuzzleCompletionHistoryCopyTests` and `CompletionUndoSemanticsTests` (tag 1.1.0). */
class CompletionHistoryAndUndoTest {
    private val finishedAt = Instant.ofEpochSecond(1_704_067_200)

    @Test
    fun swipeActionsAvailableWhenHistoryExists() {
        assertTrue(PuzzleCompletionHistoryCopy.supportsSwipeActions(1))
        assertTrue(PuzzleCompletionHistoryCopy.supportsSwipeActions(3))
        assertFalse(PuzzleCompletionHistoryCopy.supportsSwipeActions(0))
    }

    @Test
    fun swipeLabelsAreShortAndDestructive() {
        assertEquals("Remove", PuzzleCompletionHistoryCopy.SWIPE_REMOVE_TITLE)
        assertEquals("Edit", PuzzleCompletionHistoryCopy.SWIPE_EDIT_TITLE)
        assertEquals("Remove completion?", PuzzleCompletionHistoryCopy.DELETE_DIALOG_TITLE)
    }

    @Test
    fun deleteMessageForLastCompletionOmitsOtherFinishesCopy() {
        val message = PuzzleCompletionHistoryCopy.deleteDialogMessage(1, finishedAt, 1, ZoneOffset.UTC)
        assertTrue(message.contains("finish log"))
        assertFalse(message.contains("Other finishes"))
    }

    @Test
    fun deleteMessageForOneOfManyKeepsOtherFinishes() {
        val message = PuzzleCompletionHistoryCopy.deleteDialogMessage(2, finishedAt, 3, ZoneOffset.UTC)
        assertTrue(message.contains("#2"))
        assertTrue(message.contains("Other finishes stay in your history."))
    }

    @Test
    fun snapshotIsActiveInsideWindow() {
        val recordedAt = Instant.ofEpochSecond(1_000)
        val snapshot = CompletionUndoSemantics.snapshot("p", "c", PuzzleStatus.IN_PROGRESS, 45, recordedAt)
        assertTrue(snapshot.isActive(recordedAt.plusSeconds(60)))
        assertTrue(snapshot.isActive(recordedAt.plus(CompletionUndoSemantics.WINDOW)))
        assertFalse(snapshot.isActive(recordedAt.plus(CompletionUndoSemantics.WINDOW).plusSeconds(1)))
    }

    @Test
    fun activeSnapshotRequiresMatchingPuzzle() {
        val now = Instant.ofEpochSecond(1_000)
        val snapshot = CompletionUndoSemantics.snapshot("p", "c", PuzzleStatus.TODO, 0, now)
        assertNotNull(CompletionUndoSemantics.activeSnapshot(snapshot, "p", now))
        assertNull(CompletionUndoSemantics.activeSnapshot(snapshot, "other", now))
        assertNull(CompletionUndoSemantics.activeSnapshot(null, "p", now))
    }

    @Test
    fun snapshotClampsPreviousProgress() {
        val snapshot = CompletionUndoSemantics.snapshot("p", "c", PuzzleStatus.IN_PROGRESS, 140, Instant.EPOCH)
        assertEquals(100, snapshot.previousProgressPercent)
    }
}
