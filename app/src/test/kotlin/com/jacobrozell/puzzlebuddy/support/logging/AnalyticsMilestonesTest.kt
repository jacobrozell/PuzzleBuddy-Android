package com.jacobrozell.puzzlebuddy.support.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsMilestonesTest {
    @Test
    fun logsEachMilestoneOnce() {
        val store = InMemoryAnalyticsKeyValueStore()
        val logger = RecordingAppLogger()
        val milestones = AnalyticsMilestones(store, logger)

        milestones.logCollectionSizeMilestones(1)
        milestones.logCollectionSizeMilestones(1)
        milestones.logCompletionMilestones(1)
        milestones.logCompletionMilestones(10)

        assertEquals(
            listOf("first_puzzle", "first_completion", "five_completions", "completed_10"),
            logger.entries.map { it.metadata["milestone_id"] },
        )
        logger.entries.forEach { entry ->
            assertEquals("milestone_reached", entry.eventName)
        }
    }
}
