package com.jacobrozell.puzzlebuddy.data.repository

import com.jacobrozell.puzzlebuddy.data.local.dao.FriendDao
import com.jacobrozell.puzzlebuddy.data.local.toDomain
import com.jacobrozell.puzzlebuddy.data.local.toEntity
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.loan.FriendSemantics
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDao,
    private val logger: AppLogger,
    private val clock: Clock,
) {
    fun observeFriends(): Flow<List<Friend>> =
        friendDao.observeAll().map { rows ->
            rows.map { it.toDomain() }.sortedBy { it.displayName.lowercase() }
        }

    suspend fun friend(id: String): Friend? = friendDao.findById(id)?.toDomain()

    suspend fun findOrCreate(displayName: String, isDemo: Boolean): Friend {
        val normalized = FriendSemantics.normalizedDisplayName(displayName)
            ?: throw FriendStoreException.EmptyDisplayName()
        friendDao.findByNameIgnoreCase(normalized)?.let { return it.toDomain() }
        val now = Instant.now(clock)
        val created = Friend.create(displayName = normalized, createdAt = now, isDemo = isDemo)
        friendDao.upsert(created.toEntity())
        logger.info(
            LogCategory.PUZZLES,
            eventName = "friend_created",
            message = "Friend created.",
        )
        return created
    }

    suspend fun upsert(friend: Friend) {
        val normalized = FriendSemantics.normalizedDisplayName(friend.displayName)
            ?: throw FriendStoreException.EmptyDisplayName()
        friendDao.upsert(
            friend.copy(
                displayName = normalized,
                updatedAt = Instant.now(clock),
            ).toEntity(),
        )
    }

    suspend fun delete(friend: Friend, loanReferenceCount: Int) {
        if (loanReferenceCount > 0) throw FriendStoreException.ReferencedByLoans(loanReferenceCount)
        friendDao.deleteById(friend.id)
    }

    suspend fun removeDemoFriends(keepingIds: Set<String>) {
        friendDao.demoFriends().forEach { row ->
            if (row.id !in keepingIds) friendDao.deleteById(row.id)
        }
    }

    suspend fun deleteAll() {
        friendDao.deleteAll()
    }
}
