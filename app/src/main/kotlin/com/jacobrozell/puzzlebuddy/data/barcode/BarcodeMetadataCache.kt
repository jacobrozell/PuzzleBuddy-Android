package com.jacobrozell.puzzlebuddy.data.barcode

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jacobrozell.puzzlebuddy.domain.barcode.BarcodeProductMetadata
import com.jacobrozell.puzzlebuddy.domain.barcode.BarcodeTitleParser
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.barcodeCacheStore: DataStore<Preferences> by preferencesDataStore("puzzle_buddy_barcode_cache")

@Serializable
private data class CachedBarcodeEntry(
    val title: String? = null,
    val brand: String? = null,
    val pieces: Int? = null,
)

@Singleton
class BarcodeMetadataCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheKey = stringPreferencesKey("entries_json")

    suspend fun storeFromPuzzle(puzzle: Puzzle) {
        val normalized = BarcodeValidator.normalizeOrNull(puzzle.barcode.orEmpty()) ?: return
        val cache = loadCache().toMutableMap()
        cache[normalized] = CachedBarcodeEntry(
            title = BarcodeTitleParser.cleanedTitle(puzzle.name),
            brand = BarcodeTitleParser.cleanedTitle(puzzle.source),
            pieces = puzzle.pieces,
        )
        saveCache(cache)
    }

    suspend fun warmCache(puzzles: List<Puzzle>) {
        val cache = loadCache().toMutableMap()
        for (puzzle in puzzles) {
            val normalized = BarcodeValidator.normalizeOrNull(puzzle.barcode.orEmpty()) ?: continue
            cache[normalized] = CachedBarcodeEntry(
                title = BarcodeTitleParser.cleanedTitle(puzzle.name),
                brand = BarcodeTitleParser.cleanedTitle(puzzle.source),
                pieces = puzzle.pieces,
            )
        }
        saveCache(cache)
    }

    suspend fun metadataFor(barcode: String): BarcodeProductMetadata? {
        val normalized = BarcodeValidator.normalizeOrNull(barcode) ?: return null
        val entry = loadCache()[normalized] ?: return null
        return BarcodeProductMetadata(
            title = entry.title,
            brand = entry.brand,
            pieces = entry.pieces,
            imageUrl = null,
            source = "local_cache",
        )
    }

    suspend fun storeLookup(metadata: BarcodeProductMetadata, barcode: String) {
        if (metadata.source != "upcitemdb") return
        if (metadata.suggestedName == null && metadata.brand == null) return
        val normalized = BarcodeValidator.normalizeOrNull(barcode) ?: return
        val cache = loadCache().toMutableMap()
        cache[normalized] = CachedBarcodeEntry(
            title = metadata.title,
            brand = metadata.brand,
            pieces = metadata.suggestedPieces,
        )
        saveCache(cache)
    }

    private suspend fun loadCache(): Map<String, CachedBarcodeEntry> {
        val raw = context.barcodeCacheStore.data.first()[cacheKey] ?: return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, CachedBarcodeEntry>>(raw)
        }.getOrDefault(emptyMap())
    }

    private suspend fun saveCache(cache: Map<String, CachedBarcodeEntry>) {
        context.barcodeCacheStore.edit { prefs ->
            prefs[cacheKey] = json.encodeToString(cache)
        }
    }
}
