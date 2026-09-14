package com.tanniscoring.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/** Battery-friendly OLED wear palette — true black, refined accents. */
object WearCourtColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceElevated = Color(0xFF1C1C1E)
    val Border = Color(0xFF2C2C2E)
    val TextPrimary = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFFA1A1A6)
    val TextMuted = Color(0xFF636366)
    val Accent = Color(0xFF64B574)
    val AccentDim = Color(0xFF3D7A4A)
    val Serve = Color(0xFFE0B84A)
    val ServeDim = Color(0xFF9A7A20)
    val ServeContainer = Color(0xFF2A2410)
    val SideB = Color(0xFF7A9AC8)
    val Danger = Color(0xFFC45C5C)
}

private val WearOledColors = Colors(
    primary = WearCourtColors.Accent,
    primaryVariant = WearCourtColors.AccentDim,
    secondary = WearCourtColors.Serve,
    secondaryVariant = WearCourtColors.ServeDim,
    background = WearCourtColors.Black,
    surface = WearCourtColors.Surface,
    error = WearCourtColors.Danger,
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
