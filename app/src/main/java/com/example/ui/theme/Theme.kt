package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = SpotifyElevatedGrey,
    onPrimaryContainer = SpotifyGreenBright,
    secondary = SpotifyGreenBright,
    onSecondary = SpotifyBlack,
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surface = SpotifyDarkCharcoal,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyElevatedGrey,
    onSurfaceVariant = SpotifySubtextGrey,
    outline = SpotifySubtextGrey.copy(alpha = 0.3f)
)

@Composable
fun SpotifyLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpotifyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
