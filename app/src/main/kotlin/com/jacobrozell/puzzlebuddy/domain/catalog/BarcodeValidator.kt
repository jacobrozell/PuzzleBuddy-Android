package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

object BarcodeValidator {
    fun normalizeOrNull(raw: String): String? {
        val normalized = BarcodeNormalizer.normalize(raw) ?: optionalDigits(raw)
        return normalized?.takeIf { it.length in 6..14 }
    }

    fun optionalDigits(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return digits.ifEmpty { null }
    }
}
