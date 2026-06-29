package com.jacobrozell.puzzlebuddy.ui.navigation

import android.net.Uri
import com.jacobrozell.puzzlebuddy.ui.puzzles.QuickAddRequest

object PuzzleRoutes {
    fun newPuzzleRoute(request: QuickAddRequest): String = buildString {
        append("puzzle/new?barcode=${Uri.encode(request.barcode)}")
        request.name?.let { append("&name=${Uri.encode(it)}") }
        request.pieces?.let { append("&pieces=$it") }
        request.source?.let { append("&source=${Uri.encode(it)}") }
        request.lookupNotice?.let { append("&lookup_notice=${Uri.encode(it)}") }
    }

    fun newPuzzleRoute(barcode: String? = null): String =
        if (barcode.isNullOrBlank()) {
            "puzzle/new?barcode="
        } else {
            newPuzzleRoute(QuickAddRequest(barcode = barcode))
        }
}
