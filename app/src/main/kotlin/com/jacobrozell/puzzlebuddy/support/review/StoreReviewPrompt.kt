package com.jacobrozell.puzzlebuddy.support.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.jacobrozell.puzzlebuddy.support.TelemetryRuntime
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreReviewPrompt @Inject constructor(
    private val store: AnalyticsKeyValueStore,
    private val logger: AppLogger,
) {
    constructor(
        store: AnalyticsKeyValueStore,
        logger: AppLogger,
        isUnderTest: () -> Boolean,
    ) : this(store, logger) {
        this.isUnderTest = isUnderTest
    }

    private var isUnderTest: () -> Boolean = { TelemetryRuntime.isRunningUnderInstrumentedTest() }

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    enum class Reason(val entryPoint: String) {
        BARCODE_SCAN("barcode_scan"),
        PUZZLE_EDITED("puzzle_edited"),
        SETTINGS_LINK("settings_link"),
    }

    fun consume(reason: Reason): Boolean {
        if (isUnderTest()) return false
        if (store.getBoolean(STORAGE_KEY)) return false
        store.putBoolean(STORAGE_KEY, true)
        logger.info(
            LogCategory.UI,
            eventName = "store_review_requested",
            message = "Requested the Play review prompt.",
            metadata = mapOf("entry_point" to reason.entryPoint),
        )
        return true
    }

    fun recordSettingsLinkOpened() {
        store.putBoolean(STORAGE_KEY, true)
        if (isUnderTest()) return
        logger.info(
            LogCategory.UI,
            eventName = "store_review_link_opened",
            message = "Opened the Play listing from Settings.",
            metadata = mapOf("entry_point" to Reason.SETTINGS_LINK.entryPoint),
        )
    }

    fun requestIfEligible(activity: Activity, reason: Reason) {
        if (!consume(reason)) return
        scope.launch {
            try {
                delay(1_500)
                val manager = ReviewManagerFactory.create(activity)
                val info = manager.requestReviewFlow().await()
                manager.launchReviewFlow(activity, info).await()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
    }

    fun hasRequested(): Boolean = store.getBoolean(STORAGE_KEY)

    companion object {
        const val STORAGE_KEY = "PuzzleBuddy.HasRequestedStoreReview"
    }
}
