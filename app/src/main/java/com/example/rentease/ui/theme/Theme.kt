package com.example.rentease.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RentEaseDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TechDarkBg,
    primaryContainer = PrimaryDark,
    secondary = PurpleAccent,
    onSecondary = TechDarkBg,
    tertiary = WarningColor,
    background = TechDarkBg,
    onBackground = TextDark,
    surface = TechCardBg,
    onSurface = TextDark,
    surfaceVariant = TechSurface,
    onSurfaceVariant = TextLight,
    error = ErrorColor,
    onError = TextDark,
    outline = DividerColor,
    outlineVariant = TextHint
)

@Composable
fun RentEaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RentEaseDarkColorScheme,
        typography = RentEaseTypography,
        shapes = RentEaseShapes,
        content = content
    )
}
