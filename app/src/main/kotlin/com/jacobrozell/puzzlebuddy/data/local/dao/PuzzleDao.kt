package com.jacobrozell.puzzlebuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Query("SELECT * FROM puzzles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PuzzleEntity>>

    @Query("SELECT * FROM puzzles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PuzzleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PuzzleEntity)

    @Query("DELETE FROM puzzles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM puzzles")
    suspend fun deleteAll()

    @Query("DELETE FROM puzzles WHERE isDemo = 1")
    suspend fun deleteDemo()

    @Query("SELECT COUNT(*) FROM puzzles WHERE isDemo = 1")
    suspend fun countDemo(): Int
}
