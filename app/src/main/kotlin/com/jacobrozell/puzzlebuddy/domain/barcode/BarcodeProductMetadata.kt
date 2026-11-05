package com.jacobrozell.puzzlebuddy.domain.barcode

/** Quick-add prefill from a puzzle you already saved with this barcode. Never fetched online. */
data class BarcodeProductMetadata(
    val title: String?,
    val brand: String?,
    val pieces: Int?,
    val sourcePuzzleId: String? = null,
    val sourcePuzzleName: String? = null,
) {
    val suggestedName: String? get() = BarcodeTitleParser.cleanedTitle(title)
    val suggestedPieces: Int? get() = pieces ?: BarcodeTitleParser.piecesFrom(title)
    val lookupSourceLabel: String
        get() = if (!sourcePuzzleName.isNullOrEmpty()) {
            "Previously saved as $sourcePuzzleName"
        } else {
            "From your saved puzzles"
        }
}
