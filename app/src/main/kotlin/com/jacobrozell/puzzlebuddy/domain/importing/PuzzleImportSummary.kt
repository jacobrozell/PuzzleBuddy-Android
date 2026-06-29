package com.jacobrozell.puzzlebuddy.domain.importing

sealed class IPDbCSVImportError(message: String) : Exception(message) {
    data object EmptyFile : IPDbCSVImportError("The file looks empty. Export a CSV from IPDb and try again.")
    data object MissingTitleColumn : IPDbCSVImportError("Could not find a puzzle title column. IPDb exports should include Title or Name.")
    data object UnreadableEncoding : IPDbCSVImportError("Could not read the file. Save the export as UTF-8 CSV and try again.")
}

data class PuzzleImportSummary(
    var imported: Int = 0,
    var skippedDuplicates: Int = 0,
    var skippedInvalid: Int = 0,
    val errors: MutableList<String> = mutableListOf(),
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()

    val message: String
        get() {
            val parts = buildList {
                if (imported > 0) add("$imported puzzle${if (imported == 1) "" else "s"} imported")
                if (skippedDuplicates > 0) add("$skippedDuplicates duplicate${if (skippedDuplicates == 1) "" else "s"} skipped")
                if (skippedInvalid > 0) add("$skippedInvalid row${if (skippedInvalid == 1) "" else "s"} skipped (missing name)")
            }
            return if (parts.isEmpty()) "No puzzles were imported." else parts.joinToString(". ") + "."
        }
}
