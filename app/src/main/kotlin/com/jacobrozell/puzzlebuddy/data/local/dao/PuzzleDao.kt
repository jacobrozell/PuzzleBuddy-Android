package com.jacobrozell.puzzlebuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jacobrozell.puzzlebuddy.data.local.PuzzleWithChildren
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Transaction
    @Query("SELECT * FROM puzzles ORDER BY name COLLATE NOCASE ASC")
    fun observeAllWithChildren(): Flow<List<PuzzleWithChildren>>

    @Query("SELECT * FROM puzzles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PuzzleEntity>>

    @Transaction
    @Query("SELECT * FROM puzzles WHERE id = :id LIMIT 1")
    suspend fun findWithChildren(id: String): PuzzleWithChildren?

    @Query("SELECT * FROM puzzles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PuzzleEntity?

    @Query("SELECT * FROM puzzles")
    suspend fun allOnce(): List<PuzzleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PuzzleEntity)

    @Query("DELETE FROM puzzles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM puzzles")
    suspend fun deleteAll()

    @Query("SELECT id FROM puzzles WHERE isDemo = 1")
    suspend fun demoIds(): List<String>

    @Query("SELECT COUNT(*) FROM puzzles WHERE isDemo = 1")
    suspend fun countDemo(): Int

    @Query("UPDATE puzzles SET timesCompleted = :timesCompleted WHERE id = :id")
    suspend fun updateTimesCompleted(id: String, timesCompleted: Int)

    @Query("UPDATE puzzles SET timerAccumulatedSeconds = :seconds, timerRunningSinceEpochMillis = :runningSince WHERE id = :id")
    suspend fun updateTimer(id: String, seconds: Long, runningSince: Long?)
}
