package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DemoDataCatalog {
    const val PUZZLE_COUNT = 4

    fun makePuzzles(): List<Puzzle> = listOf(
        demoFixture("Mountain Sunset", 500, PuzzleRating.FOUR),
        demoFixture("Ocean Breeze", 1000, PuzzleRating.FIVE),
        inProgressFixture("Tabletop Sky", 300),
        completedFixture("Harbor Lights", 750),
    )

    private fun demoFixture(
        name: String,
        pieces: Int,
        rating: PuzzleRating = PuzzleRating.NONE,
        difficulty: PuzzleDifficulty = PuzzleDifficulty.NONE,
    ): Puzzle = Puzzle(
        name = name,
        pieces = pieces,
        rating = rating,
        difficulty = difficulty,
        estimatedTimeSpent = null,
        completionDate = Instant.now(),
        isDemo = true,
    )

    private fun completedFixture(name: String, pieces: Int): Puzzle =
        demoFixture(name, pieces, PuzzleRating.THREE).copy(
            status = PuzzleStatus.COMPLETED,
            progressPercent = 100,
            source = "Retail store",
        )

    private fun inProgressFixture(name: String, pieces: Int): Puzzle =
        demoFixture(name, pieces, PuzzleRating.TWO).copy(
            status = PuzzleStatus.IN_PROGRESS,
            progressPercent = 45,
            source = "Gift",
        )
}
