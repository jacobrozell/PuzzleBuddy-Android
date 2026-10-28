package com.jacobrozell.puzzlebuddy.data.local

import com.jacobrozell.puzzlebuddy.data.local.entity.FriendEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleCompletionEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzlePhotoEntity
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import java.time.Instant

fun PuzzleWithChildren.toDomain(friendsById: Map<String, Friend>): Puzzle {
    val photos = photos.map { it.toDomain() }.sortedBy { it.sortOrder }
    val completions = completions.map { it.toDomain() }.sortedBy { it.completionNumber }
    val friendName = puzzle.loanedToFriendId?.let { friendsById[it]?.displayName }
    return puzzle.toDomain(
        photos = photos,
        completions = completions,
        loanedToDisplayName = friendName,
        timesCompleted = maxOf(puzzle.timesCompleted, completions.size),
    )
}

fun PuzzleEntity.toDomain(
    photos: List<PuzzlePhoto> = emptyList(),
    completions: List<PuzzleCompletion> = emptyList(),
    loanedToDisplayName: String? = null,
    timesCompleted: Int = this.timesCompleted,
): Puzzle = Puzzle(
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
    startDate = startDateEpochMillis?.let(Instant::ofEpochMilli),
    status = PuzzleStatus.fromRaw(status),
    hasMissingPieces = hasMissingPieces,
    notes = notes,
    source = source,
    purchaseLocation = purchaseLocation,
    releaseYear = releaseYear,
    puzzleType = PuzzleType.fromRaw(puzzleType),
    material = PuzzleMaterial.fromRaw(material),
    disposition = PuzzleDisposition.fromRaw(disposition),
    progressPercent = progressPercent,
    purchasePrice = purchasePrice,
    purchaseCurrencyCode = purchaseCurrencyCode,
    puzzleShape = PuzzleShape.fromRaw(puzzleShape),
    cutType = PuzzleCutType.fromRaw(cutType),
    dimensionsText = dimensionsText,
    timesCompleted = timesCompleted,
    photos = photos,
    completions = completions,
    isDemo = isDemo,
    barcode = barcode,
    tags = tagsJson,
    isOnLoan = isOnLoan,
    loanedToFriendId = loanedToFriendId,
    loanedToDisplayName = loanedToDisplayName,
    loanedAt = loanedAtEpochMillis?.let(Instant::ofEpochMilli),
    dueBackDate = dueBackEpochMillis?.let(Instant::ofEpochMilli),
    lastLoanNudgeAt = lastLoanNudgeAtEpochMillis?.let(Instant::ofEpochMilli),
    timerAccumulatedSeconds = timerAccumulatedSeconds,
    timerRunningSince = timerRunningSinceEpochMillis?.let(Instant::ofEpochMilli),
)

fun Puzzle.toEntity(): PuzzleEntity = PuzzleEntity(
    id = id,
    name = name.trim(),
    pieces = pieces,
    rating = rating.value,
    difficulty = difficulty.raw,
    estimatedTimeHours = estimatedTimeSpent?.hours,
    estimatedTimeMinutes = estimatedTimeSpent?.minutes,
    completionDateEpochMillis = completionDate.toEpochMilli(),
    startDateEpochMillis = startDate?.toEpochMilli(),
    status = status.raw,
    hasMissingPieces = hasMissingPieces,
    notes = notes?.trim()?.ifEmpty { null },
    source = source?.trim()?.ifEmpty { null },
    purchaseLocation = purchaseLocation?.trim()?.ifEmpty { null },
    releaseYear = releaseYear,
    puzzleType = puzzleType.raw,
    material = material.raw,
    disposition = disposition.raw,
    progressPercent = progressPercent,
    purchasePrice = purchasePrice,
    purchaseCurrencyCode = purchaseCurrencyCode,
    puzzleShape = puzzleShape.raw,
    cutType = cutType.raw,
    dimensionsText = dimensionsText,
    timesCompleted = timesCompleted,
    isDemo = isDemo,
    barcode = BarcodeNormalizer.normalize(barcode),
    tagsJson = PuzzleTagSemantics.sanitizedTags(tags),
    isOnLoan = isOnLoan,
    loanedToFriendId = loanedToFriendId,
    loanedAtEpochMillis = loanedAt?.toEpochMilli(),
    dueBackEpochMillis = dueBackDate?.toEpochMilli(),
    lastLoanNudgeAtEpochMillis = lastLoanNudgeAt?.toEpochMilli(),
    timerAccumulatedSeconds = timerAccumulatedSeconds.coerceAtLeast(0),
    timerRunningSinceEpochMillis = timerRunningSince?.toEpochMilli(),
)

fun PuzzlePhoto.toEntity(puzzleId: String): PuzzlePhotoEntity = PuzzlePhotoEntity(
    id = id,
    puzzleId = puzzleId,
    sortOrder = sortOrder,
    fileName = fileName,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

fun PuzzlePhotoEntity.toDomain(): PuzzlePhoto = PuzzlePhoto(
    id = id,
    sortOrder = sortOrder,
    fileName = fileName,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

fun PuzzleCompletion.toEntity(puzzleId: String): PuzzleCompletionEntity = PuzzleCompletionEntity(
    id = id,
    puzzleId = puzzleId,
    completionNumber = completionNumber,
    startedAtEpochMillis = startedAt?.toEpochMilli(),
    completedAtEpochMillis = completedAt.toEpochMilli(),
    timeSpentHours = timeSpentHours,
    timeSpentMinutes = timeSpentMinutes,
    rating = rating,
)

fun PuzzleCompletionEntity.toDomain(): PuzzleCompletion = PuzzleCompletion(
    id = id,
    completionNumber = completionNumber,
    startedAt = startedAtEpochMillis?.let(Instant::ofEpochMilli),
    completedAt = Instant.ofEpochMilli(completedAtEpochMillis),
    timeSpentHours = timeSpentHours,
    timeSpentMinutes = timeSpentMinutes,
    rating = rating,
)

fun Friend.toEntity(): FriendEntity = FriendEntity(
    id = id,
    displayName = displayName,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    remoteId = remoteId,
    isDemo = isDemo,
)

fun FriendEntity.toDomain(): Friend = Friend(
    id = id,
    displayName = displayName,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    remoteId = remoteId,
    isDemo = isDemo,
)
