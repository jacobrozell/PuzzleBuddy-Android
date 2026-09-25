package com.jacobrozell.puzzlebuddy.support.logging

import java.util.Date

class RecordingAppLogger : AppLogger {
    val entries = mutableListOf<LogEntry>()

    override fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String>,
    ) {
        entries += LogEntry(
            timestamp = Date(),
            level = level,
            category = category,
            eventName = eventName,
            message = message,
            metadata = metadata,
        )
    }
}
