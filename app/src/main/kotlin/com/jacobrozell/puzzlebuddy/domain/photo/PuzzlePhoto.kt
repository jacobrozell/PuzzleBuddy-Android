package com.jacobrozell.puzzlebuddy.domain.photo

import java.time.Instant
import java.util.UUID

const val MAX_PHOTOS = 5

data class PuzzlePhoto(
    val id: String = UUID.randomUUID().toString(),
    val sortOrder: Int,
    val fileName: String? = null,
    val createdAt: Instant,
)
