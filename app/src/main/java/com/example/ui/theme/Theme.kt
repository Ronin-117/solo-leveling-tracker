package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = MutedCyan,
    tertiary = LightCyan,
    background = PitchBlack,
    surface = PitchBlack,
    onPrimary = PitchBlack,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = NeonCyan
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for pitch-black look
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackColorScheme,
        typography = Typography,
        content = content
    )
}
