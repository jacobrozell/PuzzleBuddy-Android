package com.jacobrozell.puzzlebuddy.ui.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Caps a bottom sheet to the window and scrolls inside it.
 * An uncapped sheet on a short landscape window clips the last button and does not scroll.
 */
@Composable
fun Modifier.boundedSheetScroll(): Modifier {
    val scroll = rememberScrollState()
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    return this
        .fillMaxWidth()
        .heightIn(max = maxHeight)
        .verticalScroll(scroll)
        .navigationBarsPadding()
        .imePadding()
}
