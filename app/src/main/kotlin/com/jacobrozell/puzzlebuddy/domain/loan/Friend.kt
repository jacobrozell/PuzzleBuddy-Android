package com.jacobrozell.puzzlebuddy.domain.loan

import java.time.Instant
import java.util.UUID

data class Friend(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val remoteId: String? = null,
    val isDemo: Boolean = false,
) {
    companion object {
        fun create(
            id: String = UUID.randomUUID().toString(),
            displayName: String,
            createdAt: Instant,
            updatedAt: Instant = createdAt,
            remoteId: String? = null,
            isDemo: Boolean = false,
        ): Friend = Friend(
            id = id,
            displayName = FriendSemantics.normalizedDisplayName(displayName).orEmpty(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            remoteId = remoteId,
            isDemo = isDemo,
        )
    }
}

object FriendSemantics {
    const val DISPLAY_NAME_MAX_LENGTH = 80

    fun normalizedDisplayName(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.take(DISPLAY_NAME_MAX_LENGTH)
    }
}
