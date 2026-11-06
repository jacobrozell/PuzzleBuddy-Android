package com.jacobrozell.puzzlebuddy.domain.onboarding

/** Onboarding pages, copied from iOS `OnboardingCopy.swift` (tag 1.1.0). */
object OnboardingCopy {
    data class Page(val title: String, val message: String)

    fun pages(appName: String = "Puzzle Buddy"): List<Page> = listOf(
        Page(
            title = "Welcome to $appName",
            message = "Your personal jigsaw puzzle catalog — track every box on your shelf, offline and private.",
        ),
        Page(
            title = "Shop With Confidence",
            message = "Scan a barcode at the thrift store to check duplicates instantly — no account or internet required.",
        ),
        Page(
            title = "Build Your Collection",
            message = "Log brands, piece counts, tags, and ratings. Track boxes on loan, then spin the dice to pick your next puzzle.",
        ),
        Page(
            title = "Ready to Puzzle?",
            message = "Everything stays on your device. If you tap Complete by accident, you can undo or fix that finish in history.",
        ),
    )
}
