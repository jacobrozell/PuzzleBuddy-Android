package com.jacobrozell.puzzlebuddy.support.logging

data class FirebaseAnalyticsEvent(
    val name: String,
    val parameters: Map<String, String>,
)

object FirebaseAnalyticsEventMapping {
    private val allowlistedEvents = setOf(
        "app_bootstrap_ready",
        "onboarding_completed",
        "puzzle_list_refreshed",
        "puzzle_added",
        "puzzle_updated",
        "puzzle_deleted",
        "puzzle_import_completed",
        "puzzle_backup_restored",
        "puzzle_load_failed",
        "puzzle_redo_started",
        "puzzle_completion_recorded",
        "settings_collection_exported",
        "shopping_scan_match",
        "shopping_scan_no_match",
    )

    private val firebaseNameOverrides = mapOf(
        "app_bootstrap_ready" to "app_open",
    )

    fun map(entry: LogEntry, appVersion: String?): FirebaseAnalyticsEvent? {
        if (!allowlistedEvents.contains(entry.eventName)) return null
        val parameters = FirebaseMetadataSanitizer
            .sanitize(entry.metadata, AnalyticsMetadataKeys.firebaseParameters)
            .toMutableMap()
        if (!appVersion.isNullOrBlank()) {
            parameters["app_version"] = appVersion
        }
        parameters["log_category"] = entry.category.rawValue
        val firebaseName = firebaseNameOverrides[entry.eventName] ?: entry.eventName
        return FirebaseAnalyticsEvent(firebaseName, parameters)
    }
}
