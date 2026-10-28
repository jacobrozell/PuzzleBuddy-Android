package com.jacobrozell.puzzlebuddy.data.local

object StoreRuntime {
    const val STORE_WAS_RESET_NOTICE = "PuzzleBuddy.StoreWasResetNoticePending"
    const val EPHEMERAL_STORE_BANNER_DISMISSED = "PuzzleBuddy.EphemeralStoreBannerDismissed"

    @Volatile
    var isRunningInEphemeralStore: Boolean = false
}
