package com.wordmatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design palette (see DESIGN_PLAN section 2).
val VibrantBlue = Color(0xFF3B82F6)
val SunnyOrange = Color(0xFFFB923C)
val SoftGreen = Color(0xFF10B981)
val LightCream = Color(0xFFFFFBF0)
val DarkSlate = Color(0xFF1F2937)
val WarmGray = Color(0xFF9CA3AF)

private val WordMatchColors = lightColorScheme(
    primary = VibrantBlue,
    secondary = SunnyOrange,
    tertiary = SoftGreen,
    background = LightCream,
    surface = LightCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkSlate,
    onSurface = DarkSlate
)

@Composable
fun WordMatchTheme(content: @Composable () -> Unit) {
    // Kids app: single bright light theme, no dark mode by design.
    MaterialTheme(colorScheme = WordMatchColors, content = content)
}
