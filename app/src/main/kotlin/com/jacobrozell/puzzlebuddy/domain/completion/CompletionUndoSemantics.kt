package com.jacobrozell.puzzlebuddy.domain.completion

import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import java.time.Duration
import java.time.Instant

data class CompletionUndoSnapshot(
    val puzzleId: String,
    val completionId: String,
    val previousStatus: PuzzleStatus,
    val previousProgressPercent: Int,
    val recordedAt: Instant,
) {
    fun isActive(now: Instant, window: Duration = CompletionUndoSemantics.WINDOW): Boolean =
        Duration.between(recordedAt, now) <= window
}

object CompletionUndoSemantics {
    val WINDOW: Duration = Duration.ofMinutes(5)

    fun snapshot(
        puzzleId: String,
        completionId: String,
        previousStatus: PuzzleStatus,
        previousProgressPercent: Int,
        recordedAt: Instant,
    ): CompletionUndoSnapshot = CompletionUndoSnapshot(
        puzzleId = puzzleId,
        completionId = completionId,
        previousStatus = previousStatus,
        previousProgressPercent = PuzzleProgressSemantics.clamped(previousProgressPercent),
        recordedAt = recordedAt,
    )

    fun activeSnapshot(
        snapshot: CompletionUndoSnapshot?,
        puzzleId: String,
        now: Instant,
        window: Duration = WINDOW,
    ): CompletionUndoSnapshot? {
        if (snapshot == null || snapshot.puzzleId != puzzleId || !snapshot.isActive(now, window)) return null
        return snapshot
    }
}
