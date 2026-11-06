package com.jacobrozell.puzzlebuddy.support.share

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleCollectionShare
import java.io.File

object PuzzleCollectionSharer {
    suspend fun share(context: Context, repository: PuzzleRepository, puzzles: List<Puzzle>) {
        val caption = PuzzleCollectionShare.makeCaption(puzzles) ?: return
        val covers = PuzzleCollageRenderer.featuredTiles(puzzles).associate { puzzle ->
            val photo = PuzzlePhotoSemantics.cover(puzzle.photos)
            puzzle.id to photo?.let { repository.photoData(it) }
        }
        val png = PuzzleCollageRenderer.render(puzzles, covers)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "collection.png")
        file.writeBytes(png)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
