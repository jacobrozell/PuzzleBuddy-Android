package com.jacobrozell.puzzlebuddy.support.logging

data class FirebaseAnalyticsEvent(
    val name: String,
    val parameters: Map<String, String>,
)

object FirebaseAnalyticsEventMapping {
    private val allowlistedEvents = setOf(
        "app_bootstrap_ready",
        "onboarding_completed",
        "onboarding_skipped",
        "puzzle_list_refreshed",
        "puzzle_added",
        "puzzle_updated",
        "puzzle_deleted",
        "puzzle_import_completed",
        "puzzle_backup_restored",
        "puzzle_load_failed",
        "puzzle_redo_started",
        "puzzle_completion_recorded",
        "puzzle_status_changed",
        "settings_collection_exported",
        "shopping_scan_match",
        "shopping_scan_no_match",
        "barcode_scan_completed",
        "tab_selected",
        "pick_next_puzzle_selected",
        "demo_data_loaded",
        "demo_data_removed",
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
