package com.jacobrozell.puzzlebuddy.support.review

import com.jacobrozell.puzzlebuddy.support.logging.InMemoryAnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports iOS `StoreReviewPromptTests` for consume and the settings link. The Play review call stays out of these tests. */
class StoreReviewPromptTest {
    private fun prompt(underTest: Boolean = false): Pair<StoreReviewPrompt, RecordingAppLogger> {
        val logger = RecordingAppLogger()
        val prompt = StoreReviewPrompt(InMemoryAnalyticsKeyValueStore(), logger) { underTest }
        return prompt to logger
    }

    @Test
    fun consumeAllowsOneRequest() {
        val (prompt, logger) = prompt()
        assertTrue(prompt.consume(StoreReviewPrompt.Reason.BARCODE_SCAN))
        assertTrue(prompt.hasRequested())
        assertEquals("store_review_requested", logger.entries.single().eventName)
        assertEquals("barcode_scan", logger.entries.single().metadata["entry_point"])
        assertFalse(prompt.consume(StoreReviewPrompt.Reason.PUZZLE_EDITED))
        assertEquals(1, logger.entries.size)
    }

    @Test
    fun consumeSkipsDuringUiTesting() {
        val (prompt, logger) = prompt(underTest = true)
        assertFalse(prompt.consume(StoreReviewPrompt.Reason.BARCODE_SCAN))
        assertFalse(prompt.hasRequested())
        assertTrue(logger.entries.isEmpty())
    }

    @Test
    fun settingsLinkBlocksLaterPrompt() {
        val (prompt, logger) = prompt()
        prompt.recordSettingsLinkOpened()
        assertTrue(prompt.hasRequested())
        assertEquals("store_review_link_opened", logger.entries.single().eventName)
        assertEquals("settings_link", logger.entries.single().metadata["entry_point"])
        assertFalse(prompt.consume(StoreReviewPrompt.Reason.BARCODE_SCAN))
    }

    @Test
    fun settingsLinkUnderTestSetsFlagWithoutLogging() {
        val (prompt, logger) = prompt(underTest = true)
        prompt.recordSettingsLinkOpened()
        assertTrue(prompt.hasRequested())
        assertTrue(logger.entries.isEmpty())
    }
}
