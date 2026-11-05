package com.jacobrozell.puzzlebuddy.data.repository

import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

/** Remembers barcode → title/brand/pieces from saved puzzles for offline quick add. */
interface PuzzleBarcodeRecorder {
    suspend fun storeFromPuzzle(puzzle: Puzzle)
}

/** Bundled demo cover photos, by asset name without the `.jpg` suffix. */
fun interface DemoPhotoSource {
    fun bytes(assetName: String): ByteArray?
}

/** Pushes coarse collection user properties to Analytics after the collection changes. */
interface CollectionAnalyticsSync {
    suspend fun syncCollection(puzzles: List<Puzzle>, onboardingComplete: Boolean? = null)
}
