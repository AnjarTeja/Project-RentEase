package com.example.rentease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rentease.ui.theme.TechCardBg

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    borderColor: Color = Color(0xFF00D9FF).copy(alpha = 0.3f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(TechCardBg)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.1f),
                        borderColor,
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(radius)
            )
            .padding(16.dp)
    ) {
        content()
    }
}
