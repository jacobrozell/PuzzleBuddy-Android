package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus

object PuzzleProgressSemantics {
    fun clamped(value: Int): Int = value.coerceIn(0, 100)

    fun statusFor(progress: Int): PuzzleStatus = when (clamped(progress)) {
        0 -> PuzzleStatus.TODO
        100 -> PuzzleStatus.COMPLETED
        else -> PuzzleStatus.IN_PROGRESS
    }

    fun progressFor(status: PuzzleStatus, current: Int): Int = when (status) {
        PuzzleStatus.TODO -> 0
        PuzzleStatus.COMPLETED -> 100
        PuzzleStatus.IN_PROGRESS -> {
            val clamped = clamped(current)
            when (clamped) {
                0, 100 -> 10
                else -> clamped
            }
        }
    }

    fun displayLabel(progress: Int): String = "${clamped(progress)}% complete"
}
