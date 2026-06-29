package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.BuildConfig
import com.jacobrozell.puzzlebuddy.support.FirebaseBootstrap
import java.util.Date

interface AppLogger {
    fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String> = emptyMap(),
    )
}

fun AppLogger.debug(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
) = log(LogLevel.DEBUG, category, eventName, message, metadata)

fun AppLogger.info(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
) = log(LogLevel.INFO, category, eventName, message, metadata)

fun AppLogger.warning(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
) = log(LogLevel.WARNING, category, eventName, message, metadata)

fun AppLogger.error(
    category: LogCategory,
    eventName: String,
    message: String,
    metadata: Map<String, String> = emptyMap(),
) = log(LogLevel.ERROR, category, eventName, message, metadata)

class DefaultAppLogger(
    private val minimumLevel: LogLevel,
    private val sink: LogSink,
) : AppLogger {
    override fun log(
        level: LogLevel,
        category: LogCategory,
        eventName: String,
        message: String,
        metadata: Map<String, String>,
    ) {
        if (!level.atLeast(minimumLevel)) return
        val sanitized = AnalyticsMetadataKeys.withoutPersonalData(metadata)
        sink.write(
            LogEntry(
                timestamp = Date(),
                level = level,
                category = category,
                eventName = eventName,
                message = message,
                metadata = sanitized,
            ),
        )
    }

    companion object {
        fun create(firebaseBootstrap: FirebaseBootstrap, appVersion: String): DefaultAppLogger {
            val sink = CompositeLogSink(
                listOf(
                    ConsoleLogSink(),
                    FirebaseAnalyticsLogSink(firebaseBootstrap, appVersion),
                    FirebaseCrashlyticsLogSink(firebaseBootstrap, appVersion),
                ),
            )
            val minimum = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
            return DefaultAppLogger(minimum, sink)
        }
    }
}
