package com.jacobrozell.puzzlebuddy.data.repository

import android.util.LruCache
import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.toDomain
import com.jacobrozell.puzzlebuddy.data.local.toEntity
import com.jacobrozell.puzzlebuddy.domain.catalog.DemoDataCatalog
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDuplicateChecker
import com.jacobrozell.puzzlebuddy.domain.importing.IPDbCSVImporter
import com.jacobrozell.puzzlebuddy.domain.importing.PuzzleImportSummary
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAddSource
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAnalyticsMetadata
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.support.logging.warning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepository @Inject constructor(
    private val puzzleDao: PuzzleDao,
    private val logger: AppLogger,
    private val barcodeMetadataCache: BarcodeMetadataCache,
) {
    private val imageCache = LruCache<String, ByteArray>(32)

    fun observePuzzles(): Flow<List<Puzzle>> =
        puzzleDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { error ->
                logger.warning(
                    LogCategory.PUZZLES,
                    eventName = "puzzle_load_failed",
                    message = error.message ?: "Puzzle load failed.",
                )
                emit(emptyList())
            }

    suspend fun allPuzzles(): List<Puzzle> =
        try {
            observePuzzles().first()
        } catch (error: Exception) {
            logger.warning(
                LogCategory.PUZZLES,
                eventName = "puzzle_load_failed",
                message = error.message ?: "Puzzle load failed.",
            )
            emptyList()
        }

    suspend fun findById(id: String): Puzzle? =
        puzzleDao.findById(id)?.toDomain()

    suspend fun upsert(
        puzzle: Puzzle,
        imageData: ByteArray? = null,
        addSource: PuzzleAddSource? = null,
    ) {
        val existing = puzzleDao.findById(puzzle.id)
        val isNew = existing == null
        val previousStatus = existing?.toDomain()?.status
        val image = when {
            imageData != null -> imageData
            else -> existing?.imageData
        }
        val sanitized = puzzle.sanitized().copy(hasImage = image != null)
        puzzleDao.upsert(sanitized.toEntity(image))
        if (image != null) {
            imageCache.put(puzzle.id, image)
        } else {
            imageCache.remove(puzzle.id)
        }

        if (isNew) {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_added",
                message = "Puzzle added.",
                metadata = PuzzleAnalyticsMetadata.metadata(
                    puzzle = sanitized,
                    addSource = addSource ?: PuzzleAddSource.MANUAL,
                ),
            )
        } else {
            logger.info(
                LogCategory.PUZZLES,
                eventName = "puzzle_updated",
                message = "Puzzle updated.",
                metadata = PuzzleAnalyticsMetadata.metadata(puzzle = sanitized),
            )
            if (previousStatus != null && previousStatus != sanitized.status) {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "puzzle_status_changed",
                    message = "Puzzle status changed.",
                    metadata = PuzzleAnalyticsMetadata.statusChangedMetadata(
                        from = previousStatus,
                        to = sanitized.status,
                        pieces = sanitized.pieces,
                    ),
                )
                if (previousStatus != PuzzleStatus.COMPLETED && sanitized.status == PuzzleStatus.COMPLETED) {
                    logger.info(
                        LogCategory.PUZZLES,
                        eventName = "puzzle_completion_recorded",
                        message = "Recorded puzzle completion.",
                        metadata = PuzzleAnalyticsMetadata.completionMetadata(
                            puzzle = sanitized,
                            completionNumber = 1,
                        ),
                    )
                }
            }
        }
        if (sanitized.barcode != null) {
            barcodeMetadataCache.storeFromPuzzle(sanitized)
        }
    }

    suspend fun delete(id: String) {
        puzzleDao.deleteById(id)
        imageCache.remove(id)
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_deleted",
            message = "Puzzle deleted.",
            metadata = mapOf("puzzle_count" to allPuzzles().size.toString()),
        )
    }

    suspend fun imageDataFor(id: String): ByteArray? {
        imageCache.get(id)?.let { return it }
        val data = puzzleDao.findById(id)?.imageData
        if (data != null) imageCache.put(id, data)
        return data
    }

    suspend fun clearAll() {
        puzzleDao.deleteAll()
        imageCache.evictAll()
        logger.info(
            LogCategory.PUZZLES,
            eventName = "puzzle_collection_cleared",
            message = "Collection cleared.",
        )
    }

    suspend fun demoCount(): Int = puzzleDao.countDemo()

    suspend fun loadDemoPuzzles() {
        try {
            for (puzzle in DemoDataCatalog.makePuzzles()) {
                upsert(puzzle, addSource = PuzzleAddSource.DEMO)
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
        puzzleDao.deleteDemo()
        imageCache.evictAll()
        logger.info(
            LogCategory.PUZZLES,
            eventName = "demo_data_removed",
            message = "Demo puzzles removed.",
        )
    }

    suspend fun importPuzzles(incoming: List<Puzzle>): PuzzleImportSummary {
        val summary = PuzzleImportSummary()
        val existing = allPuzzles()
        for (puzzle in incoming) {
            val trimmedName = puzzle.name.trim()
            if (trimmedName.isEmpty()) {
                summary.skippedInvalid++
                continue
            }
            val candidate = puzzle.copy(name = trimmedName)
            val duplicate = PuzzleDuplicateChecker.findDuplicate(
                barcode = candidate.barcode,
                excludingId = null,
                puzzles = existing,
            )
            if (duplicate != null) {
                summary.skippedDuplicates++
                continue
            }
            upsert(candidate, addSource = PuzzleAddSource.IMPORT)
            summary.imported++
        }
        return summary
    }

    suspend fun importIpdbCsv(data: ByteArray): PuzzleImportSummary {
        val puzzles = IPDbCSVImporter.puzzlesFrom(data)
        return importPuzzles(puzzles)
    }

    suspend fun findByBarcode(barcode: String): Puzzle? {
        val normalized = BarcodeValidator.normalizeOrNull(barcode) ?: return null
        return allPuzzles().firstOrNull { puzzle ->
            BarcodeValidator.normalizeOrNull(puzzle.barcode.orEmpty()) == normalized
        }
    }

    suspend fun isDuplicateBarcode(barcode: String, excludingId: String? = null): Puzzle? {
        val puzzles = allPuzzles()
        return PuzzleDuplicateChecker.findDuplicate(barcode, excludingId, puzzles)
    }
}
