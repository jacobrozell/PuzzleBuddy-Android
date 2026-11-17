package com.jacobrozell.puzzlebuddy.ui.barcode

/**
 * Retail symbologies the iOS scanner accepts, plus UPC-A, which ML Kit reports separately.
 * QR codes and other 2D symbols on the same box are ignored.
 *
 * Format ints match `com.google.mlkit.vision.barcode.common.Barcode`.
 */
object ProductBarcode {
    const val CODE_128 = 1
    const val CODE_39 = 2
    const val EAN_13 = 32
    const val EAN_8 = 64
    const val UPC_A = 512
    const val UPC_E = 1024

    private val priority = listOf(UPC_A, EAN_13, UPC_E, EAN_8, CODE_128, CODE_39)

    /** One barcode from a frame. Prefers the retail UPC/EAN over a secondary code on the box. */
    fun choose(candidates: List<Pair<Int, String>>): String? =
        candidates
            .filter { (format, raw) -> format in priority && raw.isNotBlank() }
            .minWithOrNull(compareBy<Pair<Int, String>> { priority.indexOf(it.first) }.thenBy { it.second })
            ?.second
}
