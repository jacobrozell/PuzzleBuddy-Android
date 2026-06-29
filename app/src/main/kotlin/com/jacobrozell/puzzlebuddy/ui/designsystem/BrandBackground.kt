package com.jacobrozell.puzzlebuddy.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

@Composable
fun BrandBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandTokens.gradientTop.copy(alpha = 0.18f),
                        BrandTokens.gradientMid.copy(alpha = 0.10f),
                        BrandTokens.gradientBottom.copy(alpha = 0.06f),
                    ),
                ),
            ),
    ) {
        content()
    }
}
