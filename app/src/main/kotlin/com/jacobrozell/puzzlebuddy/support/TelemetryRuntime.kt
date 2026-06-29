package com.jacobrozell.puzzlebuddy.support

import com.jacobrozell.puzzlebuddy.BuildConfig

object TelemetryRuntime {
    fun isRunningUnderInstrumentedTest(): Boolean =
        try {
            Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            val registry = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            registry.getMethod("getInstrumentation").invoke(null) != null
        } catch (_: Throwable) {
            false
        }

    /** Matches Puzzle Buddy iOS: Release-only remote telemetry; off under instrumented tests. */
    fun isRemoteTelemetryCollectionEnabled(): Boolean =
        !isRunningUnderInstrumentedTest() && !BuildConfig.DEBUG
}
