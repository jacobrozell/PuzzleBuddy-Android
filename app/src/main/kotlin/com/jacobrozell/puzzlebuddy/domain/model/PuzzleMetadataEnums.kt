package com.jacobrozell.puzzlebuddy.domain.model

enum class PuzzleType(val raw: String) {
    NONE("None"),
    LANDSCAPE("Landscape"),
    MYSTERY("Mystery"),
    GRADIENT("Gradient"),
    COLLAGE("Collage"),
    PANORAMIC("Panoramic"),
    OTHER("Other"),
    ;

    val displayLabel: String get() = if (this == NONE) "Not set" else raw

    val accessibilityDescription: String get() = if (this == NONE) "No puzzle type" else "Type $raw"

    companion object {
        val selectableCases: List<PuzzleType> = entries.filter { it != NONE }

        fun fromRaw(raw: String): PuzzleType = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleMaterial(val raw: String) {
    NONE("None"),
    CARDBOARD("Cardboard"),
    WOOD("Wood"),
    PLASTIC("Plastic"),
    OTHER("Other"),
    ;

    val displayLabel: String get() = if (this == NONE) "Not set" else raw

    val accessibilityDescription: String get() = if (this == NONE) "No material" else "Material $raw"

    companion object {
        val selectableCases: List<PuzzleMaterial> = entries.filter { it != NONE }

        fun fromRaw(raw: String): PuzzleMaterial = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleDisposition(val raw: String) {
    NONE("None"),
    KEPT("Kept"),
    DONATED("Donated"),
    SOLD("Sold"),
    GIFTED("Gifted"),
    TRASHED("Trashed"),
    ;

    val displayLabel: String get() = if (this == NONE) "Not set" else raw

    val accessibilityDescription: String get() = if (this == NONE) "No disposition" else "Disposition $raw"

    companion object {
        val selectableCases: List<PuzzleDisposition> = entries.filter { it != NONE }

        fun fromRaw(raw: String): PuzzleDisposition = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleShape(val raw: String) {
    NONE("None"),
    RECTANGULAR("Rectangular"),
    SQUARE("Square"),
    ROUND("Round"),
    IRREGULAR("Irregular"),
    ;

    val displayLabel: String get() = if (this == NONE) "Not set" else raw

    val accessibilityDescription: String get() = if (this == NONE) "No shape" else "Shape $raw"

    companion object {
        val selectableCases: List<PuzzleShape> = entries.filter { it != NONE }

        fun fromRaw(raw: String): PuzzleShape = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}

enum class PuzzleCutType(val raw: String) {
    NONE("None"),
    RIBBON("Ribbon"),
    GRID("Grid"),
    RANDOM("Random"),
    UNKNOWN("Unknown"),
    ;

    val displayLabel: String get() = if (this == NONE) "Not set" else raw

    val accessibilityDescription: String get() = if (this == NONE) "No cut type" else "Cut type $raw"

    companion object {
        val selectableCases: List<PuzzleCutType> = entries.filter { it != NONE }

        fun fromRaw(raw: String): PuzzleCutType = entries.firstOrNull { it.raw == raw } ?: NONE
    }
}
