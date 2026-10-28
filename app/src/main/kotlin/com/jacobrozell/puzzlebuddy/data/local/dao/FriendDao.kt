package com.jacobrozell.puzzlebuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacobrozell.puzzlebuddy.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends")
    suspend fun all(): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE displayName = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FriendEntity)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()

    @Query("SELECT * FROM friends WHERE isDemo = 1")
    suspend fun demoFriends(): List<FriendEntity>
}
