package com.jacobrozell.puzzlebuddy.domain.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeLookupServiceTest {
    @Test
    fun parseResponseExtractsTitleBrandAndPieces() {
        val json = """
            {"items":[{"title":"Winter Lights 1000 Piece Jigsaw","brand":"Ravensburger","images":["https://example.com/a.jpg"]}]}
        """.trimIndent().toByteArray()
        val metadata = BarcodeLookupService.parseResponse(json)
        assertEquals("Winter Lights 1000 Piece Jigsaw", metadata?.title)
        assertEquals("Ravensburger", metadata?.brand)
        assertEquals(1000, metadata?.suggestedPieces)
    }

    @Test
    fun parseResponseReturnsNullWhenNoItems() {
        val json = """{"items":[]}""".toByteArray()
        assertNull(BarcodeLookupService.parseResponse(json))
    }
}

class BarcodeTitleParserTest {
    @Test
    fun extractsPieceCountFromTitle() {
        assertEquals(1000, BarcodeTitleParser.piecesFrom("Winter Lights 1000 Piece Jigsaw"))
    }
}
