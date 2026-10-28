package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DemoShelf(
    val puzzles: List<Puzzle>,
    val demoPhotos: Map<String, List<String>>,
)

object DemoDataCatalog {
    const val PUZZLE_COUNT = 13
    const val DUPLICATE_CHECK_PUZZLE_NAME = "The Bizarre Bookshop"
    const val COMPLETED_PUZZLE_NAME = "Paris in a Day"
    const val PRIMARY_SEEDED_PUZZLE_NAME = DUPLICATE_CHECK_PUZZLE_NAME

    fun make(now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): DemoShelf {
        val puzzles = listOf(
            bizarreBookshop(),
            canalCruise(),
            veniceRomance(now, zoneId),
            parisInADay(now, zoneId),
            winterInCentralPark(now, zoneId),
            starryNight(now, zoneId),
            countryCottage(now, zoneId),
            canalAtSunset(now, zoneId),
            yosemiteValley(now, zoneId),
            floralArch(now, zoneId),
            underwaterParadise(now, zoneId),
            gradientLines(),
            mysteryAmusementPark(),
        )
        val photos = puzzles.associate { puzzle ->
            puzzle.id to puzzle.photos.mapNotNull { photo ->
                photo.fileName?.removeSuffix(".jpg")
            }
        }
        return DemoShelf(puzzles, photos)
    }

    fun makePuzzles(now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): List<Puzzle> =
        make(now, zoneId).puzzles

    private fun bizarreBookshop(): Puzzle = demo(
        name = DUPLICATE_CHECK_PUZZLE_NAME,
        pieces = 1000,
        rating = PuzzleRating.FOUR,
        difficulty = PuzzleDifficulty.THREE,
    ).copy(
        source = "Ravensburger",
        barcode = "012345678905",
        purchaseLocation = "Target",
        releaseYear = 2019,
        puzzleType = PuzzleType.COLLAGE,
        dimensionsText = "27 × 20 in",
        purchasePrice = 24.99,
        purchaseCurrencyCode = "USD",
        tags = listOf("books", "colin thompson", "cozy"),
        notes = "Quirky book titles on every shelf — a fun Sunday build.",
        photos = photos("bizarre-bookshop-cover"),
    )

    private fun canalCruise(): Puzzle = demo(
        name = "Canal Cruise in Venice",
        pieces = 1000,
        rating = PuzzleRating.FIVE,
        difficulty = PuzzleDifficulty.THREE,
    ).copy(
        source = "Ravensburger",
        barcode = "012345678906",
        purchaseLocation = "Amazon",
        releaseYear = 2021,
        puzzleType = PuzzleType.LANDSCAPE,
        dimensionsText = "27 × 20 in",
        purchasePrice = 22.99,
        purchaseCurrencyCode = "USD",
        tags = listOf("venice", "travel", "europe"),
        photos = photos("canal-cruise-venice-cover"),
    )

    private fun veniceRomance(now: Instant, zoneId: ZoneId): Puzzle = demo(
        name = "Venice Romance",
        pieces = 1000,
        rating = PuzzleRating.TWO,
        difficulty = PuzzleDifficulty.FOUR,
    ).copy(
        status = PuzzleStatus.IN_PROGRESS,
        progressPercent = 45,
        source = "Ravensburger",
        barcode = "012345678907",
        purchaseLocation = "Thrift store",
        releaseYear = 2018,
        puzzleType = PuzzleType.LANDSCAPE,
        dimensionsText = "27 × 20 in",
        purchasePrice = 6.00,
        purchaseCurrencyCode = "USD",
        estimatedTimeSpent = PuzzleTime(4, 30),
        startDate = daysAgo(3, now, zoneId),
        tags = listOf("venice", "sunset", "travel"),
        photos = photos("venice-romance-cover"),
    )

    private fun parisInADay(now: Instant, zoneId: ZoneId): Puzzle {
        val completedOn = dateInCurrentYear(6, 18, now, zoneId)
        return completed(
            name = COMPLETED_PUZZLE_NAME,
            pieces = 1000,
            rating = PuzzleRating.THREE,
            difficulty = PuzzleDifficulty.THREE,
            completedOn = completedOn,
            startedDaysBefore = 5,
            hours = 8,
            minutes = 15,
            zoneId = zoneId,
        ).copy(
            source = "Galison",
            barcode = "012345678908",
            purchaseLocation = "Local bookshop",
            releaseYear = 2022,
            puzzleType = PuzzleType.COLLAGE,
            disposition = PuzzleDisposition.KEPT,
            dimensionsText = "20 × 27 in",
            purchasePrice = 18.00,
            tags = listOf("paris", "michael storrings", "city"),
            notes = "Gift from my sister — framed the box art after finishing.",
            isOnLoan = true,
            loanedToDisplayName = "Mom",
            loanedAt = daysAgo(10, now, zoneId),
            dueBackDate = plusDays(7, now, zoneId),
            photos = photos("paris-day-cover", "paris-day-completed"),
        )
    }

