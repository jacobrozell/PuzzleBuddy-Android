package com.jacobrozell.puzzlebuddy.domain.barcode

data class BarcodeLookupResult(
    val metadata: BarcodeProductMetadata?,
    val notice: Notice?,
) {
    enum class Notice(val message: String) {
        RATE_LIMITED("Online lookup limit reached for today. Enter details manually or try again tomorrow."),
        UNAVAILABLE("Online lookup is unavailable right now. Enter details manually."),
        NOT_FOUND("No product details found for this barcode. Enter a name below."),
    }

    companion object {
        fun success(metadata: BarcodeProductMetadata) = BarcodeLookupResult(metadata, null)
        fun failure(notice: Notice) = BarcodeLookupResult(null, notice)
        fun empty() = BarcodeLookupResult(null, null)

        fun noticeForHttpStatus(statusCode: Int): Notice? = when {
            statusCode == 429 -> Notice.RATE_LIMITED
            statusCode !in 200..299 -> Notice.UNAVAILABLE
            else -> null
        }
    }
}
