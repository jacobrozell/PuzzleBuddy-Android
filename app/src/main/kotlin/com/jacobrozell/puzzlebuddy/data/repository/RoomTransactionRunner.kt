package com.jacobrozell.puzzlebuddy.data.repository

import androidx.room.withTransaction
import com.jacobrozell.puzzlebuddy.data.local.PuzzleBuddyDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: PuzzleBuddyDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
