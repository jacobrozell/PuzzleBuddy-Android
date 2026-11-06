package com.jacobrozell.puzzlebuddy.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports iOS `OnboardingCopyTests` (tag 1.1.0), plus a guard against the removed 1.0 features. */
class OnboardingCopyTest {
    @Test
    fun onboardingHasFourPages() {
        assertEquals(4, OnboardingCopy.pages().size)
    }

    @Test
    fun collectionPageMentionsOnLoan() {
        val page = OnboardingCopy.pages()[2]
        assertEquals("Build Your Collection", page.title)
        assertTrue(page.message.contains("on loan", ignoreCase = true))
    }

    @Test
    fun finalPageMentionsUndoOrHistory() {
        val page = OnboardingCopy.pages().last()
        assertEquals("Ready to Puzzle?", page.title)
        assertTrue(page.message.contains("undo", ignoreCase = true) || page.message.contains("history", ignoreCase = true))
        assertTrue(page.message.contains("complete", ignoreCase = true))
    }

    @Test
    fun welcomePageUsesAppName() {
        val page = OnboardingCopy.pages(appName = "Puzzle Buddy")[0]
        assertTrue(page.title.contains("Puzzle Buddy"))
        assertTrue(page.message.contains("offline", ignoreCase = true))
    }

    @Test
    fun copyNoLongerMentionsLookupOrImport() {
        val text = OnboardingCopy.pages().joinToString(" ") { it.title + " " + it.message }
        assertFalse(text.contains("look up", ignoreCase = true))
        assertFalse(text.contains("IPDb", ignoreCase = true))
    }
}
