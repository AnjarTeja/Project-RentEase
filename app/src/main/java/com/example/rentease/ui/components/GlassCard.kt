package com.example.rentease.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rentease.ui.theme.TechCardBg

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = TechCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        content()
    }
}

@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x1AFFFFFF),
                        Color(0x08FFFFFF)
                    )
                )
            )
            .padding(16.dp)
    ) {
        content()
    }
}
