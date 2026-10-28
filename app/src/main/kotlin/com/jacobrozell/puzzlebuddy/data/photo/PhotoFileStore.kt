package com.jacobrozell.puzzlebuddy.data.photo

import java.io.File

interface PhotoFileStore {
    suspend fun write(bytes: ByteArray): String
    fun file(fileName: String): File
    suspend fun read(fileName: String): ByteArray?
    suspend fun delete(fileName: String)
    suspend fun deleteAll()
    suspend fun deleteOrphans(keep: Set<String>)
}
