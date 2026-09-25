package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Once-per-cold-start session snapshot + days-since-last-open bucket. */
@Singleton
class AnalyticsSessionContext(
    private val store: AnalyticsKeyValueStore,
    private val logger: AppLogger,
    private val clock: AnalyticsClock,
    private val onCollectionSync: suspend (List<Puzzle>) -> Unit,
) {
    @Inject
    constructor(
        store: AnalyticsKeyValueStore,
        logger: AppLogger,
        userContext: AnalyticsUserContext,
    ) : this(
        store = store,
        logger = logger,
        clock = AnalyticsClock { System.currentTimeMillis() },
        onCollectionSync = { puzzles -> userContext.syncCollection(puzzles) },
    )

    private var didLogSnapshot = false
    private var pendingDaysBucket: String? = null

    fun beginSession() {
        if (pendingDaysBucket != null) return
        pendingDaysBucket = consumeDaysSinceLastOpenBucket()
    }

    suspend fun logSnapshotIfNeeded(puzzles: List<Puzzle>) {
        if (didLogSnapshot) return
        didLogSnapshot = true

        val metadata = PuzzleAnalyticsMetadata.collectionSnapshotMetadata(puzzles).toMutableMap()
        metadata["days_since_last_open_bucket"] = pendingDaysBucket
            ?: PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(days = null)

        logger.info(
            LogCategory.APP,
            eventName = "session_snapshot",
            message = "Session collection snapshot.",
            metadata = metadata,
        )
        onCollectionSync(puzzles)
    }

    fun resetForTesting() {
        didLogSnapshot = false
        pendingDaysBucket = null
    }

    private fun consumeDaysSinceLastOpenBucket(): String {
        val now = clock.nowMillis()
        val previous = store.getLongOrNull(LAST_OPEN_KEY)
        val bucket = if (previous == null) {
            PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(days = null)
        } else {
            val days = TimeUnit.MILLISECONDS.toDays(now - previous).toInt()
            PuzzleAnalyticsMetadata.daysSinceLastOpenBucket(days = maxOf(0, days))
        }
        store.putLong(LAST_OPEN_KEY, now)
        return bucket
    }

    companion object {
        const val LAST_OPEN_KEY = "PuzzleBuddy.AnalyticsLastOpenAt"
    }
}
