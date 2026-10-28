package com.jacobrozell.puzzlebuddy.domain.completion

import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import java.time.Instant
import java.time.ZoneId

object PuzzleCompletionHistoryCopy {
    const val SWIPE_REMOVE_TITLE = "Remove"
    const val SWIPE_EDIT_TITLE = "Edit"
    const val DELETE_DIALOG_TITLE = "Remove completion?"
    const val LAST_COMPLETION_STATUS_PROMPT =
        "This was your only completion log. Choose a new status for the puzzle."

    fun supportsSwipeActions(completionCount: Int): Boolean = completionCount > 0

    fun deleteDialogMessage(
        completionNumber: Int,
        completedAt: Instant,
        remainingCount: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = MediumDateLabel.format(completedAt, zoneId)
        return if (remainingCount <= 1) {
            "Remove the finish log from $date?"
        } else {
            "Remove completion #$completionNumber from $date? Other finishes stay in your history."
        }
    }
}
