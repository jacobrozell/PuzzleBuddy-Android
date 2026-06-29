package com.jacobrozell.puzzlebuddy.data.local.entity

import androidx.room.Entity
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
    val status: String,
    val hasMissingPieces: Boolean,
    val notes: String?,
    val source: String?,
    val progressPercent: Int,
    val isDemo: Boolean,
    val barcode: String?,
    val tagsJson: List<String>,
    val imageData: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PuzzleEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
