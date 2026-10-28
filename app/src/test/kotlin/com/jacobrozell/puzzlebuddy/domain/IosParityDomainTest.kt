package com.jacobrozell.puzzlebuddy.domain

import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionMilestones
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.catalog.DemoDataCatalog
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDateSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListQuery
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListSortOption
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListStatusFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleRandomPicker
import com.jacobrozell.puzzlebuddy.domain.catalog.PurchasePriceFormatting
import com.jacobrozell.puzzlebuddy.domain.completion.CompletionUndoSemantics
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionHistoryCopy
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionSemantics
import com.jacobrozell.puzzlebuddy.domain.loan.FriendSemantics
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleCollectionCollageLayout
import com.jacobrozell.puzzlebuddy.domain.surface.AppLinks
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class IosParityDomainTest {
    private val zone = ZoneOffset.UTC
    private val anchor = Instant.ofEpochSecond(1_800_000_000)

    private fun date(isoDay: String): Instant = Instant.parse("${isoDay}T00:00:00Z")

    private fun puzzle(
        name: String = "Sample",
        status: PuzzleStatus = PuzzleStatus.TODO,
    ) = Puzzle(
        name = name,
        pieces = 500,
        status = status,
        completionDate = anchor,
    )

    @Test
    fun fiveStatusesUseExactRawStrings() {
        assertEquals(
            listOf("Wishlist", "To-Do", "In-Progress", "Completed", "Abandoned"),
            PuzzleStatus.entries.map { it.raw },
        )
        assertEquals(PuzzleStatus.TODO, PuzzleStatus.fromRaw("nope"))
        assertEquals("Wishlist, not yet owned", PuzzleStatus.WISHLIST.accessibilityDescription)
        assertEquals(0, PuzzleProgressSemantics.progressFor(PuzzleStatus.WISHLIST, 40))
        assertEquals(0, PuzzleProgressSemantics.progressFor(PuzzleStatus.ABANDONED, 40))
        assertEquals(100, PuzzleProgressSemantics.progressFor(PuzzleStatus.COMPLETED, 40))
        assertEquals(10, PuzzleProgressSemantics.progressFor(PuzzleStatus.IN_PROGRESS, 0))
        assertEquals(10, PuzzleProgressSemantics.progressFor(PuzzleStatus.IN_PROGRESS, 100))
        assertEquals(42, PuzzleProgressSemantics.progressFor(PuzzleStatus.IN_PROGRESS, 42))
    }

    @Test
    fun statusChangeSetsStartDate() {
        val now = date("2026-06-04")
        val started = puzzle().withStatusChange(PuzzleStatus.TODO, PuzzleStatus.IN_PROGRESS, now)
        assertEquals(now, started.startDate)
        assertEquals(10, started.progressPercent)

        val completed = started.copy(startDate = null, completionDate = date("2026-06-02"))
            .withStatusChange(PuzzleStatus.IN_PROGRESS, PuzzleStatus.COMPLETED, now)
        assertEquals(date("2026-06-02"), completed.startDate)
        assertEquals(100, completed.progressPercent)

        val abandoned = puzzle(status = PuzzleStatus.IN_PROGRESS).copy(startDate = null, completionDate = date("2026-06-01"))
            .withStatusChange(PuzzleStatus.IN_PROGRESS, PuzzleStatus.ABANDONED, now)
        assertEquals(date("2026-06-01"), abandoned.startDate)
        assertEquals(0, abandoned.progressPercent)
    }

    @Test
    fun progressDayLabelsCountCalendarDays() {
        val active = puzzle(status = PuzzleStatus.IN_PROGRESS).copy(
            startDate = date("2026-06-01"),
            completionDate = date("2026-06-01"),
        )
        assertEquals("3 days puzzling", PuzzleDateSemantics.progressDaysLabel(active, zone, date("2026-06-04")))
        val done = puzzle(status = PuzzleStatus.COMPLETED).copy(
            startDate = date("2026-06-01"),
            completionDate = date("2026-06-03"),
        )
        assertEquals("Finished in 2 days", PuzzleDateSemantics.progressDaysLabel(done, zone, date("2026-06-09")))
        assertEquals(0, PuzzleDateSemantics.dayCount(date("2026-06-04"), date("2026-06-01"), zone))
    }

    @Test
    fun priceClampCurrencyAndUnknownEnum() {
        assertEquals(12.5, PurchasePriceFormatting.parse("12.50"))
        assertNull(PurchasePriceFormatting.parse(""))
        assertEquals(999_999.99, PurchasePriceFormatting.parse("1000000"))
        assertNull(PurchasePriceFormatting.parse("-1"))
        assertEquals(12.5, PurchasePriceFormatting.parse("12,50"))
        val prepared = puzzle().copy(
            purchasePrice = 2_000_000.0,
            purchaseCurrencyCode = null,
            dimensionsText = "x".repeat(100),
            loanedToDisplayName = "  Mom  ",
            timerAccumulatedSeconds = -4,
        ).preparedForPersistence("USD")
        assertEquals(999_999.99, prepared.purchasePrice)
        assertEquals("USD", prepared.purchaseCurrencyCode)
        assertEquals(80, prepared.dimensionsText?.length)
        assertEquals("Mom", prepared.loanedToDisplayName)
        assertEquals(0, prepared.timerAccumulatedSeconds)
        assertEquals(PuzzleType.NONE, PuzzleType.fromRaw("nope"))
        assertNull(puzzle().copy(purchasePrice = null, purchaseCurrencyCode = "EUR").preparedForPersistence("USD").purchaseCurrencyCode)
    }

    @Test
    fun photoReorderAndLimit() {
        val photos = listOf(
            PuzzlePhoto(id = "a", sortOrder = 2, fileName = "a.jpg", createdAt = anchor),
            PuzzlePhoto(id = "b", sortOrder = 0, fileName = "b.jpg", createdAt = anchor),
            PuzzlePhoto(id = "c", sortOrder = 1, fileName = "c.jpg", createdAt = anchor),
        )
        val ordered = PuzzlePhotoSemantics.sortedAndNormalized(photos)
        assertEquals(listOf("b", "c", "a"), ordered.map { it.id })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
        assertEquals("b", PuzzlePhotoSemantics.cover(photos)?.id)
        val moved = PuzzlePhotoSemantics.movingPhotoToCover("a", photos)
        assertEquals("a", PuzzlePhotoSemantics.cover(moved)?.id)
        val later = PuzzlePhotoSemantics.movingPhotoOneStep("b", PuzzlePhotoSemantics.MoveDirection.LATER, ordered)
        assertEquals(listOf("c", "b", "a"), PuzzlePhotoSemantics.photosInOrder(later).map { it.id })
        val tooMany = (0 until 7).map { index ->
            PuzzlePhoto(id = "$index", sortOrder = index, fileName = "$index.jpg", createdAt = anchor)
        }
        assertEquals(5, puzzle().copy(photos = tooMany).preparedForPersistence("USD").photos.size)
    }

    @Test
    fun completionSemanticsCopyAndUndoWindow() {
        val finished = puzzle(status = PuzzleStatus.COMPLETED).copy(
            startDate = date("2026-06-01"),
            completionDate = date("2026-06-03"),
            estimatedTimeSpent = PuzzleTime(2, 5),
            rating = PuzzleRating.FOUR,
        )
        val made = PuzzleCompletionSemantics.makeCompletion(finished, 2)
        assertEquals(2, made.completionNumber)
        assertEquals(date("2026-06-01"), made.startedAt)
        assertEquals("2 hr 5 min", made.timeSpentLabel)
        assertEquals(4.0, made.rating)
        val none = PuzzleCompletionSemantics.makeCompletion(finished.copy(rating = PuzzleRating.NONE), 1)
        assertNull(none.rating)
        val renumbered = PuzzleCompletionSemantics.renumbered(
            listOf(
                PuzzleCompletion(id = "late", completionNumber = 1, completedAt = date("2026-06-10")),
                PuzzleCompletion(id = "early", completionNumber = 2, completedAt = date("2026-06-01")),
            ),
        )
        assertEquals(listOf("early", "late"), renumbered.map { it.id })
        assertEquals(listOf(1, 2), renumbered.map { it.completionNumber })
        Locale.setDefault(Locale.US)
        val message = PuzzleCompletionHistoryCopy.deleteDialogMessage(2, date("2026-06-03"), remainingCount = 3, zoneId = zone)
        assertTrue(message.startsWith("Remove completion #2 from "))
        assertTrue(message.contains("Other finishes stay in your history."))
        val last = PuzzleCompletionHistoryCopy.deleteDialogMessage(1, date("2026-06-03"), remainingCount = 1, zoneId = zone)
        assertTrue(last.startsWith("Remove the finish log from "))
        val snapshot = CompletionUndoSemantics.snapshot("p", "c", PuzzleStatus.IN_PROGRESS, 140, anchor)
        assertEquals(100, snapshot.previousProgressPercent)
        assertNotNull(CompletionUndoSemantics.activeSnapshot(snapshot, "p", anchor.plusSeconds(60)))
        assertNull(CompletionUndoSemantics.activeSnapshot(snapshot, "p", anchor.plusSeconds(5 * 60 + 1)))
        assertNull(CompletionUndoSemantics.activeSnapshot(snapshot, "other", anchor))
    }

    @Test
    fun loanOverdueBoundaryAndDisposition() {
        val dueToday = puzzle().copy(
            isOnLoan = true,
            dueBackDate = date("2026-06-04"),
            loanedToDisplayName = " Mom ",
        )
        assertFalse(PuzzleLoanSemantics.isOverdue(dueToday, date("2026-06-04"), zone))
        assertEquals("On loan", PuzzleLoanSemantics.listBadgeTitle(dueToday, date("2026-06-04"), zone))
        assertTrue(PuzzleLoanSemantics.isOverdue(dueToday, date("2026-06-05"), zone))
        assertEquals("Overdue", PuzzleLoanSemantics.listBadgeTitle(dueToday, date("2026-06-05"), zone))
        assertEquals("Mom", FriendSemantics.normalizedDisplayName(" Mom "))
        assertNull(FriendSemantics.normalizedDisplayName("   "))
        assertEquals(80, FriendSemantics.normalizedDisplayName("a".repeat(90))?.length)
        for (disposition in listOf(
            PuzzleDisposition.GIFTED,
            PuzzleDisposition.SOLD,
            PuzzleDisposition.DONATED,
            PuzzleDisposition.TRASHED,
        )) {
            val cleared = PuzzleLoanSemantics.prepareLoanState(
                dueToday.copy(disposition = disposition),
                date("2026-06-04"),
            )
            assertFalse(cleared.isOnLoan)
            assertNull(cleared.loanedToDisplayName)
        }
        val kept = PuzzleLoanSemantics.prepareLoanState(dueToday.copy(loanedAt = null), date("2026-06-04"))
        assertEquals(date("2026-06-04"), kept.loanedAt)
        assertEquals("Mom", kept.loanedToDisplayName)
    }

    @Test
    fun timerSemanticsMatchIosCases() {
        assertEquals(125, PuzzleTimerSemantics.elapsedSeconds(125, null, anchor))
        assertFalse(PuzzleTimerSemantics.isRunning(null, anchor))
        val now = anchor.plusSeconds(90)
        assertEquals(120, PuzzleTimerSemantics.elapsedSeconds(30, anchor, now))
        assertTrue(PuzzleTimerSemantics.isRunning(anchor, now))
        val paused = PuzzleTimerSemantics.paused(
            puzzle().copy(timerRunningSince = anchor),
            anchor.plusSeconds(600),
        )
        assertEquals(600, paused.timerAccumulatedSeconds)
        assertNull(paused.timerRunningSince)
        val earlier = anchor.minusSeconds(300)
        assertTrue(PuzzleTimerSemantics.isClockSkewed(anchor, earlier))
        assertEquals(45, PuzzleTimerSemantics.elapsedSeconds(45, anchor, earlier))
        assertEquals(0, PuzzleTimerSemantics.elapsedSeconds(-10, null, earlier))
        assertNull(PuzzleTimerSemantics.roundedMinutes(0))
        assertEquals(1, PuzzleTimerSemantics.roundedMinutes(29))
        assertEquals(1, PuzzleTimerSemantics.roundedMinutes(89))
        assertEquals(2, PuzzleTimerSemantics.roundedMinutes(90))
        assertEquals(134, PuzzleTimerSemantics.roundedMinutes(8_040))
        assertEquals(PuzzleTime(2, 14), PuzzleTimerSemantics.puzzleTime(8_040))
        assertEquals(1, PuzzleTimerSemantics.completionPrefill(3_600, null)?.hours)
        assertEquals(1, PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime())?.hours)
        assertNull(PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime(hours = 3)))
        assertNull(PuzzleTimerSemantics.completionPrefill(3_600, PuzzleTime(minutes = 5)))
        assertNull(PuzzleTimerSemantics.formSuggestion(8_040, PuzzleTime(2, 14)))
        assertEquals(14, PuzzleTimerSemantics.formSuggestion(8_040, PuzzleTime(2, 0))?.minutes)
        val first = puzzle("A").copy(id = "a", timerRunningSince = anchor)
        val second = puzzle("B").copy(id = "b")
        assertEquals(listOf("a"), PuzzleTimerSemantics.idsToPause("b", listOf(first, second, puzzle("C").copy(id = "c"))))
        assertEquals("0:00:00", PuzzleTimerSemantics.clockLabel(0))
        assertEquals("2:14:05", PuzzleTimerSemantics.clockLabel(8_045))
        assertEquals("25:01:01", PuzzleTimerSemantics.clockLabel(90_061))
        assertEquals("0 seconds", PuzzleTimerSemantics.spokenLabel(0))
        assertEquals("2 hours, 14 minutes, 5 seconds", PuzzleTimerSemantics.spokenLabel(8_045))
        assertEquals("under_15m", PuzzleTimerSemantics.durationBucket(0))
        assertEquals("15m_1h", PuzzleTimerSemantics.durationBucket(900))
        assertEquals("1h_3h", PuzzleTimerSemantics.durationBucket(3_600))
        assertEquals("over_3h", PuzzleTimerSemantics.durationBucket(10_800))
    }

    @Test
    fun listFiltersOnLoanOverdueAndSearch() {
        val loaned = puzzle("Paris").copy(
            isOnLoan = true,
            loanedToDisplayName = "Mom",
            dueBackDate = date("2026-06-01"),
            purchaseLocation = "Target",
            releaseYear = 2019,
            puzzleType = PuzzleType.COLLAGE,
        )
        val shelf = puzzle("Shelf")
        val now = date("2026-06-04")
        val overdue = PuzzleListQuery.apply(
            puzzles = listOf(loaned, shelf),
            statusFilter = PuzzleListStatusFilter.ALL,
            searchText = "",
            sortOption = PuzzleListSortOption.NAME,
            overdueOnly = true,
            now = now,
            zoneId = zone,
        )
        assertEquals(listOf("Paris"), overdue.map { it.name })
        assertEquals(listOf("Paris"), PuzzleListQuery.search(listOf(loaned, shelf), "mom").map { it.name })
        assertEquals(listOf("Paris"), PuzzleListQuery.search(listOf(loaned, shelf), "2019").map { it.name })
        assertEquals(listOf("Paris"), PuzzleListQuery.search(listOf(loaned, shelf), "collage").map { it.name })
        assertTrue(
            PuzzleListQuery.hasSecondaryFilters(
                searchText = "",
                missingPiecesOnly = false,
                overdueOnly = true,
            ),
        )
        assertEquals(PuzzleListPieceCountFilter.ANY, PuzzleListPieceCountFilter.ANY)
    }

    @Test
    fun statsCountEachFinishAndOlderYears() {
        val twice = puzzle(status = PuzzleStatus.COMPLETED).copy(
            pieces = 1000,
            completions = listOf(
                PuzzleCompletion(completionNumber = 1, completedAt = date("2025-11-02")),
                PuzzleCompletion(completionNumber = 2, completedAt = date("2026-03-15")),
            ),
            completionDate = date("2026-03-15"),
            timesCompleted = 2,
        )
        val months = CollectionStats.monthlyCompletionCounts(listOf(twice), 2026, zone)
        assertEquals(1, months[2])
        assertEquals(0, months[10])
        val priorYear = CollectionStats.monthlyCompletionCounts(listOf(twice), 2025, zone)
        assertEquals(1, priorYear[10])
        assertEquals(listOf(2026, 2025), CollectionStats.yearsWithCompletions(listOf(twice), zone))
        val edited = twice.copy(
            completions = listOf(
                twice.completions[0].copy(completedAt = date("2026-01-02")),
                twice.completions[1],
            ),
        )
        val moved = CollectionStats.monthlyCompletionCounts(listOf(edited), 2026, zone)
        assertEquals(1, moved[0])
        assertEquals(1, moved[2])
        assertEquals(0, CollectionStats.monthlyCompletionCounts(listOf(edited), 2025, zone)[10])
        val stats = CollectionStats.compute(listOf(twice), zone, date("2026-06-01"))
        assertEquals(1, stats.completionsThisYear)
        val sameYear = twice.copy(
            completions = listOf(
                PuzzleCompletion(completionNumber = 1, completedAt = date("2026-01-02")),
                PuzzleCompletion(completionNumber = 2, completedAt = date("2026-03-15")),
            ),
        )
        assertEquals(2, CollectionStats.compute(listOf(sameYear), zone, date("2026-06-01")).completionsThisYear)
        val earned = CollectionMilestones.earned(stats.copy(completedCount = 1, totalMinutesPuzzling = 6_000, totalPiecesCompleted = 10_000, completionsThisYear = 12))
        assertTrue(earned.map { it.id }.containsAll(listOf("completed_1", "pieces_10000", "hours_100", "year_completions_12")))
        assertTrue(CollectionMilestones.newlyEarned(stats.copy(completedCount = 1), setOf("completed_1")).none { it.id == "completed_1" })
    }

    @Test
    fun pickNextSkipsOnLoanAndShareGrid() {
        val loaned = puzzle("Paris").copy(id = "loan", status = PuzzleStatus.TODO, isOnLoan = true, pieces = 1000)
        val todo = puzzle("Shelf").copy(id = "shelf", pieces = 1000)
        val eligible = PuzzleRandomPicker.eligible(
            listOf(loaned, todo, puzzle("Done").copy(status = PuzzleStatus.COMPLETED, pieces = 1000)),
            includeInProgress = false,
            pieceCountFilter = PuzzleListPieceCountFilter.ANY,
            tagFilter = null,
        )
        assertEquals(listOf("shelf"), eligible.map { it.id })
        assertEquals(AppLinks.shareFooterLabel, "jacobrozell.github.io/PuzzleBuddy")
        assertEquals(3 to 2, PuzzleCollectionCollageLayout.gridDimensions(6).let { it.columns to it.rows })
    }

    @Test
    fun demoCatalogMatchesReviewFixtures() {
        val now = date("2026-06-20")
        val shelf = DemoDataCatalog.make(now, zone)
        assertEquals(13, shelf.puzzles.size)
        assertTrue(shelf.puzzles.all { it.isDemo })
        val paris = shelf.puzzles.first { it.name == "Paris in a Day" }
        assertTrue(paris.isOnLoan)
        assertEquals("Mom", paris.loanedToDisplayName)
        assertEquals(listOf("paris-day-cover", "paris-day-completed"), shelf.demoPhotos[paris.id])
        val floral = shelf.puzzles.first { it.name == "Floral Arch" }
        assertEquals(2, floral.completions.size)
        assertEquals(2, floral.timesCompleted)
        assertTrue(shelf.puzzles.any { it.name == "Venice Romance" && it.status == PuzzleStatus.IN_PROGRESS })
        assertTrue(shelf.puzzles.any { it.name == "Underwater Paradise" && it.status == PuzzleStatus.IN_PROGRESS })
    }
}
