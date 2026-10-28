package com.jacobrozell.puzzlebuddy.domain.catalog

data class CollectionMilestone(
    val id: String,
    val title: String,
    val subtitle: String?,
    val icon: String,
)

fun interface MilestoneAcknowledgementStore {
    fun acknowledgedIds(): Set<String>
}

object CollectionMilestones {
    const val STORAGE_KEY = "PuzzleBuddy.AcknowledgedMilestones"

    fun newlyEarned(
        stats: CollectionStats,
        previouslyAcknowledged: Set<String>,
    ): List<CollectionMilestone> =
        earned(stats).filter { it.id !in previouslyAcknowledged }

    fun earned(stats: CollectionStats): List<CollectionMilestone> = buildList {
        if (stats.completedCount >= 1) {
            add(
                CollectionMilestone(
                    id = "completed_1",
                    title = "First puzzle completed!",
                    subtitle = "Your collection journey begins.",
                    icon = "checkmark.seal.fill",
                ),
            )
        }
        if (stats.completedCount >= 10) {
            add(
                CollectionMilestone(
                    id = "completed_10",
                    title = "10 puzzles completed!",
                    subtitle = "Double digits at the table.",
                    icon = "star.circle.fill",
                ),
            )
        }
        if (stats.completedCount >= 50) {
            add(
                CollectionMilestone(
                    id = "completed_50",
                    title = "50 puzzles completed!",
                    subtitle = "Serious collector status.",
                    icon = "crown.fill",
                ),
            )
        }
        if (stats.totalPiecesCompleted >= 10_000) {
            add(
                CollectionMilestone(
                    id = "pieces_10000",
                    title = "10,000 pieces assembled!",
                    subtitle = "That's a lot of edge pieces.",
                    icon = "puzzlepiece.extension.fill",
                ),
            )
        }
        if (stats.totalPiecesCompleted >= 50_000) {
            add(
                CollectionMilestone(
                    id = "pieces_50000",
                    title = "50,000 pieces assembled!",
                    subtitle = "Legendary puzzler.",
                    icon = "sparkles",
                ),
            )
        }
        if (stats.totalMinutesPuzzling >= 6_000) {
            add(
                CollectionMilestone(
                    id = "hours_100",
                    title = "100 hours at the table!",
                    subtitle = "Time well puzzled.",
                    icon = "clock.fill",
                ),
            )
        }
        if (stats.completionsThisYear >= 12) {
            add(
                CollectionMilestone(
                    id = "year_completions_12",
                    title = "12 puzzles this year!",
                    subtitle = "A puzzle a month pace.",
                    icon = "calendar",
                ),
            )
        }
    }
}