    private fun winterInCentralPark(now: Instant, zoneId: ZoneId): Puzzle = completed(
        name = "Winter in Central Park",
        pieces = 500,
        rating = PuzzleRating.FOUR_HALF,
        difficulty = PuzzleDifficulty.TWO,
        completedOn = dateInCurrentYear(1, 15, now, zoneId),
        startedDaysBefore = 2,
        hours = 3,
        minutes = 10,
        zoneId = zoneId,
    ).copy(
        source = "Galison",
        barcode = "012345678909",
        purchaseLocation = "Local bookshop",
        puzzleType = PuzzleType.LANDSCAPE,
        purchasePrice = 16.99,
        tags = listOf("winter", "cozy", "city"),
        photos = photos("bizarre-bookshop-cover"),
    )

    private fun starryNight(now: Instant, zoneId: ZoneId): Puzzle = completed(
        name = "Starry Night",
        pieces = 1000,
        rating = PuzzleRating.FIVE,
        difficulty = PuzzleDifficulty.FOUR,
        completedOn = dateInCurrentYear(2, 10, now, zoneId),
        startedDaysBefore = 4,
        hours = 6,
        minutes = 45,
        zoneId = zoneId,
    ).copy(
        source = "Buffalo Games",
        barcode = "012345678910",
        purchaseLocation = "Target",
        puzzleType = PuzzleType.OTHER,
        purchasePrice = 14.99,
        tags = listOf("art", "classic"),
        photos = photos("canal-cruise-venice-cover"),
    )

    private fun countryCottage(now: Instant, zoneId: ZoneId): Puzzle = completed(
        name = "Country Cottage",
        pieces = 750,
        rating = PuzzleRating.FOUR,
        difficulty = PuzzleDifficulty.THREE,
        completedOn = dateInCurrentYear(3, 22, now, zoneId),
        startedDaysBefore = 6,
        hours = 5,
        minutes = 20,
        zoneId = zoneId,
    ).copy(
        source = "Ravensburger",
        barcode = "012345678911",
        purchaseLocation = "Amazon",
        puzzleType = PuzzleType.LANDSCAPE,
        purchasePrice = 19.99,
        tags = listOf("cozy", "cottage", "spring"),
        photos = photos("venice-romance-cover"),
    )

    private fun canalAtSunset(now: Instant, zoneId: ZoneId): Puzzle = completed(
        name = "Canal at Sunset",
        pieces = 1500,
        rating = PuzzleRating.FOUR_HALF,
        difficulty = PuzzleDifficulty.FOUR,
        completedOn = dateInCurrentYear(4, 25, now, zoneId),
        startedDaysBefore = 10,
        hours = 14,
        minutes = 30,
        zoneId = zoneId,
    ).copy(
        source = "Ravensburger",
        barcode = "012345678912",
        purchaseLocation = "Barnes & Noble",
        puzzleType = PuzzleType.PANORAMIC,
        purchasePrice = 29.99,
        tags = listOf("venice", "sunset", "travel"),
        photos = photos("canal-cruise-venice-cover"),
    )

    private fun yosemiteValley(now: Instant, zoneId: ZoneId): Puzzle = completed(
        name = "Yosemite Valley",
        pieces = 2000,
        rating = PuzzleRating.FIVE,
        difficulty = PuzzleDifficulty.FIVE,
        completedOn = dateInCurrentYear(5, 12, now, zoneId),
        startedDaysBefore = 14,
        hours = 22,
        minutes = 0,
        zoneId = zoneId,
    ).copy(
        source = "Ravensburger",
        barcode = "012345678913",
        purchaseLocation = "Amazon",
        puzzleType = PuzzleType.LANDSCAPE,
        purchasePrice = 34.99,
        tags = listOf("mountains", "nature", "national parks"),
        photos = photos("paris-day-cover"),
    )

    private fun floralArch(now: Instant, zoneId: ZoneId): Puzzle {
        val completedOn = dateInCurrentYear(6, 3, now, zoneId)
        val firstFinish = dateInCurrentYear(2, 20, now, zoneId)
        return completed(
            name = "Floral Arch",
            pieces = 300,
            rating = PuzzleRating.THREE_HALF,
            difficulty = PuzzleDifficulty.ONE,
            completedOn = completedOn,
            startedDaysBefore = 1,
            hours = 2,
            minutes = 0,
            zoneId = zoneId,
        ).copy(
            timesCompleted = 2,
            source = "Galison",
            barcode = "012345678914",
            purchaseLocation = "Target",
            puzzleType = PuzzleType.COLLAGE,
            purchasePrice = 12.99,
            tags = listOf("flowers", "spring", "cozy"),
            photos = photos("paris-day-completed"),
            completions = listOf(
                PuzzleCompletion(completionNumber = 1, completedAt = firstFinish, rating = 3.0),
                PuzzleCompletion(completionNumber = 2, completedAt = completedOn, rating = 3.5),
            ),
        )
    }

