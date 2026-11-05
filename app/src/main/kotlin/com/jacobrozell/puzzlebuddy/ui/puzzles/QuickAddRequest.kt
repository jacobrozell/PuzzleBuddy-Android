package com.jacobrozell.puzzlebuddy.ui.puzzles

data class QuickAddRequest(
    val barcode: String,
    val name: String? = null,
    val pieces: Int? = null,
    val source: String? = null,
)
