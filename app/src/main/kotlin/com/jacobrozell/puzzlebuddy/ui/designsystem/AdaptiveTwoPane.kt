package com.jacobrozell.puzzlebuddy.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val wide = AdaptiveLayout.usesWideDetailLayout(maxWidth.value, maxHeight.value)
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Box(Modifier.weight(1f)) { firstPane() }
                Box(Modifier.weight(1f)) { secondPane() }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                firstPane()
                secondPane()
            }
        }
    }
}
