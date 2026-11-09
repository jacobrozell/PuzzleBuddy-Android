package com.jacobrozell.puzzlebuddy.domain.photo

object PuzzlePhotoPick {
    /**
     * Why some of the photos the user just picked were not saved.
     * [slots] is how many more this puzzle can hold. [selected] is how many came back from the
     * picker. [unreadable] counts photos that fit but could not be read.
     */
    fun overflowMessage(slots: Int, selected: Int, unreadable: Int): String? {
        if (slots < 0 || selected < 0 || unreadable < 0) return null
        val leftOut = (selected - slots).coerceAtLeast(0)
        val failed = unreadable.coerceAtMost(minOf(slots, selected).coerceAtLeast(0))
        if (leftOut == 0 && failed == 0) return null
        val parts = mutableListOf<String>()
        if (leftOut > 0) {
            val dropped = if (leftOut == 1) "1 was not added" else "$leftOut were not added"
            parts += if (slots <= 0) {
                "This puzzle already has $MAX_PHOTOS photos. $dropped."
            } else if (slots == 1) {
                "Only 1 more photo fits on this puzzle. $dropped."
            } else {
                "Only $slots more photos fit on this puzzle. $dropped."
            }
        }
        if (failed > 0) {
            parts += if (failed == 1) {
                "1 photo could not be read and was not added."
            } else {
                "$failed photos could not be read and were not added."
            }
        }
        return parts.joinToString(" ")
    }
}
