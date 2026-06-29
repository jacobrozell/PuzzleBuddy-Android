package com.jacobrozell.puzzlebuddy.domain.barcode

data class BarcodeProductMetadata(
    val title: String?,
    val brand: String?,
    val pieces: Int?,
    val imageUrl: String?,
    val source: String,
) {
    val suggestedName: String? get() = BarcodeTitleParser.cleanedTitle(title)
    val suggestedPieces: Int? get() = pieces ?: BarcodeTitleParser.piecesFrom(title)
    val lookupSourceLabel: String?
        get() = when (source) {
            "local_cache" -> "From your saved puzzles"
            "upcitemdb" -> "From online product lookup"
            else -> null
        }

    companion object {
        fun fromLookup(title: String?, brand: String?, imageUrl: String?): BarcodeProductMetadata =
            BarcodeProductMetadata(
                title = BarcodeTitleParser.cleanedTitle(title),
                brand = BarcodeTitleParser.cleanedTitle(brand),
                pieces = BarcodeTitleParser.piecesFrom(title),
                imageUrl = imageUrl,
                source = "upcitemdb",
            )
    }
}
