package com.jacobrozell.puzzlebuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jacobrozell.puzzlebuddy.data.local.dao.FriendDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleCompletionDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzleDao
import com.jacobrozell.puzzlebuddy.data.local.dao.PuzzlePhotoDao
import com.jacobrozell.puzzlebuddy.data.local.entity.FriendEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleCompletionEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzleEntity
import com.jacobrozell.puzzlebuddy.data.local.entity.PuzzlePhotoEntity

@Database(
    entities = [
        PuzzleEntity::class,
        PuzzlePhotoEntity::class,
        PuzzleCompletionEntity::class,
        FriendEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PuzzleBuddyDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
    abstract fun puzzlePhotoDao(): PuzzlePhotoDao
    abstract fun puzzleCompletionDao(): PuzzleCompletionDao
    abstract fun friendDao(): FriendDao
}
