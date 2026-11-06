package com.jacobrozell.puzzlebuddy.domain.surface

import com.jacobrozell.puzzlebuddy.support.logging.InMemoryAnalyticsKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports iOS `WhatsNewPromptTests` (tag 1.1.0), without the marketing-capture case or the iPhone Duo item. */
class WhatsNewPromptTest {
    @Test
    fun presentsForUpgraderWhoHasNotSeenNotes() {
        assertTrue(
            WhatsNewPrompt.shouldPresent(
                onboardingComplete = true,
                seenVersion = null,
                isUnderTest = false,
            ),
        )
    }

    @Test
    fun skipsFirstRunUntilOnboardingFinishes() {
        assertFalse(
            WhatsNewPrompt.shouldPresent(
                onboardingComplete = false,
                seenVersion = null,
                isUnderTest = false,
            ),
        )
    }

    @Test
    fun skipsAfterNotesHaveBeenSeen() {
        val store = InMemoryAnalyticsKeyValueStore()
        store.putString(WhatsNewPrompt.STORAGE_KEY, WhatsNewPrompt.NOTES_VERSION)
        assertFalse(
            WhatsNewPrompt.shouldPresent(
                onboardingComplete = true,
                seenVersion = store.getStringOrNull(WhatsNewPrompt.STORAGE_KEY),
                isUnderTest = false,
            ),
        )
    }

    @Test
    fun skipsDuringUiTesting() {
        assertFalse(
            WhatsNewPrompt.shouldPresent(
                onboardingComplete = true,
                seenVersion = null,
                isUnderTest = true,
            ),
        )
    }

    @Test
    fun versionCompareTreatsMissingAsOlder() {
        assertTrue(WhatsNewPrompt.isVersion(null, olderThan = "1.1.0"))
        assertTrue(WhatsNewPrompt.isVersion("1.0.0", olderThan = "1.1.0"))
        assertFalse(WhatsNewPrompt.isVersion("1.1.0", olderThan = "1.1.0"))
        assertFalse(WhatsNewPrompt.isVersion("1.2.0", olderThan = "1.1.0"))
        assertEquals(0, WhatsNewPrompt.compareVersions("1.1.0", "1.1.0"))
        assertTrue(WhatsNewPrompt.compareVersions("1.0", "1.0.1") < 0)
    }

    @Test
    fun copyMentionsHistoryLoanTimerAndRedo() {
        val joined = WhatsNewCopy.items.joinToString(" ") { "${it.title} ${it.message}" }
        assertTrue(joined.contains("finish", ignoreCase = true))
        assertTrue(joined.contains("loan", ignoreCase = true))
        assertTrue(joined.contains("timer", ignoreCase = true))
        assertTrue(joined.contains("again", ignoreCase = true))
        assertEquals(4, WhatsNewCopy.items.size)
        assertEquals(listOf("history", "loan", "timer", "redo"), WhatsNewCopy.items.map { it.id })
    }
}
