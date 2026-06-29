package com.jacobrozell.puzzlebuddy.domain.barcode

import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.prefs.AppPreferencesStore
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class UpcLookupItem(
    val title: String? = null,
    val brand: String? = null,
    val images: List<String> = emptyList(),
)

@Serializable
private data class UpcLookupResponse(
    val items: List<UpcLookupItem> = emptyList(),
)

@Singleton
class BarcodeLookupService @Inject constructor(
    private val metadataCache: BarcodeMetadataCache,
    private val appPreferences: AppPreferencesStore,
    private val logger: AppLogger,
) {
    private val memoryCache = mutableMapOf<String, BarcodeProductMetadata>()

    suspend fun lookup(barcode: String): BarcodeLookupResult {
        val normalized = BarcodeValidator.normalizeOrNull(barcode)
            ?: return BarcodeLookupResult.failure(BarcodeLookupResult.Notice.NOT_FOUND)

        memoryCache[normalized]?.let { return BarcodeLookupResult.success(it) }

        metadataCache.metadataFor(normalized)?.let { cached ->
            memoryCache[normalized] = cached
            logger.info(
                LogCategory.PUZZLES,
                eventName = "barcode_lookup_succeeded",
                message = "Barcode metadata found in local cache.",
                metadata = mapOf("has_title" to if (cached.title == null) "0" else "1"),
            )
            return BarcodeLookupResult.success(cached)
        }

        if (!appPreferences.isBarcodeLookupEnabled.first()) {
            return BarcodeLookupResult.empty()
        }

        return performOnlineLookup(normalized)
    }

    private suspend fun performOnlineLookup(normalized: String): BarcodeLookupResult =
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL("$ENDPOINT?upc=$normalized").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val status = connection.responseCode
                BarcodeLookupResult.noticeForHttpStatus(status)?.let { notice ->
                    logLookupFailed(
                        message = "Barcode lookup returned status $status.",
                        metadata = mapOf("status_code" to status.toString()),
                    )
                    return@withContext BarcodeLookupResult.failure(notice)
                }
                val body = connection.inputStream.use { it.readBytes() }
                val metadata = parseResponse(body)
                if (metadata != null) {
                    memoryCache[normalized] = metadata
                    metadataCache.storeLookup(metadata, normalized)
                    logger.info(
                        LogCategory.PUZZLES,
                        eventName = "barcode_lookup_succeeded",
                        message = "Barcode metadata found.",
                        metadata = mapOf("has_title" to if (metadata.title == null) "0" else "1"),
                    )
                    BarcodeLookupResult.success(metadata)
                } else {
                    logLookupFailed(message = "No items in barcode lookup response.")
                    BarcodeLookupResult.failure(BarcodeLookupResult.Notice.NOT_FOUND)
                }
            } catch (error: Exception) {
                logLookupFailed(message = error.message ?: "Barcode lookup failed.")
                BarcodeLookupResult.failure(BarcodeLookupResult.Notice.UNAVAILABLE)
            }
        }

    private fun logLookupFailed(message: String, metadata: Map<String, String> = emptyMap()) {
        logger.info(
            LogCategory.PUZZLES,
            eventName = "barcode_lookup_failed",
            message = message,
            metadata = metadata,
        )
    }

    companion object {
        private const val ENDPOINT = "https://api.upcitemdb.com/prod/trial/lookup"
        private val json = Json { ignoreUnknownKeys = true }

        fun parseResponse(data: ByteArray): BarcodeProductMetadata? {
            val response = json.decodeFromString<UpcLookupResponse>(String(data, Charsets.UTF_8))
            val first = response.items.firstOrNull() ?: return null
            val imageUrl = first.images.firstOrNull()?.takeIf { it.isNotBlank() }
            return BarcodeProductMetadata.fromLookup(first.title, first.brand, imageUrl)
        }
    }
}
