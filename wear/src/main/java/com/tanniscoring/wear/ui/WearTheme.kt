package com.tanniscoring.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/** Battery-friendly OLED wear palette — true black, muted accents. */
object WearCourtColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceElevated = Color(0xFF1A1A1A)
    val Border = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFE8E8E8)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted = Color(0xFF6B6B6B)
    val Accent = Color(0xFF5A8F62)
    val AccentDim = Color(0xFF3D6B44)
    val Serve = Color(0xFFC9A227)
    val ServeDim = Color(0xFF8A7018)
    val ServeContainer = Color(0xFF2A2410)
    val SideB = Color(0xFF8A5A4A)
}

private val WearOledColors = Colors(
    primary = WearCourtColors.Accent,
    primaryVariant = WearCourtColors.AccentDim,
    secondary = WearCourtColors.Serve,
    secondaryVariant = WearCourtColors.ServeDim,
    background = WearCourtColors.Black,
    surface = WearCourtColors.Surface,
    error = Color(0xFFB85C5C),
    onPrimary = WearCourtColors.Black,
    onSecondary = WearCourtColors.Black,
    onBackground = WearCourtColors.TextPrimary,
    onSurface = WearCourtColors.TextPrimary,
    onError = WearCourtColors.TextPrimary,
)

@Composable
fun TanniscoringWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearOledColors,
        content = content,
    )
}
