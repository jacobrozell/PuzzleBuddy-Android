package com.jacobrozell.puzzlebuddy.data.local

import android.content.Context
import androidx.room.Room
import com.jacobrozell.puzzlebuddy.support.TelemetryRuntime
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.warning

object PuzzleBuddyDatabaseFactory {
    private const val DB_NAME = "puzzle_buddy.db"

    fun create(context: Context, logger: AppLogger): PuzzleBuddyDatabase {
        if (TelemetryRuntime.isRunningUnderInstrumentedTest()) {
            return inMemory(context)
        }

        return try {
            openPersistent(context)
        } catch (loadError: Exception) {
            logger.warning(
                LogCategory.PUZZLES,
                eventName = "model_container_load_failed",
                message = loadError.message ?: "Database open failed.",
            )
            try {
                context.deleteDatabase(DB_NAME)
                openPersistent(context)
            } catch (resetError: Exception) {
                logger.warning(
                    LogCategory.PUZZLES,
                    eventName = "model_container_reset_failed",
                    message = resetError.message ?: "Database reset failed.",
                )
                inMemory(context)
            }
        }
    }

    private fun openPersistent(context: Context): PuzzleBuddyDatabase {
        val database = Room.databaseBuilder(context, PuzzleBuddyDatabase::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
        database.openHelper.writableDatabase
        return database
    }

    private fun inMemory(context: Context): PuzzleBuddyDatabase =
        Room.inMemoryDatabaseBuilder(context, PuzzleBuddyDatabase::class.java)
            .build()
}
