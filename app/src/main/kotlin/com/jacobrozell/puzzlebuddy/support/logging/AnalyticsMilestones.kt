package com.jacobrozell.puzzlebuddy.support.logging

import javax.inject.Inject
import javax.inject.Singleton

/** Fires `milestone_reached` once per milestone id (analytics-only ack). */
@Singleton
class AnalyticsMilestones @Inject constructor(
    private val store: AnalyticsKeyValueStore,
    private val logger: AppLogger,
) {
    fun logIfNeeded(milestoneID: String) {
        if (milestoneID.isEmpty()) return
        val logged = store.getStringSet(STORAGE_KEY).toMutableSet()
        if (!logged.add(milestoneID)) return
        store.putStringSet(STORAGE_KEY, logged)

        logger.info(
            LogCategory.APP,
            eventName = "milestone_reached",
            message = "Collection milestone reached.",
            metadata = mapOf("milestone_id" to milestoneID),
        )
    }

    fun logCollectionSizeMilestones(puzzleCount: Int) {
        if (puzzleCount >= 1) logIfNeeded("first_puzzle")
        if (puzzleCount >= 10) logIfNeeded("ten_puzzles")
    }

    fun logCompletionMilestones(completedCount: Int) {
        if (completedCount >= 1) logIfNeeded("first_completion")
        if (completedCount >= 5) logIfNeeded("five_completions")
        if (completedCount >= 10) logIfNeeded("completed_10")
    }

    fun resetForTesting() {
        store.putStringSet(STORAGE_KEY, emptySet())
    }

    companion object {
        const val STORAGE_KEY = "PuzzleBuddy.AnalyticsLoggedMilestones"
    }
}
