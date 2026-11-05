package com.jacobrozell.puzzlebuddy.data.repository

import com.jacobrozell.puzzlebuddy.domain.completion.CompletionUndoSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsMilestones
import com.jacobrozell.puzzlebuddy.support.logging.InMemoryAnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.LogEntry
import com.jacobrozell.puzzlebuddy.support.logging.RecordingAppLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Store behavior ported from iOS `PuzzleStoreTests`, `PuzzleExpandedFeatureTests`,
 * `PuzzleReviewFixTests`, `PuzzleLoanAndFriendTests`, and `PuzzleTimerStoreTests` at tag 1.1.0.
 */
class PuzzleRepositoryTest {
    private val start = Instant.parse("2026-06-15T12:00:00Z")
    private lateinit var db: InMemoryPuzzleDatabase
    private lateinit var clock: MutableClock
    private lateinit var logger: RecordingAppLogger
    private lateinit var photoStore: InMemoryPhotoFileStore
    private lateinit var repo: PuzzleRepository

    @Before
    fun setUp() {
        db = InMemoryPuzzleDatabase()
        clock = MutableClock(start)
        logger = RecordingAppLogger()
        photoStore = InMemoryPhotoFileStore()
        repo = PuzzleRepository(
            puzzleDao = db.puzzleDao,
            photoDao = db.photoDao,
            completionDao = db.completionDao,
            friendDao = db.friendDao,
            friendRepository = FriendRepository(db.friendDao, logger, clock),
            transactions = db.transactions,
            photoStore = photoStore,
            demoAssets = DemoPhotoSource { name -> "demo:$name".toByteArray() },
            clock = clock,
            logger = logger,
            barcodeMetadataCache = RecordingBarcodeRecorder(),
            analyticsMilestones = AnalyticsMilestones(InMemoryAnalyticsKeyValueStore(), logger),
            analyticsUserContext = RecordingCollectionAnalytics(),
        )
    }

    // region Helpers

    private fun fixture(
        name: String,
        status: PuzzleStatus = PuzzleStatus.TODO,
        pieces: Int = 500,
    ) = Puzzle(name = name, pieces = pieces, status = status, completionDate = clock.now)

    private suspend fun add(puzzle: Puzzle): String {
        repo.add(puzzle)
        return puzzle.id
    }

    private suspend fun addInProgress(name: String, time: PuzzleTime? = null): String =
        add(fixture(name, PuzzleStatus.IN_PROGRESS).copy(estimatedTimeSpent = time))

    private suspend fun load(id: String): Puzzle = requireNotNull(repo.findById(id)) { "Missing puzzle $id" }

    private fun events(name: String): List<LogEntry> = logger.entries.filter { it.eventName == name }

