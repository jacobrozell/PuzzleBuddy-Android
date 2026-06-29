package com.jacobrozell.puzzlebuddy.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReadableContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isLandscape = maxHeight < maxWidth
        val shouldConstrain = maxWidth >= AdaptiveLayout.expandedWidthBreakpoint || isLandscape
        if (shouldConstrain) {
            val targetWidth = AdaptiveLayout
                .contentMaxWidth(maxWidth.value, isLandscape)
                .dp
            Box(
                modifier = Modifier
                    .widthIn(max = targetWidth)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

/** @see ReadableContentWidth */
@Composable
fun ReadableWidthColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = ReadableContentWidth(modifier = modifier, content = content)
