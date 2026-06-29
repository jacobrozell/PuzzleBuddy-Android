package com.jacobrozell.puzzlebuddy.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeValidatorTest {
    @Test
    fun normalizesDigitsOnly() {
        assertEquals("012345678905", BarcodeValidator.normalizeOrNull("012-345-678-905"))
    }

    @Test
    fun rejectsTooShort() {
        assertNull(BarcodeValidator.normalizeOrNull("12345"))
    }

    @Test
    fun rejectsTooLong() {
        assertNull(BarcodeValidator.normalizeOrNull("123456789012345"))
    }
}
