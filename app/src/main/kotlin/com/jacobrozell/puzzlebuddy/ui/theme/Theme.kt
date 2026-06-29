package com.jacobrozell.puzzlebuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF0E8C9E)
private val AccentSecondary = Color(0xFF1FB8C6)
private val AccentWarm = Color(0xFFED731F)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentSecondary,
    background = Color(0xFFF2F7FA),
    surface = Color.White,
    onSurface = Color(0xFF14171C),
    onSurfaceVariant = Color(0xFF59616B),
)

private val DarkColors = darkColorScheme(
    primary = AccentSecondary,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFF0A0C12),
    surface = Color(0xFF1C1E23),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9EA3AB),
)

object BrandTokens {
    val accent = Accent
    val accentSecondary = AccentSecondary
    val accentWarm = AccentWarm
    val gradientTop = Color(0xFF1A73D9)
    val gradientMid = Color(0xFF1FADD1)
    val gradientBottom = Color(0xFF14948C)
}

@Composable
fun PuzzleBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
