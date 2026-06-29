package com.jacobrozell.puzzlebuddy.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTags(tags: List<String>): String = json.encodeToString(tags)

    @TypeConverter
    fun toTags(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}
