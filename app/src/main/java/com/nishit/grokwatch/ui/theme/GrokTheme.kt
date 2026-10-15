package com.nishit.grokwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val GrokColors = Colors(
    primary = Color(0xFF9DB4FF),
    primaryVariant = Color(0xFF6E8CFF),
    secondary = Color(0xFFC9D4FF),
    secondaryVariant = Color(0xFF8FA2E8),
    background = Color.Black,
    surface = Color(0xFF141820),
    error = Color(0xFFFF8A80),
    onPrimary = Color(0xFF0A1020),
    onSecondary = Color(0xFF0A1020),
    onBackground = Color(0xFFF5F7FF),
    onSurface = Color(0xFFF5F7FF),
    onSurfaceVariant = Color(0xFFB7C0D6),
    onError = Color(0xFF2B0000),
)

@Composable
fun GrokTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = GrokColors, content = content)
}
