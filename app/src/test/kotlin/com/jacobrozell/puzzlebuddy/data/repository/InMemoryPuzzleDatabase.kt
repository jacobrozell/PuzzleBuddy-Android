package com.jacobrozell.puzzlebuddy.data.repository

import com.jacobrozell.puzzlebuddy.data.local.PuzzleWithChildren
import com.jacobrozell.puzzlebuddy.data.local.dao.FriendDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleCompletionDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzlePhotoDao
import com.jacobrozell.puzzlebuddy.data.local.entity.FriendEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleCompletionEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzlePhotoEntity
import com.jacobrozell.puzzlebuddy.data.photo.PhotoFileStore
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Map-backed stand-in for the four Room DAOs. [transactions] snapshots every table and restores it
 * when the block throws, so "save failed" tests see the same rollback Room gives.
 */
class InMemoryPuzzleDatabase {
    val puzzles = linkedMapOf<String, PuzzleEntity>()
    val photos = linkedMapOf<String, PuzzlePhotoEntity>()
    val completions = linkedMapOf<String, PuzzleCompletionEntity>()
    val friends = linkedMapOf<String, FriendEntity>()

    /** Throws on the next [PuzzleDao.upsert], like a SQLite write failure. */
    var failNextPuzzleWrite = false

    /** Throws on the next completion insert, after the puzzle row was already written. */
    var failNextCompletionWrite = false

    /** Throws on the next [PuzzleCompletionDao.forPuzzle], like a failed fetch. */
    var failNextCompletionRead = false

    private val version = MutableStateFlow(0)

    private fun changed() {
        version.value += 1
    }

    private fun childrenOf(entity: PuzzleEntity) = PuzzleWithChildren(
        puzzle = entity,
        photos = photos.values.filter { it.puzzleId == entity.id },
        completions = completions.values.filter { it.puzzleId == entity.id },
    )

    private fun maybeFailCompletionWrite() {
        if (failNextCompletionWrite) {
            failNextCompletionWrite = false
            throw IllegalStateException("Simulated completion write failure")
        }
    }

