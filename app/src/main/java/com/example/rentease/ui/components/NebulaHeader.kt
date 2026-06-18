package com.example.rentease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val NebulaGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1A1A3E),
        Color(0xFF0F0F2E),
        Color(0xFF16213E),
        Color(0xFF1A1A3E)
    )
)

@Composable
fun NebulaHeader(
    modifier: Modifier = Modifier,
    bottomRadius: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(NebulaGradient)
            .then(
                if (bottomRadius > 0.dp) {
                    Modifier.clip(RoundedCornerShape(bottomStart = bottomRadius, bottomEnd = bottomRadius))
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}
