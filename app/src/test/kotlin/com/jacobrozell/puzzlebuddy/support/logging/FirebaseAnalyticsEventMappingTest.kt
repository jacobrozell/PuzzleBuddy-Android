package com.jacobrozell.puzzlebuddy.support.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class FirebaseAnalyticsEventMappingTest {
    @Test
    fun mapsBootstrapToAppOpen() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.APP,
            eventName = "app_bootstrap_ready",
            message = "ready",
            metadata = mapOf("puzzle_count" to "3"),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = "1.0.0")
        assertEquals("app_open", event?.name)
        assertEquals("3", event?.parameters?.get("puzzle_count"))
    }

    @Test
    fun blocksPersonalMetadataKeys() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.PUZZLES,
            eventName = "puzzle_added",
            message = "added",
            metadata = mapOf(
                "puzzle_status" to "To-Do",
                "barcode" to "secret",
                "name" to "Winter Lights",
            ),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = null)
        assertEquals("puzzle_added", event?.name)
        assertNull(event?.parameters?.get("barcode"))
        assertNull(event?.parameters?.get("name"))
        assertEquals("To-Do", event?.parameters?.get("puzzle_status"))
    }

    @Test
    fun mapsShoppingScanEvents() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.PUZZLES,
            eventName = "shopping_scan_match",
            message = "match",
            metadata = emptyMap(),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = "1.0.0")
        assertEquals("shopping_scan_match", event?.name)
    }

    @Test
    fun mapsTabSelectedEvent() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.UI,
            eventName = "tab_selected",
            message = "tab",
            metadata = mapOf("tab" to "stats"),
        )
        val event = FirebaseAnalyticsEventMapping.map(entry, appVersion = "1.0.0")
        assertEquals("tab_selected", event?.name)
        assertEquals("stats", event?.parameters?.get("tab"))
    }

    @Test
    fun mapsSessionSnapshotAndMilestone() {
        val snapshot = FirebaseAnalyticsEventMapping.map(
            LogEntry(
                timestamp = Date(),
                level = LogLevel.INFO,
                category = LogCategory.APP,
                eventName = "session_snapshot",
                message = "snapshot",
                metadata = mapOf(
                    "collection_size_bucket" to "6_20",
                    "days_since_last_open_bucket" to "2_7",
                ),
            ),
            appVersion = "1.1.0",
        )
        assertEquals("session_snapshot", snapshot?.name)
        assertEquals("6_20", snapshot?.parameters?.get("collection_size_bucket"))

        val milestone = FirebaseAnalyticsEventMapping.map(
            LogEntry(
                timestamp = Date(),
                level = LogLevel.INFO,
                category = LogCategory.APP,
                eventName = "milestone_reached",
                message = "milestone",
                metadata = mapOf("milestone_id" to "first_puzzle"),
            ),
            appVersion = "1.1.0",
        )
        assertEquals("milestone_reached", milestone?.name)
        assertEquals("first_puzzle", milestone?.parameters?.get("milestone_id"))
    }

    @Test
    fun mapsDeferredIosElevenEvents() {
        listOf(
            "puzzle_completion_undone",
            "whats_new_shown",
            "whats_new_dismissed",
        ).forEach { eventName ->
            val event = FirebaseAnalyticsEventMapping.map(
                LogEntry(
                    timestamp = Date(),
                    level = LogLevel.INFO,
                    category = LogCategory.APP,
                    eventName = eventName,
                    message = eventName,
                    metadata = emptyMap(),
                ),
                appVersion = "1.1.0",
            )
            assertEquals(eventName, event?.name)
        }
    }

    @Test
    fun ignoresUnmappedEvents() {
        val entry = LogEntry(
            timestamp = Date(),
            level = LogLevel.INFO,
            category = LogCategory.UI,
            eventName = "barcode_lookup_succeeded",
            message = "lookup",
            metadata = emptyMap(),
        )
        assertNull(FirebaseAnalyticsEventMapping.map(entry, appVersion = null))
    }
}
