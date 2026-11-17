package com.jacobrozell.puzzlebuddy.ui.barcode

import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingScanSessionTest {
    @Test
    fun oneRetailBarcodeWinsOverASecondCodeInTheSameFrame() {
        val chosen = ProductBarcode.choose(
            listOf(
                ProductBarcode.CODE_128 to "ABC-999",
                ProductBarcode.EAN_13 to "0036000291452",
                256 to "https://example.test/qr",
            ),
        )
        assertEquals("0036000291452", chosen)
    }

    @Test
    fun qrCodesAreIgnored() {
        assertEquals(null, ProductBarcode.choose(listOf(256 to "https://example.test/box")))
    }

    @Test
    fun upcAndEan13Match() {
        assertTrue(BarcodeNormalizer.sameProduct("036000291452", "0036000291452"))
        assertFalse(BarcodeNormalizer.sameProduct("036000291452", "012345678905"))
    }

    @Test
    fun eachScanReplacesThePreviousCard() {
        val first = ShoppingScanSession.record(ShoppingModeState(), "036000291452", match = null)
        assertEquals("036000291452", (first.result as ShoppingScanResult.NoMatch).barcode)

        val again = ShoppingScanSession.record(first.copy(result = null), "012345678905", match = null)
        assertEquals("012345678905", (again.result as ShoppingScanResult.NoMatch).barcode)
        assertEquals(2, again.scansTotal)
        assertEquals(0, again.duplicatesFound)
    }

    @Test
    fun aSavedPuzzleCountsAsOwnedEvenWhenTheSymbologyDiffers() {
        val owned = Puzzle(name = "Paris in a Day", barcode = "036000291452")
        val result = ShoppingScanSession.record(ShoppingModeState(), "0036000291452", owned)
        val match = result.result as ShoppingScanResult.Match
        assertEquals("Paris in a Day", match.puzzle.name)
        assertEquals("0036000291452", match.barcode)
        assertEquals(1, result.duplicatesFound)
    }
}
