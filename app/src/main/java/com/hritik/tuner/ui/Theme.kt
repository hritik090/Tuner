package com.hritik.tuner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val InTune = Color(0xFF4ADE80)
val Close = Color(0xFFFACC15)
val Off = Color(0xFFF87171)

private val colors = darkColorScheme(
    primary = InTune,
    onPrimary = Color(0xFF052E16),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF181B21),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF3B414D),
)

@Composable
fun TunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
