package com.jacobrozell.puzzlebuddy.domain.surface

/**
 * Android release-surface gates mirroring iOS ProductService.
 */
object ProductSurface {
    val isLoginEnabled: Boolean = false
    val isCloudSyncEnabled: Boolean = false
    val isBarcodeScanEnabled: Boolean = true
    val isShoppingModeEnabled: Boolean = true
    val isPickNextEnabled: Boolean = true
    val isFriendsListEnabled: Boolean = false

    /** Off until the Play listing exists; a dead store link fails review. Gates Write a Review. */
    val isPlayListingLive: Boolean = false

    val rootTabs: List<RootTab> = listOf(RootTab.PUZZLES, RootTab.STATS, RootTab.SETTINGS)
}

enum class RootTab(val route: String, val label: String) {
    PUZZLES("puzzles", "Puzzles"),
    STATS("stats", "Stats"),
    SETTINGS("settings", "Settings"),
}
