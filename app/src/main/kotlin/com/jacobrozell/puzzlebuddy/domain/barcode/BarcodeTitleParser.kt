package com.jacobrozell.puzzlebuddy.domain.barcode

object BarcodeTitleParser {
    private val pieceCountPattern = Regex("""(?i)(\d{2,5})\s*(?:piece|pieces|pc|pce|pcs)\b""")

    fun piecesFrom(title: String?): Int? {
        if (title.isNullOrBlank()) return null
        val match = pieceCountPattern.find(title) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    fun cleanedTitle(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        return trimmed.ifEmpty { null }
    }
}
