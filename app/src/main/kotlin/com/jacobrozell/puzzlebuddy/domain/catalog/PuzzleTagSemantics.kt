package com.jacobrozell.puzzlebuddy.domain.catalog

object PuzzleTagSemantics {
    const val MAX_TAGS_PER_PUZZLE = 10
    const val MAX_TAG_LENGTH = 32

    fun normalized(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.take(MAX_TAG_LENGTH)
    }

    fun sanitizedTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (tag in tags) {
            val normalized = normalized(tag) ?: continue
            val key = normalized.lowercase()
            if (seen.add(key)) {
                result += normalized
                if (result.size >= MAX_TAGS_PER_PUZZLE) break
            }
        }
        return result
    }

    fun matches(tag: String, selected: String): Boolean {
        val left = normalized(tag) ?: return false
        val right = normalized(selected) ?: return false
        return left.equals(right, ignoreCase = true)
    }

    fun contains(tags: List<String>, selected: String): Boolean =
        tags.any { matches(it, selected) }
}

data class PuzzleTagCount(
    val name: String,
    val count: Int,
)

object PuzzleTagIndex {
    fun counts(puzzles: List<com.jacobrozell.puzzlebuddy.domain.model.Puzzle>, limit: Int = 20): List<PuzzleTagCount> {
        val frequencies = linkedMapOf<String, Pair<String, Int>>()
        for (puzzle in puzzles) {
            for (tag in puzzle.tags) {
                val normalized = PuzzleTagSemantics.normalized(tag) ?: continue
                val key = normalized.lowercase()
                val existing = frequencies[key]
                frequencies[key] = if (existing != null) {
                    existing.first to existing.second + 1
                } else {
                    normalized to 1
                }
            }
        }
        return frequencies.values
            .map { PuzzleTagCount(it.first, it.second) }
            .sortedWith(compareByDescending<PuzzleTagCount> { it.count }.thenBy { it.name.lowercase() })
            .take(limit)
    }

    fun allTagNames(puzzles: List<com.jacobrozell.puzzlebuddy.domain.model.Puzzle>): List<String> =
        counts(puzzles, Int.MAX_VALUE).map { it.name }

    fun matchingTags(
        query: String,
        excluding: List<String>,
        catalog: List<String>,
        limit: Int = 8,
    ): List<String> {
        val excluded = excluding.map { it.lowercase() }.toSet()
        val available = catalog.filter { it.lowercase() !in excluded }
        val trimmed = query.trim()
        val candidates = if (trimmed.isEmpty()) {
            available
        } else {
            available.filter { it.contains(trimmed, ignoreCase = true) }
        }
        return candidates.take(limit)
    }

    fun filter(
        puzzles: List<com.jacobrozell.puzzlebuddy.domain.model.Puzzle>,
        selectedTag: String?,
    ): List<com.jacobrozell.puzzlebuddy.domain.model.Puzzle> {
        val normalized = selectedTag?.let(PuzzleTagSemantics::normalized) ?: return puzzles
        return puzzles.filter { PuzzleTagSemantics.contains(it.tags, normalized) }
    }
}
