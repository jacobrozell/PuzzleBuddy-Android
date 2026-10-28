package com.jacobrozell.puzzlebuddy.data.photo

import android.content.Context
import com.jacobrozell.puzzlebuddy.support.ImageCompression
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzlePhotoFileStore @Inject constructor(
    @ApplicationContext context: Context,
) : PhotoFileStore {
    private val directory = File(context.filesDir, "puzzle_photos").apply { mkdirs() }

    override suspend fun write(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val name = "${UUID.randomUUID()}.jpg"
        val compressed = ImageCompression.compressJpeg(bytes)
        file(name).writeBytes(compressed)
        name
    }

    override fun file(fileName: String): File {
        val safe = File(fileName).name
        return File(directory, safe)
    }

    override suspend fun read(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val target = file(fileName)
        if (!target.exists()) null else target.readBytes()
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { file(fileName).delete() }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            directory.listFiles()?.forEach { it.delete() }
        }
    }

    override suspend fun deleteOrphans(keep: Set<String>) {
        withContext(Dispatchers.IO) {
            directory.listFiles()?.forEach { child ->
                if (child.name !in keep) child.delete()
            }
        }
    }
}
