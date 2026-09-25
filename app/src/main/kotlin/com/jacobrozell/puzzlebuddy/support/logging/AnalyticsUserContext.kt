package com.jacobrozell.puzzlebuddy.support.logging

import com.jacobrozell.puzzlebuddy.data.prefs.OnboardingStore
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.FirebaseBootstrap
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-safe user properties for retention and collection-depth segmentation.
 *
 * Values are coarse enums/booleans only — never puzzle titles, brands, or barcodes.
 */
@Singleton
class AnalyticsUserContext @Inject constructor(
    private val firebaseBootstrap: FirebaseBootstrap,
    private val onboardingStore: OnboardingStore,
) {
    suspend fun syncOnboardingComplete(complete: Boolean? = null) {
        val value = complete ?: onboardingStore.isCompleteFlow.first()
        setUserProperties(mapOf("onboarding_complete" to PuzzleAnalyticsMetadata.boolString(value)))
    }

    suspend fun syncCollection(puzzles: List<Puzzle>, onboardingComplete: Boolean? = null) {
        val complete = onboardingComplete ?: onboardingStore.isCompleteFlow.first()
        setUserProperties(PuzzleAnalyticsMetadata.userPropertyValues(puzzles, complete))
    }

    private fun setUserProperties(values: Map<String, String>) {
        if (!firebaseBootstrap.shouldConfigure || !firebaseBootstrap.isAnalyticsCollectionEnabled) return
        val analytics = firebaseBootstrap.analyticsInstance() ?: return
        values.forEach { (name, value) ->
            analytics.setUserProperty(name, value)
        }
    }
}
