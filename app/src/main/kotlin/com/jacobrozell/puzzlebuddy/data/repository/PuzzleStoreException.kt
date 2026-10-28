package com.jacobrozell.puzzlebuddy.data.repository

sealed class PuzzleStoreException(message: String) : Exception(message) {
    class DuplicateBarcode(val existingPuzzleName: String, val barcode: String) : PuzzleStoreException(
        "Another puzzle ($existingPuzzleName) already uses barcode $barcode.",
    )

    class RecordNotFound : PuzzleStoreException("That puzzle is no longer in your collection.")

    class CompletionNotFound : PuzzleStoreException("That completion log is no longer available.")

    class StatusRequiredAfterRemovingLastCompletion : PuzzleStoreException(
        "Choose a status after removing the last completion.",
    )

    class CompletionUndoUnavailable : PuzzleStoreException(
        "That completion can no longer be undone from the banner.",
    )

    class CompletionHistoryUnavailable : PuzzleStoreException(
        "Could not load completion history. Your collection was left unchanged.",
    )

    class SaveFailed : PuzzleStoreException("Could not save changes to your collection.")
}

sealed class FriendStoreException(message: String) : Exception(message) {
    class EmptyDisplayName : FriendStoreException("Enter a name for this person.")

    class ReferencedByLoans(val count: Int) : FriendStoreException(
        if (count == 1) {
            "This person still has 1 puzzle on loan. Mark it returned before deleting."
        } else {
            "This person still has $count puzzles on loan. Mark them returned before deleting."
        },
    )
}
