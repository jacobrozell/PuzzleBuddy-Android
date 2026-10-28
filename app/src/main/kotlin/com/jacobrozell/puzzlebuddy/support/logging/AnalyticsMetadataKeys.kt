package com.jacobrozell.puzzlebuddy.support.logging

object AnalyticsMetadataKeys {
    private val blockedPersonalDataKeys = setOf(
        "email",
        "uid",
        "password",
        "token",
        "name",
        "displayName",
        "barcode",
        "puzzle_title",
        "puzzle_name",
        "notes",
    )

    val firebaseParameters = setOf(
        "app_version",
        "log_category",
        "puzzle_count",
        "puzzle_status",
        "completion_number",
        "completion_count",
        "import_policy",
        "format",
        "add_source",
        "piece_count_bucket",
        "has_photo",
        "photo_count",
        "status_from",
        "status_to",
        "scan_context",
        "scan_result",
        "tab",
        "entry_point",
        "page_index",
        "puzzle_type",
        "difficulty",
        "rating_bucket",
        "has_missing_pieces",
        "count_wishlist",
        "count_todo",
        "count_in_progress",
        "count_completed",
        "count_abandoned",
        "collection_size_bucket",
        "completed_count_bucket",
        "days_since_last_open_bucket",
        "milestone_id",
        "reason",
        "duration_bucket",
    )

    val crashlyticsParameters = setOf(
        "app_version",
        "log_category",
        "event_name",
        "puzzle_count",
        "puzzle_status",
        "format",
    )

    fun withoutPersonalData(metadata: Map<String, String>): Map<String, String> =
        metadata.filterKeys { key -> !blockedPersonalDataKeys.contains(key.lowercase()) }
}
