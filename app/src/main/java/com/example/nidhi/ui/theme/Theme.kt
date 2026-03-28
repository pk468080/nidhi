package com.example.nidhi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = OnPrimary,
    primaryContainer = Green50,
    secondary = Teal400,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    outline = Outline,
    outlineVariant = OutlineVariant
)

// You can define a DarkColorScheme here if needed. 
// For now, we'll use LightColorScheme for both or just fallback.
private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = OnPrimary,
    background = Color(0xFF052E16), // matching splash background
    surface = Color(0xFF064E3B)
)

@Composable
fun NidhiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
