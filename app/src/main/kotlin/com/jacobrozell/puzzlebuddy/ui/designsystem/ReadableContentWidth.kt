package com.jacobrozell.puzzlebuddy.ui.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Caps content width on tablets and in landscape. The child tree is the same in every case, and
 * orientation comes from the configuration rather than the box height, so the keyboard opening
 * (which shrinks the height) never rebuilds the content and drops text-field focus.
 */
@Composable
fun ReadableContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val shouldConstrain = maxWidth >= AdaptiveLayout.expandedWidthBreakpoint || isLandscape
        val targetWidth = if (shouldConstrain) {
            AdaptiveLayout.contentMaxWidth(maxWidth.value, isLandscape).dp
        } else {
            maxWidth
        }
        Box(
            modifier = Modifier
                .widthIn(max = targetWidth)
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
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
