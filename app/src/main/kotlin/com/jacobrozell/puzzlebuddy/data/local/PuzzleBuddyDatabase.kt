package com.jacobrozell.puzzlebuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity

@Database(
    entities = [PuzzleEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PuzzleBuddyDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
}
