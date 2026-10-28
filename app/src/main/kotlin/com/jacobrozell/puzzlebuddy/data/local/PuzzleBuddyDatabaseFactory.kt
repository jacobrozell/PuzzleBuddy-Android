package com.jacobrozell.puzzlebuddy.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jacobrozell.puzzlebuddy.support.TelemetryRuntime
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.error
import com.jacobrozell.puzzlebuddy.support.logging.warning

object PuzzleBuddyDatabaseFactory {
    private const val DB_NAME = "puzzle_buddy.db"

    fun create(
        context: Context,
        logger: AppLogger,
        store: AnalyticsKeyValueStore,
    ): PuzzleBuddyDatabase {
        if (TelemetryRuntime.isRunningUnderInstrumentedTest()) {
            StoreRuntime.isRunningInEphemeralStore = false
            return inMemory(context)
        }

        return try {
            val database = openPersistent(context, logger, store)
            StoreRuntime.isRunningInEphemeralStore = false
            database
        } catch (loadError: Exception) {
            logger.warning(
                LogCategory.PUZZLES,
                eventName = "model_container_load_failed",
                message = loadError.message ?: "Database open failed.",
            )
            try {
                context.deleteDatabase(DB_NAME)
                val reopened = openPersistent(context, logger, store)
                markStoreReset(logger, store)
                StoreRuntime.isRunningInEphemeralStore = false
                reopened
            } catch (resetError: Exception) {
                logger.error(
                    LogCategory.PUZZLES,
                    eventName = "model_container_reset_failed",
                    message = resetError.message ?: "Database reset failed.",
                )
                logger.error(
                    LogCategory.PUZZLES,
                    eventName = "model_container_ephemeral_fallback",
                    message = "Running with an in-memory collection.",
                )
                StoreRuntime.isRunningInEphemeralStore = true
                inMemory(context)
            }
        }
    }

    private fun openPersistent(
        context: Context,
        logger: AppLogger,
        store: AnalyticsKeyValueStore,
    ): PuzzleBuddyDatabase {
        val database = Room.databaseBuilder(context, PuzzleBuddyDatabase::class.java, DB_NAME)
            .fallbackToDestructiveMigrationFrom(true, 1)
            .addCallback(resetCallback(logger, store))
            .build()
        database.openHelper.writableDatabase
        return database
    }

    private fun inMemory(context: Context): PuzzleBuddyDatabase =
        Room.inMemoryDatabaseBuilder(context, PuzzleBuddyDatabase::class.java)
            .fallbackToDestructiveMigrationFrom(true, 1)
            .build()

    private fun resetCallback(logger: AppLogger, store: AnalyticsKeyValueStore) =
        object : RoomDatabase.Callback() {
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                markStoreReset(logger, store)
            }
        }

    private fun markStoreReset(logger: AppLogger, store: AnalyticsKeyValueStore) {
        logger.error(
            LogCategory.PUZZLES,
            eventName = "model_container_store_reset",
            message = "The saved collection could not be read and was reset.",
        )
        store.putBoolean(StoreRuntime.STORE_WAS_RESET_NOTICE, true)
    }
}
