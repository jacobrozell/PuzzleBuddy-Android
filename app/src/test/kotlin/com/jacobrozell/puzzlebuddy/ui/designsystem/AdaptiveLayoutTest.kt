package com.jacobrozell.puzzlebuddy.ui.designsystem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun usesWideDetailLayoutOnTablet() {
        assertTrue(AdaptiveLayout.usesWideDetailLayout(containerWidthDp = 800f, containerHeightDp = 1_000f))
    }

    @Test
    fun usesWideDetailLayoutOnPhoneLandscape() {
        assertTrue(AdaptiveLayout.usesWideDetailLayout(containerWidthDp = 800f, containerHeightDp = 400f))
    }

    @Test
    fun usesStackedDetailLayoutOnPhonePortrait() {
        assertFalse(AdaptiveLayout.usesWideDetailLayout(containerWidthDp = 390f, containerHeightDp = 844f))
    }

    @Test
    fun contentMaxWidthUsesWiderCapInLandscape() {
        val portrait = AdaptiveLayout.contentMaxWidth(containerWidthDp = 1_200f, isLandscape = false)
        val landscape = AdaptiveLayout.contentMaxWidth(containerWidthDp = 1_200f, isLandscape = true)
        assertTrue(landscape > portrait)
    }

    @Test
    fun usesNavigationRailAtExpandedBreakpoint() {
        assertTrue(AdaptiveLayout.usesNavigationRail(600f))
        assertFalse(AdaptiveLayout.usesNavigationRail(599f))
    }
}
