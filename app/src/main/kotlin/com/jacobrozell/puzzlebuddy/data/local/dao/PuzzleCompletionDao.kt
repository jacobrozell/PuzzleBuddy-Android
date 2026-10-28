package com.jacobrozell.puzzlebuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleCompletionEntity

@Dao
interface PuzzleCompletionDao {
    @Query("SELECT * FROM puzzle_completions WHERE puzzleId = :puzzleId ORDER BY completionNumber ASC")
    suspend fun forPuzzle(puzzleId: String): List<PuzzleCompletionEntity>

    @Query("SELECT * FROM puzzle_completions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PuzzleCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PuzzleCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PuzzleCompletionEntity>)

    @Query("DELETE FROM puzzle_completions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM puzzle_completions WHERE puzzleId = :puzzleId")
    suspend fun deleteForPuzzle(puzzleId: String)

    @Query("DELETE FROM puzzle_completions")
    suspend fun deleteAll()
}
