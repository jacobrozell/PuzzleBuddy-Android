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
