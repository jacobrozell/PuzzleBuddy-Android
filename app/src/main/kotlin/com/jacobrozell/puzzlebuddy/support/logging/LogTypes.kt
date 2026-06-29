package com.jacobrozell.puzzlebuddy.support.logging

import java.util.Date

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    ;

    fun atLeast(minimum: LogLevel): Boolean = ordinal >= minimum.ordinal
}

enum class LogCategory(val rawValue: String) {
    APP("app"),
    AUTH("auth"),
    PUZZLES("puzzles"),
    UI("ui"),
}

data class LogEntry(
    val timestamp: Date,
    val level: LogLevel,
    val category: LogCategory,
    val eventName: String,
    val message: String,
    val metadata: Map<String, String>,
)
