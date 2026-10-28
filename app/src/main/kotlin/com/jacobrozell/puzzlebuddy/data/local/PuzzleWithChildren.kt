package com.jacobrozell.puzzlebuddy.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleCompletionEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzlePhotoEntity

data class PuzzleWithChildren(
    @Embedded val puzzle: PuzzleEntity,
    @Relation(parentColumn = "id", entityColumn = "puzzleId")
    val photos: List<PuzzlePhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "puzzleId")
    val completions: List<PuzzleCompletionEntity>,
)
