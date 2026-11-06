package com.jacobrozell.puzzlebuddy.domain.surface

data class WhatsNewItem(
    val id: String,
    val title: String,
    val message: String,
    val symbolName: String,
)

object WhatsNewCopy {
    const val TITLE = "What's New in 1.1"
    const val SUBTITLE = "A few upgrades for the collection you already keep."
    const val DISMISS_TITLE = "Got it"

    val items: List<WhatsNewItem> = listOf(
        WhatsNewItem(
            id = "history",
            title = "Fix a finish",
            message = "Edit or delete a completion if you tapped Complete by accident.",
            symbolName = "clock.arrow.circlepath",
        ),
        WhatsNewItem(
            id = "loan",
            title = "On loan",
            message = "Track boxes you\u2019ve lent out, and see when they\u2019re overdue.",
            symbolName = "person.fill.checkmark",
        ),
        WhatsNewItem(
            id = "timer",
            title = "Time a puzzle",
            message = "Start and pause a timer on an in-progress puzzle. Only one runs at a time, and it can fill in how long it took.",
            symbolName = "timer",
        ),
        WhatsNewItem(
            id = "redo",
            title = "Puzzle again",
            message = "Start another go without losing the finishes you already logged.",
            symbolName = "arrow.counterclockwise",
        ),
    )
}

object WhatsNewPrompt {
    const val NOTES_VERSION = "1.1.0"
    const val STORAGE_KEY = "PuzzleBuddy.WhatsNewSeenVersion"

    fun shouldPresent(
        onboardingComplete: Boolean,
        seenVersion: String?,
        isUnderTest: Boolean = false,
    ): Boolean {
        if (isUnderTest) return false
        if (!onboardingComplete) return false
        return isVersion(seenVersion, olderThan = NOTES_VERSION)
    }

    fun isVersion(older: String?, olderThan: String): Boolean {
        if (older == null) return true
        return compareVersions(older, olderThan) < 0
    }

    fun compareVersions(lhs: String, rhs: String): Int {
        val left = versionComponents(lhs)
        val right = versionComponents(rhs)
        val count = maxOf(left.size, right.size)
        for (index in 0 until count) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a < b) return -1
            if (a > b) return 1
        }
        return 0
    }

    private fun versionComponents(version: String): List<Int> =
        version.split('.').mapNotNull { it.toIntOrNull() }
}
