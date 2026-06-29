package com.example.rentease.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RentEaseLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = BlueLight,
    secondary = BlueAccent,
    onSecondary = White,
    tertiary = WarningOrange,
    background = White,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = White,
    outline = DividerColor,
    outlineVariant = TextHint
)

@Composable
fun RentEaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RentEaseLightColorScheme,
        typography = RentEaseTypography,
        shapes = RentEaseShapes,
        content = content
    )
}
