package com.jacobrozell.puzzlebuddy.domain.model

object BarcodeNormalizer {
    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val digits = trimmed.filter { it.isDigit() }
        return digits.ifEmpty { trimmed }
    }

    /**
     * Same retail barcode. A UPC-A and the EAN-13 formed by adding a leading 0 are one product.
     * The camera reports either form depending on the symbology it locks onto.
     */
    fun sameProduct(left: String?, right: String?): Boolean {
        val a = normalize(left) ?: return false
        val b = normalize(right) ?: return false
        if (a == b) return true
        val aDigits = digitsInRange(a) ?: return false
        val bDigits = digitsInRange(b) ?: return false
        return retailKey(aDigits) == retailKey(bDigits)
    }

    private fun digitsInRange(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length in 6..14 }
    }

    private fun retailKey(digits: String): String =
        if (digits.length == 13 && digits.startsWith("0")) digits.drop(1) else digits
}
