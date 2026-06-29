package com.jacobrozell.puzzlebuddy.domain.surface

/**
 * Android release-surface gates mirroring iOS [ProductService] for 1.0.0 lean local-first ship.
 */
object ProductSurface {
    const val LEAN_VERSION = "1.0.0"

    val isLoginEnabled: Boolean = false
    val isCloudSyncEnabled: Boolean = false
    val isBarcodeScanEnabled: Boolean = true
    val isShoppingModeEnabled: Boolean = true
    val isIPDbImportEnabled: Boolean = true
    val isBarcodeLookupEnabled: Boolean = false

    val rootTabs: List<RootTab> = listOf(RootTab.PUZZLES, RootTab.STATS, RootTab.SETTINGS)
}

enum class RootTab(val route: String, val label: String) {
    PUZZLES("puzzles", "Puzzles"),
    STATS("stats", "Stats"),
    SETTINGS("settings", "Settings"),
}
