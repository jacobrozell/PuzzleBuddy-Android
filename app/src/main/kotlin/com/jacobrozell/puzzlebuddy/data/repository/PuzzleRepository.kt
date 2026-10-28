package com.jacobrozell.puzzlebuddy.data.repository

import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.demo.DemoAssetLoader
import com.jacobrozell.puzzlebuddy.data.local.dao.FriendDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleCompletionDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzlePhotoDao
import com.jacobrozell.puzzlebuddy.data.local.toDomain
import com.jacobrozell.puzzlebuddy.data.local.toEntity
import com.jacobrozell.puzzlebuddy.data.photo.PhotoFileStore
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.catalog.DemoDataCatalog
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDuplicateChecker
import com.jacobrozell.puzzlebuddy.domain.completion.CompletionUndoSemantics
import com.jacobrozell.puzzlebuddy.domain.completion.CompletionUndoSnapshot
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionSemantics
import com.jacobrozell.puzzlebuddy.domain.importing.IPDbCSVImporter
import com.jacobrozell.puzzlebuddy.domain.importing.PuzzleImportSummary
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsMilestones
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsUserContext
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAddSource
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAnalyticsMetadata
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.support.logging.warning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepository @Inject constructor(
    private val puzzleDao: PuzzleDao,
    private val photoDao: PuzzlePhotoDao,
    private val completionDao: PuzzleCompletionDao,
    private val friendDao: FriendDao,
    private val friendRepository: FriendRepository,
    private val transactions: TransactionRunner,
    private val photoStore: PhotoFileStore,
    private val demoAssets: DemoAssetLoader,
    private val clock: Clock,
    private val logger: AppLogger,
    private val barcodeMetadataCache: BarcodeMetadataCache,
    private val analyticsMilestones: AnalyticsMilestones,
    private val analyticsUserContext: AnalyticsUserContext,
) {
    private val undo = MutableStateFlow<CompletionUndoSnapshot?>(null)
    val pendingCompletionUndo: StateFlow<CompletionUndoSnapshot?> = undo

    fun observePuzzles(): Flow<List<Puzzle>> = combine(
        puzzleDao.observeAllWithChildren(),
        friendDao.observeAll(),
    ) { rows, friends ->
        val byId = friends.associate { it.id to it.toDomain() }
        rows.map { it.toDomain(byId) }
    }.catch { error ->
        logger.warning(
            LogCategory.PUZZLES,
            eventName = "puzzle_load_failed",
            message = error.message ?: "Puzzle load failed.",
        )
        emit(emptyList())
    }

    suspend fun allPuzzles(): List<Puzzle> = observePuzzles().first()

    suspend fun findById(id: String): Puzzle? {
        val friends = friendDao.all().associate { it.id to it.toDomain() }
        return puzzleDao.findWithChildren(id)?.toDomain(friends)
    }

    suspend fun upsert(
        puzzle: Puzzle,
        imageData: ByteArray? = null,
        addSource: PuzzleAddSource? = null,
    ) {
        val pending = linkedMapOf<String, ByteArray>()
        val next = if (imageData == null) {
            puzzle
        } else {
            val photo = PuzzlePhoto(sortOrder = 0, createdAt = Instant.now(clock))
            pending[photo.id] = imageData
            puzzle.copy(photos = listOf(photo))
        }
        if (puzzleDao.findById(puzzle.id) == null) {
            add(next, pending, addSource ?: PuzzleAddSource.MANUAL)
        } else {
            update(next, pending)
        }
    }

    suspend fun add(
        puzzle: Puzzle,
        pendingPhotoBytes: Map<String, ByteArray> = emptyMap(),
        source: PuzzleAddSource = PuzzleAddSource.MANUAL,
    ) {
        val (withFiles, written) = materializePhotos(puzzle, pendingPhotoBytes)
        try {
            val (saved, completionNumber) = transactions.run {
                requireUniqueBarcode(withFiles, excludingId = null)
                val (loaned, _) = prepareLoanFields(withFiles, storedWasOnLoan = false)
                val prepared = loaned.preparedForPersistence(defaultCurrency())
                puzzleDao.upsert(prepared.toEntity())
                if (prepared.photos.isNotEmpty()) {
                    photoDao.insertAll(prepared.photos.map { it.toEntity(prepared.id) })
                }
                val number = if (prepared.status == PuzzleStatus.COMPLETED) appendCompletion(prepared).second else null
                prepared to number
            }
            afterSave(saved, isNew = true, source = source, completionNumber = completionNumber)
        } catch (error: Exception) {
            written.forEach { photoStore.delete(it) }
            throw error
        }
    }

    suspend fun update(puzzle: Puzzle, pendingPhotoBytes: Map<String, ByteArray> = emptyMap()) {
        val (withFiles, written) = materializePhotos(puzzle, pendingPhotoBytes)
        var stagedUndo: CompletionUndoSnapshot? = null
        var droppedFiles = emptyList<String>()
        var becameOnLoan = false
        var leftStatusSeconds: Long? = null
        var previousStatus: PuzzleStatus? = null
        var completionNumber: Int? = null
        var saved = withFiles
        try {
            val outcome = transactions.run {
                requireUniqueBarcode(withFiles, excludingId = withFiles.id)
                val stored = puzzleDao.findById(withFiles.id)?.toDomain()
                    ?: throw PuzzleStoreException.RecordNotFound()
                val (loaned, markedOnLoan) = prepareLoanFields(withFiles, stored.isOnLoan)
                var next = loaned.preparedForPersistence(defaultCurrency())
                val now = Instant.now(clock)
                var leftSeconds: Long? = null
                if (next.status != PuzzleStatus.IN_PROGRESS && next.timerRunningSince != null) {
                    next = PuzzleTimerSemantics.paused(next, now)
                    leftSeconds = next.timerAccumulatedSeconds
                }
                if (next.status == PuzzleStatus.COMPLETED && stored.status != PuzzleStatus.COMPLETED) {
                    val prefill = PuzzleTimerSemantics.completionPrefill(
                        PuzzleTimerSemantics.elapsedSeconds(next, now),
                        next.estimatedTimeSpent,
                    )
                    if (prefill != null) next = next.copy(estimatedTimeSpent = prefill)
                }
                puzzleDao.upsert(next.toEntity())
                var undoSnapshot: CompletionUndoSnapshot? = null
                var recorded: Int? = null
                if (next.status == PuzzleStatus.COMPLETED && stored.status != PuzzleStatus.COMPLETED) {
                    val appended = appendCompletion(next)
                    recorded = appended.second
                    undoSnapshot = CompletionUndoSemantics.snapshot(
                        puzzleId = next.id,
                        completionId = appended.first,
                        previousStatus = stored.status,
                        previousProgressPercent = stored.progressPercent,
                        recordedAt = now,
                    )
                }
                val dropped = syncPhotos(next.id, next.photos)
                UpdateResult(next, undoSnapshot, dropped, markedOnLoan, leftSeconds, stored.status, recorded)
            }
            saved = outcome.puzzle
            stagedUndo = outcome.undo
            droppedFiles = outcome.droppedFiles
            becameOnLoan = outcome.becameOnLoan
            leftStatusSeconds = outcome.leftStatusSeconds
            previousStatus = outcome.previousStatus
            completionNumber = outcome.completionNumber
            if (stagedUndo != null) undo.value = stagedUndo
            droppedFiles.forEach { photoStore.delete(it) }
            afterSave(saved, isNew = false, source = null, completionNumber = completionNumber)
            if (previousStatus != null && previousStatus != saved.status) {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "puzzle_status_changed",
                    message = "Puzzle status changed.",
                    metadata = PuzzleAnalyticsMetadata.statusChangedMetadata(
                        from = previousStatus,
                        to = saved.status,
                        pieces = saved.pieces,
                    ),
                )
            }
            if (leftStatusSeconds != null) {
                logTimerPaused(PuzzleTimerSemantics.PauseReason.STATUS_LEFT, leftStatusSeconds)
            }
            if (becameOnLoan) {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "puzzle_marked_on_loan",
                    message = "Puzzle marked on loan.",
                )
            }
        } catch (error: Exception) {
            written.forEach { photoStore.delete(it) }
            throw error
        }
    }

    suspend fun delete(id: String) {
        val existing = puzzleDao.findWithChildren(id) ?: return
        val files = existing.photos.mapNotNull { it.fileName }
        transactions.run {
            photoDao.deleteForPuzzle(id)
            completionDao.deleteForPuzzle(id)
            puzzleDao.deleteById(id)
        }
        files.forEach { photoStore.delete(it) }
        if (undo.value?.puzzleId == id) undo.value = null
        logger.info(LogCategory.PUZZLES, eventName = "puzzle_deleted", message = "Puzzle deleted.")
        syncCollectionAnalytics()
    }

    suspend fun startRedo(id: String) {
        val stored = findById(id) ?: return
        if (stored.status != PuzzleStatus.COMPLETED) return
        val now = Instant.now(clock)
        if (undo.value?.puzzleId == id) undo.value = null
        update(
            stored.copy(
                status = PuzzleStatus.IN_PROGRESS,
                progressPercent = 0,
                startDate = now,
                timerAccumulatedSeconds = 0,
                timerRunningSince = null,
            ),
        )
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_redo_started",
            message = "Puzzle redo started.",
            metadata = mapOf("completion_count" to stored.timesCompleted.toString()),
        )
    }

    suspend fun markReturned(id: String) {
        val stored = findById(id) ?: return
        if (!stored.isOnLoan) return
        update(PuzzleLoanSemantics.clearLoan(stored))
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_marked_returned",
            message = "Puzzle marked returned.",
        )
    }

    suspend fun startTimer(id: String, now: Instant = Instant.now(clock)) {
        val pausedOthers = mutableListOf<Long>()
        var started = false
        transactions.run {
            val stored = puzzleDao.findById(id)?.toDomain() ?: throw PuzzleStoreException.RecordNotFound()
            if (stored.status != PuzzleStatus.IN_PROGRESS) return@run
            if (PuzzleTimerSemantics.isRunning(stored.timerRunningSince, now)) return@run
            val others = puzzleDao.allOnce().map { it.toDomain() }
            for (otherId in PuzzleTimerSemantics.idsToPause(id, others)) {
                val other = puzzleDao.findById(otherId)?.toDomain() ?: continue
                val folded = PuzzleTimerSemantics.paused(other, now)
                puzzleDao.updateTimer(otherId, folded.timerAccumulatedSeconds, null)
                pausedOthers += folded.timerAccumulatedSeconds
            }
            val self = PuzzleTimerSemantics.paused(stored, now)
            puzzleDao.updateTimer(id, self.timerAccumulatedSeconds, now.toEpochMilli())
            started = true
        }
        if (!started) return
        pausedOthers.forEach { seconds ->
            logTimerPaused(PuzzleTimerSemantics.PauseReason.OTHER_STARTED, seconds)
        }
        logger.info(LogCategory.PUZZLES, eventName = "puzzle_timer_started", message = "Puzzle timer started.")
    }

    suspend fun pauseTimer(id: String, now: Instant = Instant.now(clock)) {
        val seconds = transactions.run {
            val stored = puzzleDao.findById(id)?.toDomain() ?: return@run null
            if (!PuzzleTimerSemantics.isRunning(stored.timerRunningSince, now)) return@run null
            val folded = PuzzleTimerSemantics.paused(stored, now)
            puzzleDao.updateTimer(id, folded.timerAccumulatedSeconds, null)
            folded.timerAccumulatedSeconds
        } ?: return
        logTimerPaused(PuzzleTimerSemantics.PauseReason.PAUSE, seconds)
    }

    suspend fun resetTimer(id: String, now: Instant = Instant.now(clock)) {
        val total = transactions.run {
            val stored = puzzleDao.findById(id)?.toDomain() ?: return@run null
            val elapsed = PuzzleTimerSemantics.elapsedSeconds(stored, now)
            if (elapsed == 0L && stored.timerRunningSince == null) return@run null
            puzzleDao.updateTimer(id, 0, null)
            elapsed
        } ?: return
        logTimerPaused(PuzzleTimerSemantics.PauseReason.RESET, total)
    }

    suspend fun pauseSkewedTimers(now: Instant = Instant.now(clock)) {
        val skewed = puzzleDao.allOnce().map { it.toDomain() }
            .filter { PuzzleTimerSemantics.isClockSkewed(it.timerRunningSince, now) }
        if (skewed.isEmpty()) return
        transactions.run {
            skewed.forEach { puzzle ->
                val folded = PuzzleTimerSemantics.paused(puzzle, now)
                puzzleDao.updateTimer(puzzle.id, folded.timerAccumulatedSeconds, null)
            }
        }
    }

    suspend fun deleteCompletion(
        puzzleId: String,
        completionId: String,
        statusIfRemovingLast: PuzzleStatus? = null,
        logDeletedEvent: Boolean = true,
    ) {
        val puzzle = puzzleDao.findById(puzzleId) ?: throw PuzzleStoreException.RecordNotFound()
        val completion = completionDao.findById(completionId) ?: throw PuzzleStoreException.CompletionNotFound()
        val count = loadCompletions(puzzleId).size
        if (count <= 1 && puzzle.status == PuzzleStatus.COMPLETED.raw && statusIfRemovingLast == null) {
            throw PuzzleStoreException.StatusRequiredAfterRemovingLastCompletion()
        }
        transactions.run {
            completionDao.deleteById(completionId)
            reconcile(puzzleId, statusIfRemovingLast)
        }
        if (undo.value?.completionId == completionId) undo.value = null
        if (logDeletedEvent) {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_completion_deleted",
                message = "Completion deleted.",
                metadata = mapOf("completion_number" to completion.completionNumber.toString()),
            )
        }
    }

    suspend fun updateCompletion(puzzleId: String, completion: PuzzleCompletion) {
        if (puzzleDao.findById(puzzleId) == null) throw PuzzleStoreException.RecordNotFound()
        if (completionDao.findById(completion.id) == null) throw PuzzleStoreException.CompletionNotFound()
        var number = completion.completionNumber
        transactions.run {
            completionDao.upsert(completion.toEntity(puzzleId))
            val renumbered = PuzzleCompletionSemantics.renumbered(loadCompletions(puzzleId))
            number = renumbered.first { it.id == completion.id }.completionNumber
            completionDao.upsertAll(renumbered.map { it.toEntity(puzzleId) })
            reconcile(puzzleId, null)
        }
        if (undo.value?.completionId == completion.id) undo.value = null
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_completion_updated",
            message = "Completion updated.",
            metadata = mapOf("completion_number" to number.toString()),
        )
    }

    suspend fun undoLastCompletion(puzzleId: String, now: Instant = Instant.now(clock)) {
        val snapshot = undo.value
        if (snapshot == null || snapshot.puzzleId != puzzleId || !snapshot.isActive(now)) {
            if (snapshot?.puzzleId == puzzleId) undo.value = null
            throw PuzzleStoreException.CompletionUndoUnavailable()
        }
        val number = completionDao.findById(snapshot.completionId)?.completionNumber
        deleteCompletion(
            puzzleId = puzzleId,
            completionId = snapshot.completionId,
            statusIfRemovingLast = snapshot.previousStatus,
            logDeletedEvent = false,
        )
        val current = puzzleDao.findById(puzzleId)?.toDomain() ?: throw PuzzleStoreException.RecordNotFound()
        puzzleDao.upsert(
            current.copy(
                status = snapshot.previousStatus,
                progressPercent = snapshot.previousProgressPercent,
            ).toEntity(),
        )
        undo.value = null
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_completion_undone",
            message = "Completion undone.",
            metadata = mapOf("completion_number" to (number ?: 0).toString()),
        )
    }

    fun activeCompletionUndo(puzzleId: String, now: Instant = Instant.now(clock)): CompletionUndoSnapshot? =
        CompletionUndoSemantics.activeSnapshot(undo.value, puzzleId, now)

    fun expireCompletionUndoIfNeeded(puzzleId: String, now: Instant = Instant.now(clock)) {
        val snapshot = undo.value ?: return
        if (snapshot.puzzleId == puzzleId && !snapshot.isActive(now)) undo.value = null
    }

    fun dismissCompletionUndo(puzzleId: String) {
        if (undo.value?.puzzleId == puzzleId) undo.value = null
    }

    suspend fun clearAll() {
        transactions.run {
            photoDao.deleteAll()
            completionDao.deleteAll()
            puzzleDao.deleteAll()
            friendDao.deleteAll()
        }
        photoStore.deleteAll()
        undo.value = null
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_collection_cleared",
            message = "Collection cleared.",
        )
        syncCollectionAnalytics()
    }

    suspend fun demoCount(): Int = puzzleDao.countDemo()

    suspend fun loadDemoPuzzles() {
        try {
            val shelf = DemoDataCatalog.make(Instant.now(clock), ZoneId.systemDefault())
            for (puzzle in shelf.puzzles) {
                val pending = linkedMapOf<String, ByteArray>()
                val photos = puzzle.photos.map { photo ->
                    val asset = photo.fileName?.removeSuffix(".jpg") ?: return@map photo
                    val bytes = demoAssets.bytes(asset) ?: return@map photo
                    pending[photo.id] = bytes
                    photo.copy(fileName = null)
                }
                addFromSeed(puzzle.copy(photos = photos), pending, puzzle.completions)
            }
            logger.info(
                LogCategory.PUZZLES,
                eventName = "demo_data_loaded",
                message = "Demo puzzles loaded.",
                metadata = mapOf("puzzle_count" to demoCount().toString()),
            )
        } catch (error: Exception) {
            logger.warning(
                LogCategory.PUZZLES,
                eventName = "demo_data_seed_failed",
                message = error.message ?: "Demo data seed failed.",
            )
        }
    }

    suspend fun removeDemoPuzzles() {
        val ids = puzzleDao.demoIds()
        ids.forEach { delete(it) }
        val keeping = puzzleDao.allOnce().filter { it.isOnLoan }.mapNotNull { it.loanedToFriendId }.toSet()
        friendRepository.removeDemoFriends(keeping)
        logger.info(
            LogCategory.PUZZLES,
            eventName = "demo_data_removed",
            message = "Demo puzzles removed.",
            metadata = mapOf("removed_count" to ids.size.toString()),
        )
        syncCollectionAnalytics()
    }

    suspend fun importPuzzles(incoming: List<Puzzle>): PuzzleImportSummary {
        val summary = PuzzleImportSummary()
        for (puzzle in incoming) {
            val trimmedName = puzzle.name.trim()
            if (trimmedName.isEmpty()) {
                summary.skippedInvalid++
                continue
            }
            val candidate = puzzle.copy(name = trimmedName)
            try {
                add(candidate, source = PuzzleAddSource.IMPORT)
                summary.imported++
            } catch (error: PuzzleStoreException.DuplicateBarcode) {
                summary.skippedDuplicates++
            }
        }
        return summary
    }

    suspend fun importIpdbCsv(data: ByteArray): PuzzleImportSummary {
        val puzzles = IPDbCSVImporter.puzzlesFrom(data)
        return importPuzzles(puzzles)
    }

    suspend fun imageDataFor(id: String): ByteArray? {
        val cover = findById(id)?.let { PuzzlePhotoSemantics.cover(it.photos) } ?: return null
        val name = cover.fileName ?: return null
        return photoStore.read(name)
    }

    suspend fun findByBarcode(barcode: String): Puzzle? {
        val normalized = BarcodeValidator.normalizeOrNull(barcode) ?: return null
        return allPuzzles().firstOrNull { puzzle ->
            BarcodeValidator.normalizeOrNull(puzzle.barcode.orEmpty()) == normalized
        }
    }

    suspend fun isDuplicateBarcode(barcode: String, excludingId: String? = null): Puzzle? =
        PuzzleDuplicateChecker.findDuplicate(barcode, excludingId, allPuzzles())

    private suspend fun addFromSeed(
        puzzle: Puzzle,
        pending: Map<String, ByteArray>,
        completions: List<PuzzleCompletion>,
    ) {
        val (withFiles, written) = materializePhotos(puzzle, pending)
        try {
            transactions.run {
                val (loaned, _) = prepareLoanFields(withFiles, storedWasOnLoan = false)
                val prepared = loaned.preparedForPersistence(defaultCurrency()).copy(
                    completions = completions,
                    timesCompleted = maxOf(completions.size, loaned.timesCompleted),
                )
                puzzleDao.upsert(prepared.toEntity())
                if (prepared.photos.isNotEmpty()) {
                    photoDao.insertAll(prepared.photos.map { it.toEntity(prepared.id) })
                }
                if (completions.isNotEmpty()) {
                    completionDao.upsertAll(completions.map { it.toEntity(prepared.id) })
                }
            }
        } catch (error: Exception) {
            written.forEach { photoStore.delete(it) }
            throw error
        }
    }

    private suspend fun materializePhotos(
        puzzle: Puzzle,
        pending: Map<String, ByteArray>,
    ): Pair<Puzzle, List<String>> {
        val written = mutableListOf<String>()
        try {
            val photos = puzzle.photos.map { photo ->
                val bytes = pending[photo.id] ?: return@map photo
                val name = photoStore.write(bytes)
                written += name
                photo.copy(fileName = name)
            }
            return puzzle.copy(photos = photos) to written
        } catch (error: Exception) {
            written.forEach { photoStore.delete(it) }
            throw error
        }
    }

    private suspend fun requireUniqueBarcode(puzzle: Puzzle, excludingId: String?) {
        val normalized = BarcodeNormalizer.normalize(puzzle.barcode) ?: return
        val match = puzzleDao.allOnce().firstOrNull { entity ->
            entity.id != excludingId && BarcodeNormalizer.normalize(entity.barcode) == normalized
        } ?: return
        throw PuzzleStoreException.DuplicateBarcode(match.name, normalized)
    }

    private suspend fun prepareLoanFields(
        puzzle: Puzzle,
        storedWasOnLoan: Boolean,
    ): Pair<Puzzle, Boolean> {
        val prepared = PuzzleLoanSemantics.prepareLoanState(puzzle, Instant.now(clock))
        val name = prepared.loanedToDisplayName
        val withFriend = when {
            prepared.isOnLoan && name != null -> {
                val friend = friendRepository.findOrCreate(name, isDemo = puzzle.isDemo)
                prepared.copy(loanedToFriendId = friend.id)
            }
            prepared.isOnLoan -> prepared.copy(loanedToFriendId = null)
            else -> prepared
        }
        return withFriend to (withFriend.isOnLoan && !storedWasOnLoan)
    }

    private suspend fun appendCompletion(puzzle: Puzzle): Pair<String, Int> {
        val next = (loadCompletions(puzzle.id).maxOfOrNull { it.completionNumber } ?: 0) + 1
        val completion = PuzzleCompletionSemantics.makeCompletion(puzzle, next)
        completionDao.upsert(completion.toEntity(puzzle.id))
        puzzleDao.updateTimesCompleted(puzzle.id, next)
        return completion.id to next
    }

    private suspend fun loadCompletions(puzzleId: String): List<PuzzleCompletion> = try {
        completionDao.forPuzzle(puzzleId).map { it.toDomain() }
    } catch (error: PuzzleStoreException) {
        throw error
    } catch (error: Exception) {
        throw PuzzleStoreException.CompletionHistoryUnavailable()
    }

    private suspend fun reconcile(puzzleId: String, statusIfEmpty: PuzzleStatus?) {
        val renumbered = PuzzleCompletionSemantics.renumbered(loadCompletions(puzzleId))
        completionDao.upsertAll(renumbered.map { it.toEntity(puzzleId) })
        val stored = puzzleDao.findById(puzzleId)?.toDomain() ?: throw PuzzleStoreException.RecordNotFound()
        var next = stored.copy(timesCompleted = renumbered.size, completions = renumbered)
        if (renumbered.isEmpty() && next.status == PuzzleStatus.COMPLETED) {
            val status = statusIfEmpty ?: throw PuzzleStoreException.StatusRequiredAfterRemovingLastCompletion()
            var progress = next.progressPercent
            if (status == PuzzleStatus.TODO) progress = 0
            if (status == PuzzleStatus.IN_PROGRESS && progress >= 100) progress = 0
            next = next.copy(status = status, progressPercent = progress)
        } else if (renumbered.isNotEmpty()) {
            next = next.copy(completionDate = renumbered.maxBy { it.completedAt }.completedAt)
        }
        puzzleDao.upsert(next.toEntity())
    }

    private suspend fun syncPhotos(puzzleId: String, photos: List<PuzzlePhoto>): List<String> {
        val previous = photoDao.forPuzzle(puzzleId).mapNotNull { it.fileName }.toSet()
        photoDao.deleteForPuzzle(puzzleId)
        if (photos.isNotEmpty()) photoDao.insertAll(photos.map { it.toEntity(puzzleId) })
        val kept = photos.mapNotNull { it.fileName }.toSet()
        return (previous - kept).toList()
    }

    private suspend fun afterSave(
        puzzle: Puzzle,
        isNew: Boolean,
        source: PuzzleAddSource?,
        completionNumber: Int?,
    ) {
        if (puzzle.barcode != null) barcodeMetadataCache.storeFromPuzzle(puzzle)
        if (isNew) {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_added",
                message = "Puzzle added.",
                metadata = PuzzleAnalyticsMetadata.metadata(puzzle, source ?: PuzzleAddSource.MANUAL),
            )
        } else {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_updated",
                message = "Puzzle updated.",
                metadata = PuzzleAnalyticsMetadata.metadata(puzzle),
            )
        }
        if (completionNumber != null) {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_completion_recorded",
                message = "Recorded puzzle completion.",
                metadata = PuzzleAnalyticsMetadata.completionMetadata(puzzle, completionNumber),
            )
            analyticsMilestones.logCompletionMilestones(completionNumber)
        }
        syncCollectionAnalytics()
    }

    private fun logTimerPaused(reason: PuzzleTimerSemantics.PauseReason, seconds: Long) {
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_timer_paused",
            message = "Puzzle timer paused.",
            metadata = mapOf(
                "reason" to reason.raw,
                "duration_bucket" to PuzzleTimerSemantics.durationBucket(seconds),
            ),
        )
    }

    private suspend fun syncCollectionAnalytics() {
        val puzzles = allPuzzles()
        analyticsMilestones.logCollectionSizeMilestones(puzzles.size)
        analyticsMilestones.logCompletionMilestones(puzzles.count { it.status == PuzzleStatus.COMPLETED })
        analyticsUserContext.syncCollection(puzzles)
    }

    private fun defaultCurrency(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("USD")

    private data class UpdateResult(
        val puzzle: Puzzle,
        val undo: CompletionUndoSnapshot?,
        val droppedFiles: List<String>,
        val becameOnLoan: Boolean,
        val leftStatusSeconds: Long?,
        val previousStatus: PuzzleStatus,
        val completionNumber: Int?,
    )
}
