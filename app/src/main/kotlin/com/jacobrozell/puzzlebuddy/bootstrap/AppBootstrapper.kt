package com.jacobrozell.puzzlebuddy.bootstrap

import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBootstrapper @Inject constructor(
    private val logger: AppLogger,
    private val repository: PuzzleRepository,
    private val barcodeMetadataCache: BarcodeMetadataCache,
) {
    private var didLogReady = false

    suspend fun onLaunch() {
        if (didLogReady) return
        didLogReady = true
        val puzzles = repository.allPuzzles()
        barcodeMetadataCache.warmCache(puzzles)
        logger.info(
            LogCategory.APP,
            eventName = "app_bootstrap_ready",
            message = "Puzzle Buddy launched.",
            metadata = mapOf("puzzle_count" to puzzles.size.toString()),
        )
    }
}
