package com.jacobrozell.puzzlebuddy.domain.model

object BarcodeNormalizer {
    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val digits = trimmed.filter { it.isDigit() }
        return digits.ifEmpty { trimmed }
    }
}
