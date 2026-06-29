package com.jacobrozell.puzzlebuddy.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mirrors iOS [AdaptiveLayout] — breakpoints and width caps for tablet / landscape.
 */
object AdaptiveLayout {
    val expandedWidthBreakpoint: Dp = 600.dp
    val listGridMinCellWidth: Dp = 360.dp

    fun usesWideDetailLayout(containerWidthDp: Float, containerHeightDp: Float): Boolean {
        val isLandscape = containerHeightDp < containerWidthDp && containerWidthDp >= 480f
        val isExpanded = containerWidthDp >= expandedWidthBreakpoint.value
        return isExpanded || isLandscape
    }

    fun usesNavigationRail(containerWidthDp: Float): Boolean =
        containerWidthDp >= expandedWidthBreakpoint.value

    fun contentMaxWidth(containerWidthDp: Float, isLandscape: Boolean): Float {
        val minReadable = 680f
        val maxReadable = if (isLandscape) 1_180f else 1_020f
        val fraction = if (isLandscape) 0.94f else 0.96f
        val target = containerWidthDp * fraction
        return minOf(maxOf(target, minReadable), maxReadable, containerWidthDp)
    }
}
