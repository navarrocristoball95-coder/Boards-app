package com.quickreply.boards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BoardsBlue,
    onPrimary = Color.White,
    primaryContainer = BoardsBlueLight,
    onPrimaryContainer = BoardsBlueDark,
    secondary = BoardsBlueDark,
    onSecondary = Color.White,
    secondaryContainer = BoardsPillBgLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = BoardsBlue,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = BoardsCardBgLight,
    onSurfaceVariant = TextSecondaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = Color(0xFFE2E4E9),
    outlineVariant = Color(0xFFECEEF2)
)

private val DarkColorScheme = darkColorScheme(
    primary = BoardsBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E284A),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = BoardsBlueLight,
    onSecondary = Color.Black,
    secondaryContainer = BoardsPillBgDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = BoardsBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = BoardsCardBgDark,
    onSurfaceVariant = TextSecondaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = Color(0xFF33373F),
    outlineVariant = Color(0xFF262930)
)

@Composable
fun QuickReplyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
