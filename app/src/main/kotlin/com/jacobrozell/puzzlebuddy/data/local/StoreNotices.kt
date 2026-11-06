package com.jacobrozell.puzzlebuddy.data.local

import com.jacobrozell.puzzlebuddy.support.TelemetryRuntime
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsKeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The storage warning shown above the puzzle list, from iOS `PuzzleView`. */
enum class StoreNotice(
    val title: String,
    val message: String,
    val dismissLabel: String,
    val accessibilityLabel: String,
) {
    EPHEMERAL(
        title = "Changes won't be saved",
        message = "Puzzle Buddy couldn't open your saved collection. Anything you add will disappear when you close the app.",
        dismissLabel = "Dismiss storage warning",
        accessibilityLabel = "Storage warning. Changes won't be saved between app launches.",
    ),
    RESET(
        title = "Collection was reset",
        message = "We couldn't open your previous collection, so Puzzle Buddy started fresh. New puzzles you add will save normally.",
        dismissLabel = "Dismiss collection reset notice",
        accessibilityLabel = "Notice. Your previous collection could not be opened and was reset. New puzzles will save normally.",
    ),
}

/**
 * Decides which storage notice to show. The in-memory warning wins over the reset notice, and
 * neither shows under instrumented tests, matching iOS.
 */
@Singleton
class StoreNotices(
    private val store: AnalyticsKeyValueStore,
    private val isEphemeral: () -> Boolean,
    private val isUnderTest: () -> Boolean,
) {
    @Inject
    constructor(store: AnalyticsKeyValueStore) : this(
        store = store,
        isEphemeral = { StoreRuntime.isRunningInEphemeralStore },
        isUnderTest = { TelemetryRuntime.isRunningUnderInstrumentedTest() },
    )

    private val current = MutableStateFlow(compute())
    val notice: StateFlow<StoreNotice?> = current.asStateFlow()

    fun dismiss(notice: StoreNotice) {
        when (notice) {
            StoreNotice.EPHEMERAL -> store.putBoolean(StoreRuntime.EPHEMERAL_STORE_BANNER_DISMISSED, true)
            StoreNotice.RESET -> store.putBoolean(StoreRuntime.STORE_WAS_RESET_NOTICE, false)
        }
        current.value = compute()
    }

    private fun compute(): StoreNotice? = when {
        isUnderTest() -> null
        isEphemeral() && !store.getBoolean(StoreRuntime.EPHEMERAL_STORE_BANNER_DISMISSED) -> StoreNotice.EPHEMERAL
        store.getBoolean(StoreRuntime.STORE_WAS_RESET_NOTICE) -> StoreNotice.RESET
        else -> null
    }
}
