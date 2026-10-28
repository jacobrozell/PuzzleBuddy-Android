package com.jacobrozell.puzzlebuddy.domain.catalog

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.min

object PurchasePriceFormatting {
    fun displayLabel(price: Double, currencyCode: String?): String {
        val code = currencyCode ?: runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrNull() ?: "USD"
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        runCatching { format.currency = Currency.getInstance(code) }
        format.maximumFractionDigits = 2
        return format.format(price)
    }

    fun parse(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.contains("-")) return null
        val sanitized = trimmed.replace(Regex("[^0-9.,]"), "").replace(",", ".")
        val value = sanitized.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return min(value, 999_999.99)
    }
}