    val transactions = object : TransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            val saved = listOf(
                LinkedHashMap(puzzles),
                LinkedHashMap(photos),
                LinkedHashMap(completions),
                LinkedHashMap(friends),
            )
            try {
                return block()
            } catch (error: Throwable) {
                @Suppress("UNCHECKED_CAST")
                restore(saved as List<Map<String, Any>>)
                throw error
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun restore(saved: List<Map<String, Any>>) {
        puzzles.clear(); puzzles.putAll(saved[0] as Map<String, PuzzleEntity>)
        photos.clear(); photos.putAll(saved[1] as Map<String, PuzzlePhotoEntity>)
        completions.clear(); completions.putAll(saved[2] as Map<String, PuzzleCompletionEntity>)
        friends.clear(); friends.putAll(saved[3] as Map<String, FriendEntity>)
        changed()
    }

    val puzzleDao = object : PuzzleDao {
        override fun observeAllWithChildren(): Flow<List<PuzzleWithChildren>> = version.map {
            puzzles.values.sortedBy { it.name.lowercase() }.map(::childrenOf)
        }

        override fun observeAll(): Flow<List<PuzzleEntity>> = version.map {
            puzzles.values.sortedBy { it.name.lowercase() }
        }

        override suspend fun findWithChildren(id: String): PuzzleWithChildren? = puzzles[id]?.let(::childrenOf)
        override suspend fun findById(id: String): PuzzleEntity? = puzzles[id]
        override suspend fun allOnce(): List<PuzzleEntity> = puzzles.values.toList()

        override suspend fun upsert(entity: PuzzleEntity) {
            if (failNextPuzzleWrite) {
                failNextPuzzleWrite = false
                throw IllegalStateException("Simulated puzzle write failure")
            }
            puzzles[entity.id] = entity
            changed()
        }

        override suspend fun deleteById(id: String) {
            puzzles.remove(id)
            changed()
        }

        override suspend fun deleteAll() {
            puzzles.clear()
            changed()
        }

        override suspend fun demoIds(): List<String> = puzzles.values.filter { it.isDemo }.map { it.id }
        override suspend fun countDemo(): Int = puzzles.values.count { it.isDemo }

        override suspend fun updateTimesCompleted(id: String, timesCompleted: Int) {
            puzzles[id]?.let { puzzles[id] = it.copy(timesCompleted = timesCompleted) }
            changed()
        }

        override suspend fun updateTimer(id: String, seconds: Long, runningSince: Long?) {
            puzzles[id]?.let {
                puzzles[id] = it.copy(timerAccumulatedSeconds = seconds, timerRunningSinceEpochMillis = runningSince)
            }
            changed()
        }
    }

    val photoDao = object : PuzzlePhotoDao {
        override suspend fun forPuzzle(puzzleId: String): List<PuzzlePhotoEntity> =
            photos.values.filter { it.puzzleId == puzzleId }.sortedBy { it.sortOrder }

        override suspend fun insertAll(photos: List<PuzzlePhotoEntity>) {
            photos.forEach { this@InMemoryPuzzleDatabase.photos[it.id] = it }
            changed()
        }

        override suspend fun deleteForPuzzle(puzzleId: String) {
            photos.values.removeAll { it.puzzleId == puzzleId }
            changed()
        }

        override suspend fun deleteById(id: String) {
            photos.remove(id)
            changed()
        }

        override suspend fun deleteAll() {
            photos.clear()
            changed()
        }

        override suspend fun allFileNames(): List<String> = photos.values.mapNotNull { it.fileName }
    }

    val completionDao = object : PuzzleCompletionDao {
        override suspend fun forPuzzle(puzzleId: String): List<PuzzleCompletionEntity> {
            if (failNextCompletionRead) {
                failNextCompletionRead = false
                throw IllegalStateException("Simulated completion fetch failure")
            }
            return completions.values.filter { it.puzzleId == puzzleId }.sortedBy { it.completionNumber }
        }

        override suspend fun findById(id: String): PuzzleCompletionEntity? = completions[id]

        override suspend fun upsert(entity: PuzzleCompletionEntity) {
            maybeFailCompletionWrite()
            completions[entity.id] = entity
            changed()
        }

        override suspend fun upsertAll(entities: List<PuzzleCompletionEntity>) {
            maybeFailCompletionWrite()
            entities.forEach { completions[it.id] = it }
            changed()
        }

        override suspend fun deleteById(id: String) {
            completions.remove(id)
            changed()
        }

        override suspend fun deleteForPuzzle(puzzleId: String) {
            completions.values.removeAll { it.puzzleId == puzzleId }
            changed()
        }

        override suspend fun deleteAll() {
            completions.clear()
            changed()
        }
    }

    val friendDao = object : FriendDao {
        override fun observeAll(): Flow<List<FriendEntity>> = version.map {
            friends.values.sortedBy { it.displayName.lowercase() }
        }

        override suspend fun all(): List<FriendEntity> = friends.values.toList()
        override suspend fun findById(id: String): FriendEntity? = friends[id]

        override suspend fun findByNameIgnoreCase(name: String): FriendEntity? =
            friends.values.firstOrNull { it.displayName.equals(name, ignoreCase = true) }

        override suspend fun upsert(entity: FriendEntity) {
            friends[entity.id] = entity
            changed()
        }

        override suspend fun deleteById(id: String) {
            friends.remove(id)
            changed()
        }

        override suspend fun deleteAll() {
            friends.clear()
            changed()
        }

        override suspend fun demoFriends(): List<FriendEntity> = friends.values.filter { it.isDemo }
    }
}

class InMemoryPhotoFileStore : PhotoFileStore {
    val files = linkedMapOf<String, ByteArray>()
    private var counter = 0

    override suspend fun write(bytes: ByteArray): String {
        counter += 1
        val name = "photo-$counter.jpg"
        files[name] = bytes
        return name
    }

    override fun file(fileName: String): File = File("/in-memory/$fileName")
    override suspend fun read(fileName: String): ByteArray? = files[fileName]

    override suspend fun delete(fileName: String) {
        files.remove(fileName)
    }

    override suspend fun deleteAll() {
        files.clear()
    }

    override suspend fun deleteOrphans(keep: Set<String>) {
        files.keys.retainAll(keep)
    }
}

class RecordingBarcodeRecorder : PuzzleBarcodeRecorder {
    val stored = mutableListOf<Puzzle>()

    override suspend fun storeFromPuzzle(puzzle: Puzzle) {
        stored += puzzle
    }
}

class RecordingCollectionAnalytics : CollectionAnalyticsSync {
    var lastCount: Int? = null

    override suspend fun syncCollection(puzzles: List<Puzzle>, onboardingComplete: Boolean?) {
        lastCount = puzzles.size
    }
}

class MutableClock(var now: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now

    fun advanceSeconds(seconds: Long) {
        now = now.plusSeconds(seconds)
    }
}
