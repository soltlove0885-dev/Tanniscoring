package com.tanniscoring.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** OLED / court-scoreboard palette — true black, muted accents, high-contrast text. */
object CourtColors {
    val Black = Color(0xFF000000)
    val NearBlack = Color(0xFF0A0A0A)
    val Surface = Color(0xFF121212)
    val SurfaceElevated = Color(0xFF1A1A1A)
    val SurfaceCard = Color(0xFF1E1E1E)
    val Border = Color(0xFF2A2A2A)

    val TextPrimary = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted = Color(0xFF6B6B6B)

    /** Soft court green — primary accent (not neon). */
    val Accent = Color(0xFF64B574)
    val AccentDim = Color(0xFF3D7A4A)
    val AccentContainer = Color(0xFF1A2E1E)

    /** Muted clay / serve marker. */
    val Serve = Color(0xFFE0B84A)
    val ServeDim = Color(0xFF8A7018)
    val ServeContainer = Color(0xFF2A2410)

    val Danger = Color(0xFFB85C5C)
    val DangerContainer = Color(0xFF2A1515)
}

private val OledDarkColors = darkColorScheme(
    primary = CourtColors.Accent,
    onPrimary = CourtColors.Black,
    primaryContainer = CourtColors.AccentContainer,
    onPrimaryContainer = CourtColors.TextPrimary,
    secondary = CourtColors.Serve,
    onSecondary = CourtColors.Black,
    secondaryContainer = CourtColors.ServeContainer,
    onSecondaryContainer = CourtColors.Serve,
    tertiary = CourtColors.Danger,
    onTertiary = CourtColors.TextPrimary,
    tertiaryContainer = CourtColors.DangerContainer,
    onTertiaryContainer = CourtColors.Danger,
    background = CourtColors.Black,
    onBackground = CourtColors.TextPrimary,
    surface = CourtColors.NearBlack,
    onSurface = CourtColors.TextPrimary,
    surfaceVariant = CourtColors.SurfaceElevated,
    onSurfaceVariant = CourtColors.TextSecondary,
    outline = CourtColors.Border,
    outlineVariant = CourtColors.Border,
    error = CourtColors.Danger,
    onError = CourtColors.TextPrimary,
)

@Composable
fun TanniscoringTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = OledDarkColors,
        content = content,
    )
}
