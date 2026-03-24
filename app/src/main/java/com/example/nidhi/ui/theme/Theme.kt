package com.example.nidhi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light color scheme for the Nidhi app.
 * Green-themed palette inspired by Urban Company.
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Green500,
    onPrimary = OnPrimary,
    primaryContainer = Green50,
    onPrimaryContainer = Green600,

    // Secondary colors (using green variants)
    secondary = Green600,
    onSecondary = OnPrimary,
    secondaryContainer = Green100,
    onSecondaryContainer = Green600,

    // Tertiary colors (using teal for accents)
    tertiary = Teal400,
    onTertiary = OnPrimary,
    tertiaryContainer = Green50,
    onTertiaryContainer = Green600,

    // Background colors
    background = Background,
    onBackground = OnSurface,

    // Surface colors
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    // Error colors
    error = Error,
    onError = OnPrimary,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    // Outline colors
    outline = Outline,
    outlineVariant = OutlineVariant,

    // Other colors
    surfaceTint = Green500,
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Green100
)

/**
 * Main theme for the Nidhi app.
 * Applies Material 3 theming with custom design system.
 */
@Composable
fun NidhiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
