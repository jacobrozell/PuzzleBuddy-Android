package com.jacobrozell.puzzlebuddy.data.local

import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import java.time.Instant

fun PuzzleEntity.toDomain(): Puzzle = Puzzle(
    id = id,
    name = name,
    pieces = pieces,
    rating = PuzzleRating.fromValue(rating),
    difficulty = PuzzleDifficulty.fromRaw(difficulty),
    estimatedTimeSpent = if (estimatedTimeHours != null || estimatedTimeMinutes != null) {
        PuzzleTime(estimatedTimeHours, estimatedTimeMinutes)
    } else {
        null
    },
    completionDate = Instant.ofEpochMilli(completionDateEpochMillis),
    status = PuzzleStatus.fromRaw(status),
    hasMissingPieces = hasMissingPieces,
    notes = notes,
    source = source,
    progressPercent = progressPercent,
    isDemo = isDemo,
    barcode = barcode,
    tags = tagsJson,
    hasImage = imageData != null && imageData.isNotEmpty(),
)

fun Puzzle.toEntity(imageData: ByteArray? = null): PuzzleEntity = PuzzleEntity(
    id = id,
    name = name.trim(),
    pieces = pieces,
    rating = rating.value,
    difficulty = difficulty.raw,
    estimatedTimeHours = estimatedTimeSpent?.hours,
    estimatedTimeMinutes = estimatedTimeSpent?.minutes,
    completionDateEpochMillis = completionDate.toEpochMilli(),
    status = status.raw,
    hasMissingPieces = hasMissingPieces,
    notes = notes?.trim()?.ifEmpty { null },
    source = source?.trim()?.ifEmpty { null },
    progressPercent = progressPercent,
    isDemo = isDemo,
    barcode = BarcodeNormalizer.normalize(barcode),
    tagsJson = PuzzleTagSemantics.sanitizedTags(tags),
    imageData = imageData,
)
