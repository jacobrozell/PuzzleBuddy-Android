package com.jacobrozell.puzzlebuddy.support.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class FirebaseCrashlyticsEventMappingTest {
    @Test
    fun mapsAllowlistedErrorToNonFatal() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.ERROR,
            category = LogCategory.PUZZLES,
            eventName = "puzzle_load_failed",
            message = "load failed",
            metadata = mapOf("puzzle_count" to "2"),
        )
        val error = FirebaseCrashlyticsEventMapping.nonFatalError(entry, appVersion = "1.0.0")
        assertEquals(2001, (error as LoggerNonFatalException).errorCode)
        assertEquals("2", error.userInfo["puzzle_count"])
    }

    @Test
    fun ignoresWarningLevel() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.WARNING,
            category = LogCategory.PUZZLES,
            eventName = "puzzle_load_failed",
            message = "load failed",
            metadata = emptyMap(),
        )
        assertNull(FirebaseCrashlyticsEventMapping.nonFatalError(entry, appVersion = null))
    }

    @Test
    fun ignoresUnmappedErrors() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.ERROR,
            category = LogCategory.PUZZLES,
            eventName = "ipdb_import_failed",
            message = "import failed",
            metadata = emptyMap(),
        )
        assertNull(FirebaseCrashlyticsEventMapping.nonFatalError(entry, appVersion = null))
    }
}
