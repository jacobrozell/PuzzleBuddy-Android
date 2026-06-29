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
        "format",
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
