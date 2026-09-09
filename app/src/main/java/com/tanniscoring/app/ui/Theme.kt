package com.tanniscoring.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B5E20)
private val GreenLight = Color(0xFF4C8C4A)
private val CourtClay = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenLight,
    tertiary = CourtClay,
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Green,
    tertiary = CourtClay,
)

@Composable
fun TanniscoringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
