package com.jacobrozell.puzzlebuddy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "puzzles")
data class PuzzleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pieces: Int?,
    val rating: Double,
    val difficulty: String,
    val estimatedTimeHours: Int?,
    val estimatedTimeMinutes: Int?,
    val completionDateEpochMillis: Long,
    val startDateEpochMillis: Long?,
    val status: String,
    val hasMissingPieces: Boolean,
    val notes: String?,
    val source: String?,
    val purchaseLocation: String?,
    val releaseYear: Int?,
    val puzzleType: String,
    val material: String,
    val disposition: String,
    val progressPercent: Int,
    val purchasePrice: Double?,
    val purchaseCurrencyCode: String?,
    val puzzleShape: String,
    val cutType: String,
    val dimensionsText: String?,
    val timesCompleted: Int,
    val isDemo: Boolean,
    val barcode: String?,
    val tagsJson: List<String>,
    val isOnLoan: Boolean,
    val loanedToFriendId: String?,
    val loanedAtEpochMillis: Long?,
    val dueBackEpochMillis: Long?,
    val lastLoanNudgeAtEpochMillis: Long?,
    val timerAccumulatedSeconds: Long,
    val timerRunningSinceEpochMillis: Long?,
)

@Entity(
    tableName = "puzzle_photos",
    indices = [Index("puzzleId")],
)
data class PuzzlePhotoEntity(
    @PrimaryKey val id: String,
    val puzzleId: String,
    val sortOrder: Int,
    val fileName: String?,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "puzzle_completions",
    indices = [Index("puzzleId")],
)
data class PuzzleCompletionEntity(
    @PrimaryKey val id: String,
    val puzzleId: String,
    val completionNumber: Int,
    val startedAtEpochMillis: Long?,
    val completedAtEpochMillis: Long,
    val timeSpentHours: Int?,
    val timeSpentMinutes: Int?,
    val rating: Double?,
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val remoteId: String?,
    val isDemo: Boolean,
)
