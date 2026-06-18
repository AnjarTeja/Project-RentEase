package com.example.rentease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.rentease.ui.theme.TechDarkBg

val GalaxyGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B0B1A),
        Color(0xFF0F0F2E),
        Color(0xFF1A1A3E),
        TechDarkBg
    )
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    starAlpha: Float = 0.3f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GalaxyGradient)
    ) {
        StarFieldOverlay(alpha = starAlpha)
        content()
    }
}
