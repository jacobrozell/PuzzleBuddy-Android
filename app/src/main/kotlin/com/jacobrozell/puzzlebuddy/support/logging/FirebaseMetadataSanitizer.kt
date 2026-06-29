package com.jacobrozell.puzzlebuddy.support.logging

object FirebaseMetadataSanitizer {
    fun sanitize(metadata: Map<String, String>, allowedKeys: Set<String>): Map<String, String> =
        AnalyticsMetadataKeys.withoutPersonalData(metadata)
            .filterKeys { allowedKeys.contains(it) }
            .mapValues { (_, value) -> value.take(100) }
}
