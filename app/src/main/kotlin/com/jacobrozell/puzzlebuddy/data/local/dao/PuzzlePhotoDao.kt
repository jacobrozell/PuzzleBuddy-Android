package com.jacobrozell.puzzlebuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzlePhotoEntity

@Dao
interface PuzzlePhotoDao {
    @Query("SELECT * FROM puzzle_photos WHERE puzzleId = :puzzleId ORDER BY sortOrder ASC")
    suspend fun forPuzzle(puzzleId: String): List<PuzzlePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PuzzlePhotoEntity>)

    @Query("DELETE FROM puzzle_photos WHERE puzzleId = :puzzleId")
    suspend fun deleteForPuzzle(puzzleId: String)

    @Query("DELETE FROM puzzle_photos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM puzzle_photos")
    suspend fun deleteAll()

    @Query("SELECT fileName FROM puzzle_photos WHERE fileName IS NOT NULL")
    suspend fun allFileNames(): List<String>
}
