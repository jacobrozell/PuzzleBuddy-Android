package com.jacobrozell.puzzlebuddy.domain.loan

import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import java.time.Instant
import java.time.ZoneId

object PuzzleLoanSemantics {
    fun dispositionEndsOwnership(disposition: PuzzleDisposition): Boolean = when (disposition) {
        PuzzleDisposition.GIFTED,
        PuzzleDisposition.SOLD,
        PuzzleDisposition.DONATED,
        PuzzleDisposition.TRASHED,
        -> true
        PuzzleDisposition.NONE,
        PuzzleDisposition.KEPT,
        -> false
    }

    fun isOverdue(
        puzzle: Puzzle,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val due = puzzle.dueBackDate ?: return false
        if (!puzzle.isOnLoan) return false
        val dueDay = due.atZone(zoneId).toLocalDate()
        val today = now.atZone(zoneId).toLocalDate()
        return dueDay < today
    }

    fun listBadgeTitle(
        puzzle: Puzzle,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = if (isOverdue(puzzle, now, zoneId)) "Overdue" else "On loan"

    fun listBadgeAccessibilityLabel(
        puzzle: Puzzle,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        if (isOverdue(puzzle, now, zoneId)) {
            val due = puzzle.dueBackDate
            if (due != null) {
                return "Overdue, due ${MediumDateLabel.format(due, zoneId)}"
            }
        }
        val name = puzzle.loanedToDisplayName?.trim()
        if (!name.isNullOrEmpty()) return "On loan to $name"
        return "On loan"
    }

    fun dueBackDisplayValue(
        puzzle: Puzzle,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String? {
        val due = puzzle.dueBackDate ?: return null
        val date = MediumDateLabel.format(due, zoneId)
        return if (isOverdue(puzzle, now, zoneId)) "$date · Overdue" else date
    }

    fun clearLoan(puzzle: Puzzle): Puzzle = puzzle.copy(
        isOnLoan = false,
        loanedToFriendId = null,
        loanedToDisplayName = null,
        loanedAt = null,
        dueBackDate = null,
        lastLoanNudgeAt = null,
    )

    fun prepareLoanState(puzzle: Puzzle, now: Instant): Puzzle {
        if (dispositionEndsOwnership(puzzle.disposition) || !puzzle.isOnLoan) {
            return clearLoan(puzzle)
        }
        return puzzle.copy(
            loanedAt = puzzle.loanedAt ?: now,
            loanedToDisplayName = FriendSemantics.normalizedDisplayName(puzzle.loanedToDisplayName),
        )
    }
}
