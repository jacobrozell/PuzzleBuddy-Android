package com.jacobrozell.puzzlebuddy.support

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jacobrozell.puzzlebuddy.domain.export.PuzzleCollectionExportFormat
import java.io.File

object ExportShareHelper {
    fun share(
        context: Context,
        data: ByteArray,
        format: PuzzleCollectionExportFormat,
        fileName: String,
    ) {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        file.writeBytes(data)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "Puzzle Buddy export")
        }
        context.startActivity(Intent.createChooser(intent, "Export collection"))
    }
}
