package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class AnalyticsSessionContextTest {
    @Test
    fun firstOpenSnapshotUsesFirstOpenBucket() = runBlocking {
        val store = InMemoryAnalyticsKeyValueStore()
        val logger = RecordingAppLogger()
        val session = AnalyticsSessionContext(
            store = store,
            logger = logger,
            clock = AnalyticsClock { 1_000L },
        )
        session.beginSession()
        session.logSnapshotIfNeeded(emptyList())

        val event = logger.entries.single { it.eventName == "session_snapshot" }
        assertEquals("first_open", event.metadata["days_since_last_open_bucket"])
        assertEquals("0", event.metadata["collection_size_bucket"])
    }

    @Test
    fun laterOpenUsesDayBucketAndLogsOnce() = runBlocking {
        val store = InMemoryAnalyticsKeyValueStore()
        val now = TimeUnit.DAYS.toMillis(10)
        store.putLong(AnalyticsSessionContext.LAST_OPEN_KEY, now - TimeUnit.DAYS.toMillis(3))
        val logger = RecordingAppLogger()
        val session = AnalyticsSessionContext(
            store = store,
            logger = logger,
            clock = AnalyticsClock { now },
        )
        session.beginSession()
        session.logSnapshotIfNeeded(
            listOf(Puzzle(name = "Done", status = PuzzleStatus.COMPLETED)),
        )
        session.logSnapshotIfNeeded(
            listOf(Puzzle(name = "Done", status = PuzzleStatus.COMPLETED)),
        )

        val snapshots = logger.entries.filter { it.eventName == "session_snapshot" }
        assertEquals(1, snapshots.size)
        assertEquals("2_7", snapshots.first().metadata["days_since_last_open_bucket"])
        assertEquals("1", snapshots.first().metadata["count_completed"])
    }
}

private fun AnalyticsSessionContext(
    store: AnalyticsKeyValueStore,
    logger: AppLogger,
    clock: AnalyticsClock,
): AnalyticsSessionContext = AnalyticsSessionContext(
    store = store,
    logger = logger,
    clock = clock,
    onCollectionSync = {},
)