    private fun underwaterParadise(now: Instant, zoneId: ZoneId): Puzzle = demo(
        name = "Underwater Paradise",
        pieces = 1000,
        rating = PuzzleRating.NONE,
        difficulty = PuzzleDifficulty.THREE,
    ).copy(
        status = PuzzleStatus.IN_PROGRESS,
        progressPercent = 62,
        source = "Ravensburger",
        barcode = "012345678915",
        purchaseLocation = "Amazon",
        puzzleType = PuzzleType.OTHER,
        purchasePrice = 21.99,
        estimatedTimeSpent = PuzzleTime(6, 0),
        startDate = daysAgo(7, now, zoneId),
        tags = listOf("ocean", "colorful"),
        photos = photos("canal-cruise-venice-cover"),
    )

    private fun gradientLines(): Puzzle = demo(
        name = "Gradient Lines",
        pieces = 500,
        rating = PuzzleRating.ONE,
        difficulty = PuzzleDifficulty.TWO,
    ).copy(
        status = PuzzleStatus.ABANDONED,
        progressPercent = 28,
        source = "Buffalo Games",
        barcode = "012345678916",
        purchaseLocation = "Thrift store",
        puzzleType = PuzzleType.GRADIENT,
        purchasePrice = 4.00,
        hasMissingPieces = true,
        notes = "Two edge pieces missing — donating the rest.",
        tags = listOf("abstract", "gradient"),
        photos = photos("venice-romance-cover"),
    )

    private fun mysteryAmusementPark(): Puzzle = demo(
        name = "Mystery Puzzle: Amusement Park",
        pieces = 1000,
        rating = PuzzleRating.NONE,
        difficulty = PuzzleDifficulty.THREE,
    ).copy(
        status = PuzzleStatus.WISHLIST,
        source = "Ravensburger",
        purchaseLocation = "Amazon",
        puzzleType = PuzzleType.MYSTERY,
        notes = "Save for summer — love the surprise reveal puzzles.",
        tags = listOf("mystery", "summer"),
        photos = photos("bizarre-bookshop-cover"),
    )

    private fun demo(
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
        completionDate = Instant.EPOCH,
        isDemo = true,
        purchaseCurrencyCode = "USD",
        material = PuzzleMaterial.CARDBOARD,
        puzzleShape = PuzzleShape.RECTANGULAR,
        cutType = PuzzleCutType.GRID,
    )

    private fun completed(
        name: String,
        pieces: Int,
        rating: PuzzleRating,
        difficulty: PuzzleDifficulty,
        completedOn: Instant,
        startedDaysBefore: Long,
        hours: Int,
        minutes: Int,
        zoneId: ZoneId,
    ): Puzzle {
        val base = demo(name, pieces, rating, difficulty)
        return base.copy(
            status = PuzzleStatus.COMPLETED,
            progressPercent = 100,
            timesCompleted = 1,
            completionDate = completedOn,
            startDate = completedOn.atZone(zoneId).minusDays(startedDaysBefore).toInstant(),
            estimatedTimeSpent = PuzzleTime(hours, minutes),
            disposition = PuzzleDisposition.KEPT,
            completions = listOf(
                PuzzleCompletion(
                    completionNumber = 1,
                    completedAt = completedOn,
                    rating = if (rating == PuzzleRating.NONE) null else rating.value,
                ),
            ),
        )
    }

    private fun photos(vararg names: String): List<PuzzlePhoto> =
        names.mapIndexed { index, name ->
            PuzzlePhoto(sortOrder = index, fileName = "$name.jpg", createdAt = Instant.EPOCH)
        }

    private fun daysAgo(days: Long, now: Instant, zoneId: ZoneId): Instant =
        now.atZone(zoneId).minusDays(days).toInstant()

    private fun plusDays(days: Long, now: Instant, zoneId: ZoneId): Instant =
        now.atZone(zoneId).plusDays(days).toInstant()

    private fun dateInCurrentYear(month: Int, day: Int, now: Instant, zoneId: ZoneId): Instant {
        val year = now.atZone(zoneId).year
        return LocalDate.of(year, month, day).atStartOfDay(zoneId).toInstant()
    }
}
