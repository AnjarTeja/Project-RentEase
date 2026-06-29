package com.example.rentease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.rentease.ui.theme.BlueLight
import com.example.rentease.ui.theme.White

val ModernGradient = Brush.verticalGradient(
    colors = listOf(
        White,
        BlueLight.copy(alpha = 0.5f)
    )
)

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    starAlpha: Float = 0.0f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModernGradient)
    ) {
        content()
    }
}
