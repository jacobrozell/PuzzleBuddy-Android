package com.jacobrozell.puzzlebuddy.support.logging

object FirebaseCrashlyticsEventMapping {
    private val nonFatalEvents = setOf(
        "puzzle_load_failed",
        "model_container_load_failed",
        "model_container_reset_failed",
        "demo_data_seed_failed",
        "model_container_ephemeral_fallback",
    )

    private val eventCodes = mapOf(
        "puzzle_load_failed" to 2001,
        "model_container_load_failed" to 2002,
        "model_container_reset_failed" to 2003,
        "demo_data_seed_failed" to 2004,
        "model_container_ephemeral_fallback" to 2005,
    )

    fun nonFatalError(entry: LogEntry, appVersion: String?): Throwable? {
        if (!entry.level.atLeast(LogLevel.ERROR)) return null
        if (!nonFatalEvents.contains(entry.eventName)) return null
        val code = eventCodes[entry.eventName] ?: return null
        val userInfo = FirebaseMetadataSanitizer
            .sanitize(entry.metadata, AnalyticsMetadataKeys.crashlyticsParameters)
            .toMutableMap()
        userInfo["log_category"] = entry.category.rawValue
        userInfo["event_name"] = entry.eventName
        if (!appVersion.isNullOrBlank()) {
            userInfo["app_version"] = appVersion
        }
        return LoggerNonFatalException(code, userInfo)
    }
}

class LoggerNonFatalException(
    val errorCode: Int,
    val userInfo: Map<String, String>,
) : Exception("PuzzleBuddy logger error $errorCode")
