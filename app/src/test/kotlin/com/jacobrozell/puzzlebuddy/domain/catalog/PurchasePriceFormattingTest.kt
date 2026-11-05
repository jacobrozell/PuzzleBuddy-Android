package com.jacobrozell.puzzlebuddy.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports iOS `PurchasePriceFormattingTests` (tag 1.1.0). */
class PurchasePriceFormattingTest {
    @Test
    fun parseAcceptsDecimalString() {
        assertEquals(12.99, PurchasePriceFormatting.parse("12.99"))
        assertEquals(14.50, PurchasePriceFormatting.parse("$14.50"))
    }

    @Test
    fun parseEmptyReturnsNull() {
        assertNull(PurchasePriceFormatting.parse(""))
        assertNull(PurchasePriceFormatting.parse("   "))
    }

    @Test
    fun parseClampsMaximum() {
        assertEquals(999_999.99, PurchasePriceFormatting.parse("9999999"))
    }

    @Test
    fun parseRejectsNegative() {
        assertNull(PurchasePriceFormatting.parse("-5"))
    }

    @Test
    fun displayLabelFormatsUsd() {
        val label = PurchasePriceFormatting.displayLabel(12.5, "USD")
        assertTrue(label, label.contains("12.5") || label.contains("12.50"))
        assertTrue(label, label.contains("$") || label.contains("US"))
    }

    @Test
    fun parseEuropeanCommaDecimal() {
        assertEquals(12.50, PurchasePriceFormatting.parse("12,50"))
    }
}
