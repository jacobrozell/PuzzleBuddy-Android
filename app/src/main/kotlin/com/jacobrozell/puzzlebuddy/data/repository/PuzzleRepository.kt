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
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
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

    suspend fun upsert(puzzle: Puzzle, imageData: ByteArray? = null) {
        val existing = puzzleDao.findById(puzzle.id)
        val isNew = existing == null
        val image = when {
            imageData != null -> imageData
            else -> existing?.imageData
        }
        puzzleDao.upsert(puzzle.sanitized().toEntity(image))
        if (image != null) {
            imageCache.put(puzzle.id, image)
        } else {
            imageCache.remove(puzzle.id)
        }
        logger.info(
            LogCategory.PUZZLES,
            eventName = if (isNew) "puzzle_added" else "puzzle_updated",
            message = if (isNew) "Puzzle added." else "Puzzle updated.",
            metadata = mapOf(
                "puzzle_status" to puzzle.status.raw,
                "puzzle_count" to allPuzzles().size.toString(),
            ),
        )
        if (puzzle.barcode != null) {
            barcodeMetadataCache.storeFromPuzzle(puzzle.sanitized())
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
                upsert(puzzle)
            }
            logger.info(
                LogCategory.PUZZLES,
                eventName = "demo_data_loaded",
                message = "Demo puzzles loaded.",
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
            upsert(candidate)
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
