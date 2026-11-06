package com.jacobrozell.puzzlebuddy.data.export

import com.jacobrozell.puzzlebuddy.data.photo.PhotoFileStore
import com.jacobrozell.puzzlebuddy.domain.export.PuzzleCollectionExporter
import com.jacobrozell.puzzlebuddy.domain.loan.Friend
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

/** Builds a Puzzle Buddy JSON backup. Photo bytes stay in [PhotoFileStore], not in `domain/`. */
object PuzzleCollectionBackup {
    suspend fun jsonData(
        puzzles: List<Puzzle>,
        friends: List<Friend>,
        photoStore: PhotoFileStore,
        appVersion: String,
    ): ByteArray {
        val photoBytes = buildMap {
            for (puzzle in puzzles) {
                for (photo in puzzle.photos) {
                    val name = photo.fileName ?: continue
                    photoStore.read(name)?.let { put(photo.id, it) }
                }
            }
        }
        return PuzzleCollectionExporter.jsonData(puzzles, appVersion, friends, photoBytes)
    }
}