    private suspend inline fun <reified T : Throwable> assertThrows(block: suspend () -> Unit): T {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return error
            fail("Expected ${T::class.simpleName}, got $error")
        }
        fail("Expected ${T::class.simpleName}, nothing was thrown")
        throw AssertionError()
    }

    private fun epoch(seconds: Long): Instant = Instant.ofEpochSecond(seconds)

    // endregion

    // region Add, update, delete, duplicates (PuzzleStoreTests)

    @Test
    fun addAndUpdatePersist() = runTest {
        val id = add(fixture("Shelf").copy(notes = "Box dented"))
        assertEquals("Box dented", load(id).notes)

        repo.update(load(id).copy(notes = "Fine", pieces = 1000))

        val reloaded = repo.allPuzzles().single()
        assertEquals("Fine", reloaded.notes)
        assertEquals(1000, reloaded.pieces)
        assertEquals(1, events("puzzle_added").size)
        assertEquals(1, events("puzzle_updated").size)
    }

    @Test
    fun updateThrowsRecordNotFoundWhenRowMissing() = runTest {
        assertThrows<PuzzleStoreException.RecordNotFound> { repo.update(fixture("Ghost")) }
    }

    @Test
    fun addRejectsDuplicateBarcode() = runTest {
        add(fixture("First").copy(barcode = "036000291452"))
        val error = assertThrows<PuzzleStoreException.DuplicateBarcode> {
            repo.add(fixture("Second").copy(barcode = " 0360-0029-1452 "))
        }
        assertEquals("First", error.existingPuzzleName)
        assertEquals(1, repo.allPuzzles().size)
    }

    @Test
    fun updateRejectsDuplicateBarcode() = runTest {
        add(fixture("First").copy(barcode = "036000291452"))
        val second = add(fixture("Second").copy(barcode = "012345678905"))
        assertThrows<PuzzleStoreException.DuplicateBarcode> {
            repo.update(load(second).copy(barcode = "036000291452"))
        }
        assertEquals("012345678905", load(second).barcode)
    }

    @Test
    fun importCountsDuplicatesAndEmptyNames() = runTest {
        add(fixture("Owned").copy(barcode = "036000291452"))
        val summary = repo.importPuzzles(
            listOf(
                fixture("  New  "),
                fixture("Copy").copy(barcode = "036000291452"),
                fixture("   "),
            ),
        )
        assertEquals(1, summary.imported)
        assertEquals(1, summary.skippedDuplicates)
        assertEquals(1, summary.skippedInvalid)
        assertEquals(listOf("New", "Owned"), repo.allPuzzles().map { it.name })
    }

    @Test
    fun deleteRemovesPhotosCompletionsAndFiles() = runTest {
        val photo = PuzzlePhoto(sortOrder = 0, createdAt = clock.now)
        val puzzle = fixture("Gone", PuzzleStatus.COMPLETED).copy(photos = listOf(photo))
        repo.add(puzzle, pendingPhotoBytes = mapOf(photo.id to byteArrayOf(1, 2)))
        assertEquals(1, photoStore.files.size)
        assertEquals(1, db.completions.size)

        repo.delete(puzzle.id)

        assertTrue(db.puzzles.isEmpty())
        assertTrue(db.photos.isEmpty())
        assertTrue(db.completions.isEmpty())
        assertTrue(photoStore.files.isEmpty())
        assertEquals(1, events("puzzle_deleted").size)
    }

    // endregion

    // region Photos (PuzzleExpandedFeatureTests)

    @Test
    fun updatingPhotosReplacesRowsAndDeletesDroppedFiles() = runTest {
        val first = PuzzlePhoto(sortOrder = 0, createdAt = clock.now)
        val puzzle = fixture("Gallery").copy(photos = listOf(first))
        repo.add(puzzle, pendingPhotoBytes = mapOf(first.id to byteArrayOf(1)))
        val oldFile = load(puzzle.id).photos.single().fileName
        assertNotNull(oldFile)

        val replacement = PuzzlePhoto(sortOrder = 0, createdAt = clock.now)
        repo.update(load(puzzle.id).copy(photos = listOf(replacement)), mapOf(replacement.id to byteArrayOf(2)))

        val photos = load(puzzle.id).photos
        assertEquals(listOf(replacement.id), photos.map { it.id })
        assertFalse(photoStore.files.containsKey(oldFile))
        assertTrue(photoStore.files.containsKey(photos.single().fileName))
        assertEquals(1, db.photos.size)
    }

    @Test
    fun failedAddDeletesPhotoFilesItWrote() = runTest {
        add(fixture("Original").copy(barcode = "036000291452"))
        val photo = PuzzlePhoto(sortOrder = 0, createdAt = clock.now)
        assertThrows<PuzzleStoreException.DuplicateBarcode> {
            repo.add(
                fixture("Copy").copy(barcode = "036000291452", photos = listOf(photo)),
                pendingPhotoBytes = mapOf(photo.id to byteArrayOf(9)),
            )
        }
        assertTrue(photoStore.files.isEmpty())
        assertTrue(db.photos.isEmpty())
    }

    // endregion

    // region Completion history, redo, undo (PuzzleExpandedFeatureTests)

    @Test
    fun addingCompletedPuzzleLogsOneFinishWithoutUndo() = runTest {
        val id = add(fixture("Already done", PuzzleStatus.COMPLETED))
        val loaded = load(id)
        assertEquals(1, loaded.timesCompleted)
        assertEquals(1, loaded.completions.size)
        assertNull(repo.activeCompletionUndo(id))
        assertEquals("1", events("puzzle_completion_recorded").single().metadata["completion_number"])
    }

    @Test
    fun redoIncrementsCompletionCount() = runTest {
        val id = add(fixture("Repeat", PuzzleStatus.COMPLETED, pieces = 300))

        repo.startRedo(id)
        var loaded = load(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, loaded.status)
        assertEquals(0, loaded.progressPercent)
        assertNotNull(loaded.startDate)
        assertEquals(1, loaded.timesCompleted)
        assertEquals("1", events("puzzle_redo_started").single().metadata["completion_count"])

        clock.advanceSeconds(60)
        repo.update(loaded.copy(status = PuzzleStatus.COMPLETED, completionDate = clock.now))
        loaded = load(id)
        assertEquals(2, loaded.timesCompleted)
        assertEquals(listOf(1, 2), loaded.completions.map { it.completionNumber })
        assertEquals("2", events("puzzle_completion_recorded").last().metadata["completion_number"])
    }

    @Test
    fun startRedoPreservesLastCompletionDate() = runTest {
        val finishedAt = epoch(1_700_000_000)
        val id = add(fixture("KeepDate", PuzzleStatus.COMPLETED).copy(completionDate = finishedAt))

        repo.startRedo(id)

        val loaded = load(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, loaded.status)
        assertEquals(finishedAt, loaded.completionDate)
        assertNotEquals(finishedAt, loaded.startDate)
    }

    @Test
    fun startRedoIgnoresNonCompletedPuzzle() = runTest {
        val id = addInProgress("Active")
        repo.startRedo(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, load(id).status)
        assertEquals(0, load(id).timesCompleted)
        assertTrue(events("puzzle_redo_started").isEmpty())
    }

    @Test
    fun updateWithoutStatusChangeDoesNotAddCompletion() = runTest {
        val id = add(fixture("Stable", PuzzleStatus.COMPLETED))
        repo.update(load(id).copy(notes = "Still done"))
        assertEquals(1, load(id).timesCompleted)
        assertEquals(1, load(id).completions.size)
    }

    @Test
    fun completionRecordCapturesTimeSpentAndRating() = runTest {
        val id = add(
            fixture("Timed", PuzzleStatus.COMPLETED).copy(
                estimatedTimeSpent = PuzzleTime(hours = 4, minutes = 15),
                rating = PuzzleRating.FIVE,
            ),
        )
        val completion = load(id).completions.single()
        assertEquals(4, completion.timeSpentHours)
        assertEquals(15, completion.timeSpentMinutes)
        assertEquals(5.0, completion.rating)
    }

    @Test
    fun deleteMiddleCompletionRenumbers() = runTest {
        val id = add(fixture("Triple", PuzzleStatus.COMPLETED).copy(completionDate = epoch(1_000)))
        repo.update(load(id).copy(status = PuzzleStatus.IN_PROGRESS))
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, completionDate = epoch(2_000)))
        repo.startRedo(id)
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, completionDate = epoch(3_000)))
        val middle = load(id).completions.first { it.completionNumber == 2 }

        repo.deleteCompletion(id, middle.id)

        val loaded = load(id)
        assertEquals(2, loaded.timesCompleted)
        assertEquals(listOf(1, 2), loaded.completions.map { it.completionNumber })
        assertEquals(listOf(epoch(1_000), epoch(3_000)), loaded.completions.map { it.completedAt })
        assertEquals("2", events("puzzle_completion_deleted").single().metadata["completion_number"])
    }

    @Test
    fun deleteLastCompletionRevertsStatus() = runTest {
        val id = add(fixture("Oops", PuzzleStatus.COMPLETED, pieces = 300))
        val completion = load(id).completions.single()

        repo.deleteCompletion(id, completion.id, statusIfRemovingLast = PuzzleStatus.IN_PROGRESS)

        val updated = load(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, updated.status)
        assertEquals(0, updated.timesCompleted)
        assertTrue(updated.completions.isEmpty())
    }

    @Test
    fun deleteLastCompletionWithoutStatusThrows() = runTest {
        val id = add(fixture("Needs status", PuzzleStatus.COMPLETED))
        val completion = load(id).completions.single()

        assertThrows<PuzzleStoreException.StatusRequiredAfterRemovingLastCompletion> {
            repo.deleteCompletion(id, completion.id)
        }
        assertEquals(1, load(id).timesCompleted)
        assertEquals(PuzzleStatus.COMPLETED, load(id).status)
    }

    @Test
    fun undoLastCompletionRestoresPreviousStatusAndProgress() = runTest {
        val id = add(fixture("Accidental", PuzzleStatus.IN_PROGRESS).copy(progressPercent = 45))
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, progressPercent = 100))
        assertEquals(1, load(id).timesCompleted)
        assertNotNull(repo.activeCompletionUndo(id))

        repo.undoLastCompletion(id)

        val loaded = load(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, loaded.status)
        assertEquals(45, loaded.progressPercent)
        assertEquals(0, loaded.timesCompleted)
        assertTrue(loaded.completions.isEmpty())
        assertNull(repo.activeCompletionUndo(id))
        assertEquals("1", events("puzzle_completion_undone").single().metadata["completion_number"])
        assertTrue(events("puzzle_completion_deleted").isEmpty())
    }

    @Test
    fun expiredUndoIsUnavailable() = runTest {
        val id = add(fixture("Too late", PuzzleStatus.IN_PROGRESS).copy(progressPercent = 20))
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))
        clock.advanceSeconds(CompletionUndoSemantics.WINDOW.seconds + 5)

        assertThrows<PuzzleStoreException.CompletionUndoUnavailable> { repo.undoLastCompletion(id) }

        assertEquals(PuzzleStatus.COMPLETED, load(id).status)
        assertNull(repo.pendingCompletionUndo.value)
    }

    @Test
    fun failedCompleteSaveRollsBackAndLeavesNoUndo() = runTest {
        val id = add(fixture("Fail save", PuzzleStatus.IN_PROGRESS).copy(progressPercent = 40))
        db.failNextCompletionWrite = true

        assertThrows<IllegalStateException> {
            repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, progressPercent = 100))
        }

        val loaded = load(id)
        assertNull(repo.activeCompletionUndo(id))
        assertEquals(PuzzleStatus.IN_PROGRESS, loaded.status)
        assertEquals(40, loaded.progressPercent)
        assertEquals(0, loaded.timesCompleted)
        assertTrue(events("puzzle_completion_recorded").isEmpty())
    }

    @Test
    fun undoSecondCompletionKeepsEarlierHistory() = runTest {
        val id = add(fixture("Replay undo", PuzzleStatus.COMPLETED, pieces = 300))
        repo.startRedo(id)
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))
        assertEquals(2, load(id).completions.size)

        repo.undoLastCompletion(id)

        val loaded = load(id)
        assertEquals(PuzzleStatus.IN_PROGRESS, loaded.status)
        assertEquals(0, loaded.progressPercent)
        assertEquals(1, loaded.timesCompleted)
        assertEquals(1, loaded.completions.size)
    }

    @Test
    fun updateCompletionChangesDateAndPersists() = runTest {
        val id = add(fixture("Edit me", PuzzleStatus.COMPLETED).copy(completionDate = epoch(1_000)))
        val completion = load(id).completions.single()

        repo.updateCompletion(id, completion.copy(completedAt = epoch(9_000)))

        val loaded = load(id)
        assertEquals(epoch(9_000), loaded.completionDate)
        assertEquals(epoch(9_000), loaded.completions.single().completedAt)
        assertEquals(1, db.completions.values.count { it.puzzleId == id })
        assertEquals("1", events("puzzle_completion_updated").single().metadata["completion_number"])
    }

    // endregion

    // region Review fixes (PuzzleReviewFixTests)

    @Test
    fun lastCompletionDeleteToInProgressClearsHundredPercent() = runTest {
        val id = add(fixture("Last finish", PuzzleStatus.COMPLETED).copy(progressPercent = 100))
        val completion = load(id).completions.single()

        repo.deleteCompletion(id, completion.id, statusIfRemovingLast = PuzzleStatus.IN_PROGRESS)

        assertEquals(PuzzleStatus.IN_PROGRESS, load(id).status)
        assertTrue(load(id).progressPercent < 100)
    }

    @Test
    fun lastCompletionDeleteToTodo() = runTest {
        val id = add(fixture("Back to shelf", PuzzleStatus.COMPLETED).copy(progressPercent = 100))
        val completion = load(id).completions.single()

        repo.deleteCompletion(id, completion.id, statusIfRemovingLast = PuzzleStatus.TODO)

        val loaded = load(id)
        assertEquals(PuzzleStatus.TODO, loaded.status)
        assertEquals(0, loaded.progressPercent)
        assertTrue(loaded.completions.isEmpty())
    }

    @Test
    fun completionFetchFailureLeavesHistoryAlone() = runTest {
        val id = add(fixture("Keep count", PuzzleStatus.COMPLETED))
        repo.startRedo(id)
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))
        val newest = load(id).completions.last()
        db.failNextCompletionRead = true

        val error = assertThrows<PuzzleStoreException.CompletionHistoryUnavailable> {
            repo.deleteCompletion(id, newest.id)
        }

        assertEquals("Could not load completion history. Your collection was left unchanged.", error.message)
        assertEquals(2, load(id).timesCompleted)
        assertEquals(2, load(id).completions.size)
    }

    @Test
    fun deleteNewestCompletionKeepsRemainingDate() = runTest {
        val id = add(fixture("Two finishes", PuzzleStatus.COMPLETED).copy(completionDate = epoch(1_000)))
        repo.startRedo(id)
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, completionDate = epoch(2_000)))
        val newest = load(id).completions.maxBy { it.completedAt }

        repo.deleteCompletion(id, newest.id)

        val refreshed = load(id)
        assertEquals(1, refreshed.completions.size)
        assertEquals(epoch(1_000), refreshed.completionDate)
    }

    @Test
    fun editCompletionDateRenumbersAndPersistsTime() = runTest {
        val id = add(fixture("Reorder", PuzzleStatus.COMPLETED).copy(completionDate = epoch(1_000)))
        repo.startRedo(id)
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED, completionDate = epoch(2_000)))
        val older = load(id).completions.minBy { it.completedAt }

        repo.updateCompletion(
            id,
            older.copy(completedAt = epoch(3_000), timeSpentHours = 2, timeSpentMinutes = 15, rating = 4.0),
        )

        val refreshed = load(id)
        val newest = refreshed.completions.maxBy { it.completedAt }
        assertEquals(older.id, newest.id)
        assertEquals(2, newest.completionNumber)
        assertEquals(2, newest.timeSpentHours)
        assertEquals(15, newest.timeSpentMinutes)
        assertEquals(4.0, newest.rating)
        assertEquals(epoch(3_000), refreshed.completionDate)
    }

    @Test
    fun soldDonatedTrashedClearLoan() = runTest {
        for (disposition in listOf(PuzzleDisposition.SOLD, PuzzleDisposition.DONATED, PuzzleDisposition.TRASHED)) {
            val id = add(fixture(disposition.raw).copy(isOnLoan = true, loanedToDisplayName = "Sam"))
            assertTrue(load(id).isOnLoan)

            repo.update(load(id).copy(disposition = disposition))

            val refreshed = load(id)
            assertFalse(disposition.raw, refreshed.isOnLoan)
            assertNull(disposition.raw, refreshed.loanedToFriendId)
        }
    }

    @Test
    fun failedPuzzleSaveDoesNotLeaveNewFriend() = runTest {
        db.failNextPuzzleWrite = true

        assertThrows<IllegalStateException> {
            repo.add(fixture("Loaned").copy(isOnLoan = true, loanedToDisplayName = "Blake"))
        }

        assertTrue(db.friends.isEmpty())
        assertTrue(db.puzzles.isEmpty())
    }

    // endregion

    // region Loans and friends (PuzzleLoanAndFriendTests)

    @Test
    fun markOnLoanReusesFriendCaseInsensitively() = runTest {
        val first = add(fixture("One").copy(isOnLoan = true, loanedToDisplayName = "  mom "))
        val second = add(fixture("Two"))

        repo.update(load(second).copy(isOnLoan = true, loanedToDisplayName = "MOM"))

        assertEquals(1, db.friends.size)
        assertEquals(load(first).loanedToFriendId, load(second).loanedToFriendId)
        assertEquals("mom", load(second).loanedToDisplayName)
        assertNotNull(load(second).loanedAt)
        assertEquals(1, events("friend_created").size)
        assertEquals(1, events("puzzle_marked_on_loan").size)
    }

    @Test
    fun markReturnedClearsLoanAndKeepsFriend() = runTest {
        val id = add(
            fixture("Borrowed").copy(isOnLoan = true, loanedToDisplayName = "Mom", dueBackDate = epoch(2_000_000_000)),
        )

        repo.markReturned(id)

        val loaded = load(id)
        assertFalse(loaded.isOnLoan)
        assertNull(loaded.loanedToFriendId)
        assertNull(loaded.dueBackDate)
        assertEquals(1, db.friends.size)
        assertEquals(1, events("puzzle_marked_returned").size)
    }

    @Test
    fun clearAllDeletesFriendsPhotosAndFiles() = runTest {
        val photo = PuzzlePhoto(sortOrder = 0, createdAt = clock.now)
        repo.add(
            fixture("Everything", PuzzleStatus.COMPLETED).copy(
                photos = listOf(photo),
                isOnLoan = true,
                loanedToDisplayName = "Mom",
            ),
            pendingPhotoBytes = mapOf(photo.id to byteArrayOf(1)),
        )

        repo.clearAll()

        assertTrue(db.puzzles.isEmpty())
        assertTrue(db.photos.isEmpty())
        assertTrue(db.completions.isEmpty())
        assertTrue(db.friends.isEmpty())
        assertTrue(photoStore.files.isEmpty())
        assertEquals(1, events("puzzle_collection_cleared").size)
    }

    // endregion

    // region Demo data (PuzzleStoreTests, PuzzleReviewFixTests)

    @Test
    fun loadDemoPuzzlesMarksDemoAndWritesCovers() = runTest {
        repo.loadDemoPuzzles()

        val puzzles = repo.allPuzzles()
        assertEquals(13, puzzles.size)
        assertTrue(puzzles.all { it.isDemo })
        val paris = puzzles.first { it.name == "Paris in a Day" }
        assertTrue(paris.isOnLoan)
        assertEquals("Mom", paris.loanedToDisplayName)
        assertEquals(2, paris.photos.size)
        assertTrue(paris.photos.all { photoStore.files.containsKey(it.fileName) })
        val floral = puzzles.first { it.name == "Floral Arch" }
        assertEquals(2, floral.completions.size)
        assertEquals("13", events("demo_data_loaded").single().metadata["puzzle_count"])
    }

    @Test
    fun removeDemoPuzzlesKeepsUserPuzzlesAndTheirLoanFriend() = runTest {
        repo.loadDemoPuzzles()
        val mine = add(fixture("Mine").copy(isOnLoan = true, loanedToDisplayName = "Mom"))
        val friendId = load(mine).loanedToFriendId

        repo.removeDemoPuzzles()

        assertEquals(listOf("Mine"), repo.allPuzzles().map { it.name })
        assertTrue(db.friends.containsKey(friendId))
        assertTrue(photoStore.files.isEmpty())
        assertEquals("13", events("demo_data_removed").single().metadata["removed_count"])
    }

    @Test
    fun removeDemoPuzzlesIsNoOpWhenNonePresent() = runTest {
        add(fixture("Mine"))
        repo.removeDemoPuzzles()
        assertEquals(1, repo.allPuzzles().size)
        assertEquals("0", events("demo_data_removed").single().metadata["removed_count"])
    }

    // endregion

    // region Timer (PuzzleTimerStoreTests)

    @Test
    fun startPersistsAnchor() = runTest {
        val id = addInProgress("Anchor")
        val started = clock.now.minusSeconds(600)

        repo.startTimer(id, now = started)

        assertEquals(started, load(id).timerRunningSince)
        assertEquals(0, load(id).timerAccumulatedSeconds)
        assertEquals(1, events("puzzle_timer_started").size)
    }

    @Test
    fun startingSecondPuzzlePausesFirst() = runTest {
        val first = addInProgress("First")
        val second = addInProgress("Second")
        val begin = clock.now.minusSeconds(1_000)

        repo.startTimer(first, now = begin)
        repo.startTimer(second, now = begin.plusSeconds(300))

        assertNull(load(first).timerRunningSince)
        assertEquals(300, load(first).timerAccumulatedSeconds)
        assertEquals(begin.plusSeconds(300), load(second).timerRunningSince)
        assertEquals("other_started", events("puzzle_timer_paused").single().metadata["reason"])
    }

    @Test
    fun pauseAndResumeAccumulate() = runTest {
        val id = addInProgress("Resume")
        val begin = clock.now.minusSeconds(1_000)

        repo.startTimer(id, now = begin)
        repo.pauseTimer(id, now = begin.plusSeconds(120))
        repo.startTimer(id, now = begin.plusSeconds(500))
        repo.pauseTimer(id, now = begin.plusSeconds(530))

        assertEquals(150, load(id).timerAccumulatedSeconds)
        assertNull(load(id).timerRunningSince)
    }

    @Test
    fun startIgnoredWhenNotInProgress() = runTest {
        val id = add(fixture("Shelf"))
        repo.startTimer(id)
        assertNull(load(id).timerRunningSince)
        assertTrue(events("puzzle_timer_started").isEmpty())
    }

    @Test
    fun leavingInProgressPauses() = runTest {
        val id = addInProgress("Leave")
        repo.startTimer(id, now = clock.now.minusSeconds(120))

        repo.update(load(id).copy(status = PuzzleStatus.TODO))

        assertNull(load(id).timerRunningSince)
        assertEquals(120, load(id).timerAccumulatedSeconds)
        assertEquals("status_left", events("puzzle_timer_paused").single().metadata["reason"])
    }

    @Test
    fun completeWithEmptyLoggedTimeWritesTimerTime() = runTest {
        val id = addInProgress("Prefill")
        repo.startTimer(id, now = clock.now.minusSeconds(8_040))

        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))

        val completed = load(id)
        assertNull(completed.timerRunningSince)
        assertEquals(2, completed.estimatedTimeSpent?.hours)
        assertEquals(14, completed.estimatedTimeSpent?.minutes)
        assertEquals(2, completed.completions.single().timeSpentHours)
        assertEquals(14, completed.completions.single().timeSpentMinutes)
    }

    @Test
    fun completeKeepsExistingLoggedTime() = runTest {
        val id = addInProgress("Logged")
        repo.startTimer(id, now = clock.now.minusSeconds(600))

        repo.update(load(id).copy(estimatedTimeSpent = PuzzleTime(hours = 5), status = PuzzleStatus.COMPLETED))

        val completed = load(id)
        assertEquals(5, completed.estimatedTimeSpent?.hours)
        assertNull(completed.estimatedTimeSpent?.minutes)
        assertEquals(5, completed.completions.single().timeSpentHours)
    }

    @Test
    fun redoZeroesTimer() = runTest {
        val id = addInProgress("Redo")
        repo.startTimer(id, now = clock.now.minusSeconds(600))
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))
        assertTrue(load(id).timerAccumulatedSeconds > 0)

        repo.startRedo(id)

        assertEquals(PuzzleStatus.IN_PROGRESS, load(id).status)
        assertEquals(0, load(id).timerAccumulatedSeconds)
        assertNull(load(id).timerRunningSince)
    }

    @Test
    fun undoCompletionKeepsAccumulatedSeconds() = runTest {
        val id = addInProgress("Undo")
        repo.startTimer(id, now = clock.now.minusSeconds(600))
        repo.update(load(id).copy(status = PuzzleStatus.COMPLETED))
        val accumulated = load(id).timerAccumulatedSeconds

        repo.undoLastCompletion(id)

        assertEquals(PuzzleStatus.IN_PROGRESS, load(id).status)
        assertEquals(accumulated, load(id).timerAccumulatedSeconds)
        assertNull(load(id).timerRunningSince)
    }

    @Test
    fun resetZeroesTimerAndKeepsLoggedTime() = runTest {
        val id = addInProgress("Reset", time = PuzzleTime(hours = 1, minutes = 5))
        repo.startTimer(id, now = clock.now.minusSeconds(600))

        repo.resetTimer(id)

        val loaded = load(id)
        assertEquals(0, loaded.timerAccumulatedSeconds)
        assertNull(loaded.timerRunningSince)
        assertEquals(PuzzleTime(hours = 1, minutes = 5), loaded.estimatedTimeSpent)
        assertEquals("reset", events("puzzle_timer_paused").single().metadata["reason"])
    }

    @Test
    fun editSaveKeepsRunningTimer() = runTest {
        val id = addInProgress("Edit")
        val begin = clock.now.minusSeconds(300)
        repo.startTimer(id, now = begin)

        repo.update(load(id).copy(notes = "Edited"))

        assertEquals(begin, load(id).timerRunningSince)
    }

    @Test
    fun futureAnchorIsPausedOnLoad() = runTest {
        val id = addInProgress("Skew")
        db.puzzles[id] = db.puzzles.getValue(id).copy(
            timerAccumulatedSeconds = 45,
            timerRunningSinceEpochMillis = clock.now.plusSeconds(3_600).toEpochMilli(),
        )

        repo.pauseSkewedTimers()

        assertNull(load(id).timerRunningSince)
        assertEquals(45, load(id).timerAccumulatedSeconds)
    }

    // endregion

    // region Telemetry

    @Test
    fun statusChangeLogsFromAndTo() = runTest {
        val id = add(fixture("Move"))
        repo.update(load(id).copy(status = PuzzleStatus.IN_PROGRESS))
        val metadata = events("puzzle_status_changed").single().metadata
        assertEquals("To-Do", metadata["status_from"])
        assertEquals("In-Progress", metadata["status_to"])
    }

    // endregion
}
